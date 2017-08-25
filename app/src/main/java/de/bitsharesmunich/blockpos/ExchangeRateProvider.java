package de.bitsharesmunich.blockpos;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.Converter;
import de.bitsharesmunich.graphenej.Price;
import de.bitsharesmunich.graphenej.api.GetObjects;
import de.bitsharesmunich.graphenej.errors.IncompleteAssetError;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.BitAssetData;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.ExchangeRateListener;

/**
 * Class created to encapsulate the procedure used to obtain an exchange rate between any two assets.
 *
 * Created by nelson on 1/10/17.
 */
public class ExchangeRateProvider {
    private final String TAG = this.getClass().getName();
    private Context mContext;
    private Handler mHandler;
    private Asset base;
    private Asset quote;

    /* Database interface */
    private BlockpayDatabase database;

    /* External listener */
    private ExchangeRateListener mListener;

    /* Threads */
    private WebsocketWorkerThread fetchBitassetData;

    /* Listener to the 'get_objects' API call */
    private WitnessResponseListener mObjectsListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mObjectsListener.onSuccess");
            List<BitAssetData> bitAssetDataArray = (List<BitAssetData>) response.result;
            if(bitAssetDataArray.size() == 2){
                // In this case we're dealing with a conversion between 2 smartcoins
                Price basePrice = bitAssetDataArray.get(0).current_feed.core_exchange_rate;
                Price quotePrice = bitAssetDataArray.get(1).current_feed.core_exchange_rate;
                Asset baseBaseAsset = basePrice.base.getAsset();
                Asset baseQuoteAsset = basePrice.quote.getAsset();
                Asset quoteBaseAsset = quotePrice.base.getAsset();
                Asset quoteQuoteAsset = quotePrice.quote.getAsset();
                baseBaseAsset.setPrecision(database.fillAssetDetails(baseBaseAsset).getPrecision());
                baseQuoteAsset.setPrecision(database.fillAssetDetails(baseQuoteAsset).getPrecision());
                quoteBaseAsset.setPrecision(database.fillAssetDetails(quoteBaseAsset).getPrecision());
                quoteQuoteAsset.setPrecision(database.fillAssetDetails(quoteQuoteAsset).getPrecision());

                Converter converter = new Converter();
                double baseToCore = converter.getConversionRate(basePrice, Converter.BASE_TO_QUOTE);
                double coreToBase = converter.getConversionRate(quotePrice, Converter.QUOTE_TO_BASE);
                double finalRate = baseToCore * coreToBase;
                Log.d(TAG, String.format("base to core: %.5f, core to base: %.5f, final rate: %.5f", baseToCore, coreToBase, finalRate));

                final ExchangeRateResult result = new ExchangeRateResult(base, quote, finalRate, null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onExchangeRate(result);
                    }
                });
            }else if(bitAssetDataArray.size() == 1){
                // In the case we're dealing with just one smartcoin
                Price price = bitAssetDataArray.get(0).current_feed.core_exchange_rate;
                Asset baseAsset = database.fillAssetDetails(price.base.getAsset());
                Asset quoteAsset = database.fillAssetDetails(price.quote.getAsset());
                Log.d(TAG,"base: "+baseAsset.getObjectId()+", quote: "+quoteAsset.getObjectId());
                price.base.getAsset().setPrecision(baseAsset.getPrecision());
                price.quote.getAsset().setPrecision(quoteAsset.getPrecision());
                Converter converter = new Converter();
                int direction;
                if(price.base.getAsset().getObjectId().equals(base.getObjectId())){
                    direction = Converter.BASE_TO_QUOTE;
                }else{
                    direction = Converter.QUOTE_TO_BASE;
                }
                double convertionRate = converter.getConversionRate(price, direction);
                final ExchangeRateResult result = new ExchangeRateResult(baseAsset, quoteAsset, convertionRate, null);
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mListener.onExchangeRate(result);
                    }
                });
            }
        }

        @Override
        public void onError(final BaseResponse.Error error) {
            Log.d(TAG,"mObjectsListener.onError. Msg: "+error.message);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mListener.onExchangeRate(new ExchangeRateResult(base, quote, 0, error.message));
                }
            });
        }
    };

    /**
     * Public constructor, specifying not just the base and quote assets, but also the core asset in order to
     * try to perform an indirect conversion in case there is no active market between base and quote.
     *
     * @param context: Instance of the Context class
     * @param base: The asset the user has, and which a unit of is assumed.
     * @param quote: The asset the user wants to get, and which a unit of the base will be converted to.
     * @param listener: The interested party, which will be notified of the exchange rate when the query is done.
     */
    public ExchangeRateProvider(Context context, Asset base, Asset quote, ExchangeRateListener listener){
        if(base.getPrecision() == -1 || quote.getPrecision() == -1){
            throw new IncompleteAssetError();
        }
        this.mContext = context;
        this.mHandler = new Handler();
        this.base = base;
        this.quote = quote;
        this.mListener = listener;
        this.database = new BlockpayDatabase(mContext);
    }

    /**
     * Public constructor that assumes the core currency as the base, and receives then just the quote
     * smartcoin.
     * @param context: Instance of the Context class
     * @param quote: The asset the user wants to get, and which a unit of the base will be converted to.
     * @param listener: The interested party, which will be notified of the exchange rate when the query is done.
     */
    public ExchangeRateProvider(Context context, Asset quote, ExchangeRateListener listener){
        if(quote.getPrecision() == -1){
            throw new IncompleteAssetError();
        }
        this.mContext = context;
        this.mHandler = new Handler();
        this.base = new Asset("1.3.0");
        this.quote = quote;
        this.mListener = listener;
        this.database = new BlockpayDatabase(mContext);
    }

    /**
     * Public method that triggers the conversion procedure. This class will first try to perform a direct
     * conversion between base and quote assets. But this might fail due to the lack of an active market between
     * these two assets.
     *
     * In this case, an indirect exchange rate will be obtained by first obtaining the relationship between
     * base to core, and then from core to quote.
     *
     * If any step of this also fails the, listener will be notified with an error.
     */
    public void dispatch(){
        ArrayList<String> ids = new ArrayList<>();
        if(base != null && this.base.getBitassetId() != null && !this.base.getBitassetId().equals(""))
            ids.add(this.base.getBitassetId());
        if(quote != null && !this.quote.getBitassetId().equals(""))
            ids.add(this.quote.getBitassetId());
        GetObjects getObjects = new GetObjects(ids, mObjectsListener);
        fetchBitassetData = new WebsocketWorkerThread(getObjects);
        fetchBitassetData.start();
    }
}