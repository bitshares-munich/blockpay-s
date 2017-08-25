package de.bitsharesmunich.blockpos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.Gson;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormatSymbols;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import butterknife.BindView;
import butterknife.ButterKnife;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.BaseOperation;
import de.bitsharesmunich.graphenej.Invoice;
import de.bitsharesmunich.graphenej.LimitOrder;
import de.bitsharesmunich.graphenej.LineItem;
import de.bitsharesmunich.graphenej.ObjectType;
import de.bitsharesmunich.graphenej.OrderBook;
import de.bitsharesmunich.graphenej.PublicKey;
import de.bitsharesmunich.graphenej.Transaction;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.Util;
import de.bitsharesmunich.graphenej.api.GetLimitOrders;
import de.bitsharesmunich.graphenej.api.TransactionBroadcastSequence;
import de.bitsharesmunich.graphenej.interfaces.SubscriptionHub;
import de.bitsharesmunich.graphenej.interfaces.SubscriptionListener;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.BroadcastedTransaction;
import de.bitsharesmunich.graphenej.models.SubscriptionResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.operations.LimitOrderCreateOperation;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.graphenej.operations.TransferOperationBuilder;
import de.bitsharesmunich.interfaces.ExchangeRateListener;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.BlocktradesResponse;
import de.bitsharesmunich.models.BlocktradesSessionResponse;
import de.bitsharesmunich.models.InitiateTradeResponse;
import de.bitsharesmunich.models.InputEstimate;
import de.bitsharesmunich.models.MerchantEmail;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.BlockpayConverter;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.TinyDB;
import de.bitsharesmunich.utils.TransactionCosts;
import de.bitsharesmunich.utils.TransferType;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Across this class, a convention was adopted of using underscore-prefixed variable names
 * when referring to values in the base representation of a given asset. The base representation
 * is the one that takes into consideration all decimal places of an asset and thus expresses its
 * value as a long.
 *
 * So for instance if the user typed in 1.05 USD, the variable {@link QRCodeActivity#_inputFiatAmount}
 * will hold the value 10500, taking into account the precision of value 4 for the bitUSD smartcoin.
 */
public class QRCodeActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();

    /* Intent keys for extras */
    public static final String KEY_INPUT_COIN_TYPE = "inputCoinType";
    public static final String KEY_INPUT_COIN_NAME = "inputCoinName";
    public static final String KEY_AMOUNT = "amount";
    public static final String KEY_CORE_TO_OUTPUT_EXCHANGE = "key_core_to_output";
    public static final String KEY_INPUT_TO_OUTPUT_EXCHANGE = "key_input_to_output";
    public static final String KEY_ORDER_BOOK = "key_order_book";

    /* The maximum number of limit orders to retrieve */
    private static final int MAX_LIMIT_ORDERS = 100;

    /* Time in miliseconds the QR code will be available for the customer to scan */
    private final int ON_TICK_INTERVAL = 1000;

    /**
     * Factor used to avoid accepting values that might not correspond to the requested one.
     * 1.5 means we will recognize an incoming transaction as the one we requested for
     * even if it is 50% above the requested amount.
     */
    private static final double INCOMING_TRANSACTION_UPPER_TOLERANCE = 1.5;

    /**
     * Factor used as a threshold for incoming transactions below the expected value.
     * 0.05 means we will recognize an incoming transaction as the one we requested for
     * even if it is 5% below the requested amount.
     */
    private static final double INCOMING_TRANSACTION_LOWER_TOLERANCE = 0.05;

    /**
     * Time to wait for the incoming payment
     */
    private static final int PAYMENT_WAIT_TIME = 60000;

    /**
     * The desired precision for fiat currencies
     */
    private final int FIAT_PRECISION = 2;

    /**
     * The precision used in the core asset
     */
    private final int CORE_ASSET_PRECISION = 5;

    /* Constants used to communicate with the blocktrades API */
    private final String INPUT_COIN_TYPE = "inputCoinType";
    private final String OUTPUT_COIN_TYPE = "outputCoinType";
    private final String OUTPUT_ADDRESS = "outputAddress";
    private final String SESSION_TOKEN = "sessionToken";
    private final String REFUND_ADDRESS = "refundAddress";

    @BindView(R.id.tvTimer) TextView tvTimer;
    @BindView(R.id.txtScreen) TextView tvAmount;
    @BindView(R.id.ivCoin) ImageView ivCoin;

    @BindView((R.id.tvAmountDistribution)) TextView tvAmountDistribution;

    @BindView(R.id.drawer_layout_icon) ImageView drawer_layout_icon;

    String fiatSymbol;

    String inputCoinType, inputCoinName;
    ImageLoader imageLoader;
    int waitingTime;
    Locale locale;

    /**
     * Dialog to be displayed while looking for network data
     */
    private ProgressDialog progress;

    /**
     * Blocktrades session token
     */
    private String sessionToken;

    /**
     * Countdown waitingTime used to automatically cancel the activity after a given amount of time.
     */
    private CountDownTimer countDownTimer;

    /**
     * Selected input fiat currency as per <a href="http://www.iso.org/iso/home/standards/currency_codes.htm">ISO-4217</a>
     */
    private String inputFiatCurrency;

    /**
     * Fiat amount originally specified by the customer.
     */
    @Deprecated
    private Double inputFiatAmount;
    private long _inputFiatAmount;

    /**
     * Fiat amount converted to a DEX-supported fiat amount. Might be the same
     * as inputFiatAmount.
     */
    private Double outputFiatAmount;
    private long _outputFiatAmount;


    /**
     * Variable used to specify the total amount requested to the customer in the output currency and in base
     * value. By total we mean enough value to cover for:
     *
     * - The price of the good to be paid to the merchant
     * - Bitshares Munich's fee
     * - Network fees
     */
    private long _totalOutputAmount;

    /**
     * Boolean variable that will be set to true in case we're handling an indirect conversion,
     * or false otherwise.
     */
    private boolean isIndirectConversion;

    /**
     * Pending transfer operation, or the incoming transfer operation we will be waiting for after we
     * displayed the QR code for the customer
     */
    private TransferOperation pendingTransferOperation;

    /**
     * Asset used to express the price of an item, this is usually expected to be the same
     * as the {@link QRCodeActivity#outputAsset}, but it is not necessary that the typed in currency
     * be the same as the one used to collect the final amount.
     */
    private Asset inputAsset;

    /**
     * Core asset of the system, which in this case would be the BTS token.
     */
    private Asset coreAsset;

    /**
     * Specified output smartcoin (bitUSD, bitEUR, bitCNY, etc)
     */
    private Asset outputAsset;

    /* Database interface */
    private BlockpayDatabase database;

    /* Current user account */
    private UserAccount mUserAccount;

    /* Hardcoded blocktrades address */
    private UserAccount blocktradesAccount = new UserAccount("1.2.32567", "blocktrades");

    /* Hardcoded Bitshares Munich account */
    private UserAccount bitsharesMunichAccount = new UserAccount("1.2.90200", "bitshares-munich");

    /* This is one of the of the recipient account's public key, it will be used for memo encoding */
    private PublicKey blocktradesPublicKey;

    /* List of existing accounts */
    private ArrayList<AccountDetails> accountDetails;

    /* Private key of the currently selected account */
    private String privateKey;

    /* Bridge-provided input amount */
    private Double inputAmount;

    /* Worker thread that will perform a 'get_accoun_by_name' query to the witness node */
    private WebsocketWorkerThread getAccountByName;

    /**
     * Current exchange rate between the selected fiat currency input and the selected smartcoin output.
     */
    private double mEndToEndExchangeRate = 1.0;

    /**
     * Current exchange rate between the core currency (BTS) and the selected smartcoin output.
     * This is required in order to calculate now much extra input would be necessesary in order
     * to cover for network costs.
     */
    private double mCoreToOutputExchangeRate = 1.0;

    /**
     * Field used to load the UI only once.
     * Since the UI is being loaded now at the {@link #onWindowFocusChanged(boolean)} method, we need
     * a way to setup it only once, since this method can be called multiple times over the life cycle
     * of the activity.
     */
    private boolean isLoaded;

    /**
     * This field is used in order to recognize transaction objects and mark them as received.
     * We currently need this as a kind of workaround since transactions are being broadcasted
     * twice by the witness node.
     *
     * By marking a transaction as received here, we make sure we only react to it once.
     */
    private HashMap<String, Boolean> mTransactionMemory;

    /**
     * Field used to keep track of partial payments, in case we happen to receive them.
     */
    private List<AssetAmount> partialPayments;

    /**
     * Order book between the 2 assets of interest
     */
    private OrderBook mOrderBook;

    /**
     * Boolean variable used to keep track of the order book status
     */
    private boolean hasOrderBook = false;

    /**
     * Boolean variable used to keep track of the need of an order book for core-to-output conversions
     */
    private boolean needsOrderBook = true;

    /**
     * Handler used to schedule something in the UI thread
     */
    private Handler mHandler;

    /**
     * Worker thread in charge of getting the order book data
     */
    private WebsocketWorkerThread orderBookThread;

    /**
     * Converter class that will take care of conversions between the 3 basic assets that the app is capable of
     * handling, which are:
     *
     * - Input
     * - Core
     * - Output
     *
     * The input and output will smartcoins selected by the user in the settings screen. While the Core asset will
     * always be the BTS.
     */
    private BlockpayConverter blockpayConverter;

    /**
     * Constant used to specify a state in which we're only waiting for the result of 1 thread
     * with the desired exchange rate.
     */
    private final int EXCHANGE_RATE_READY = 0;

    /**
     * Constant used to specify a state in which we're waiting for the result of 2 threads, with
     * 2 different exchange rates.
     */
    private final int EXCHANGE_RATE_WAITING = 1;

    /**
     * In some situations we might spawn 2 parallel threads to recover the 2 minimum required
     * exchange rates. In those situations, the mExchangeRateState flag must be switched to the
     * EXCHANGE_RATE_WAITING state in order to help with the coordination of the 2 threads
     */
    private int mExchangeRateState = EXCHANGE_RATE_READY;

    /**
     * Boolean flag used to decide whether to run the {@link #setupFiatAmount} method. This is only required
     * in certain situations where we have 2 concurrent threads.
     */
    private boolean alreadySetup = false;

    /**
     * Fields used to keep a reference to the operations contained in the last transaction in case they fail
     */
    private List<BaseOperation> mOperationsCache;
    private Asset mFeeAssetCache;

    /**
     * Variable that will be used to store the transfer type we're dealing with
     */
    private TransferType mTransferType;

    /**
     * Constant that specifies the maximum number of retries to perform while accessing the DEX
     * in order to exchange an asset for another
     */
    private static final int MAX_RETRIES = 3;

    /**
     * Increase factor to be used in case our initial DEX exchange offer fails to match any order
     */
    private static final double INCREASE_OFFER_FACTOR = 1.01;

    /**
     * Counter used to keep track of the number of retries
     */
    private int mRetries;

    /**
     * Class used for transaction costs calculations
     */
    private TransactionCosts mTransactionCosts;

    /**
     * Costs expressed in the core asset
     */
    private AssetAmount _transferCosts;
    private AssetAmount _exchangeCosts;
    private AssetAmount _bitsharesMunichCut;

    /**
     * This extra cost has been added in order to compensate for the uncertainty present in the current
     * bridge estimate-input-amount API call. It increases the requested amount by 0.01 USD in order
     * to protect the merchant from receiving a bit less of what he asked for.
     */
    private AssetAmount _bridgeUncertainty;

    /**
     * Variable used to keep track of the index of the current node
     */
    private int mUriIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_code);
        ButterKnife.bind(this);

        mHandler = new Handler();

        if(BuildConfig.BUILD_TYPE.equals("debug")){
            Log.d(TAG,"Overriding bitshares munich account");
            bitsharesMunichAccount = new UserAccount("1.2.116354", "bilthon83");
        }

        /* Obtaining an instance of the database interface */
        database = new BlockpayDatabase(this);

        /* Setting up core asset and deducing the network fees */
        coreAsset = database.fillAssetDetails(new Asset(Constants.CORE_ASSET_ID));

        setToolbarAndNavigationBar(getString(R.string.txt_please_pay), true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        drawer_layout_icon.setVisibility(View.GONE);

        String language = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_LANGUAGE);
        String country = getResources().getConfiguration().locale.getCountry();
        locale = new Locale(language, country);

        String selectedSmartCoinString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin));
        Smartcoin selectedSmartCoin = gson.fromJson(selectedSmartCoinString, Smartcoin.class);
        outputAsset = new Asset(selectedSmartCoin.asset_id, selectedSmartCoin.symbol, Integer.parseInt(selectedSmartCoin.precision));

        if(!coreAsset.equals(outputAsset)){
            /* Requesting the order book */
            orderBookThread = new WebsocketWorkerThread(new GetLimitOrders(coreAsset.getObjectId(), outputAsset.getObjectId(), MAX_LIMIT_ORDERS, mLimitOrdersListener), mUriIndex);
            orderBookThread.start();
        }else{
            needsOrderBook = false;
        }

        TinyDB tinyDB = new TinyDB(getApplicationContext());
        accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.get(i).isSelected) {
                mUserAccount = new UserAccount(accountDetails.get(i).account_id, accountDetails.get(i).account_name);
                try {
                    privateKey = Crypt.getInstance().decrypt_string(accountDetails.get(i).wif_key);
                } catch (Exception e){
                    Log.e(TAG,"Exception while decrypting private key. Msg: "+e.getMessage());
                    privateKey="";
                }
                break;
            }
        }

        fiatSymbol = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_fiat_symbol));
        inputFiatCurrency = tinyDB.getString(Constants.KEY_CURRENCY);

        mTransactionMemory = new HashMap<>();
    }

    /**
     * Listener called when we get a response from the network with the full order book.
     */
    private WitnessResponseListener mLimitOrdersListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mLimitOrdersListener.onSuccess");
            final List<LimitOrder> orders = (List<LimitOrder>) response.result;
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mOrderBook = new OrderBook(orders);
                    if(!hasOrderBook){
                        Log.i(TAG,"Got late order book!");
                        hasOrderBook = true;
                        checkConversionType();
                    }
                }
            });
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG,"mLimitOrdersListener.onError. Msg: "+error.message);
            if(mUriIndex < BlockpayApplication.urlsSocketConnection.length - 1){
                mUriIndex++;
                orderBookThread = new WebsocketWorkerThread(new GetLimitOrders(coreAsset.getObjectId(), outputAsset.getObjectId(), MAX_LIMIT_ORDERS, mLimitOrdersListener), mUriIndex);
                orderBookThread.start();
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), getString(R.string.error_node_unreachable), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }
    };

    @Override
    protected void onStart() {
        super.onStart();
        ((SubscriptionHub) getApplication()).addSubscriptionListener(transferListener);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if(hasFocus && !isLoaded){
            Log.d(TAG,"input fiat currency..: "+ inputFiatCurrency);

            inputCoinType = getIntent().getStringExtra(KEY_INPUT_COIN_TYPE); // Ex. BTC
            inputCoinName = getIntent().getStringExtra(KEY_INPUT_COIN_NAME); // Ex. BITCOIN
            inputFiatAmount = getIntent().getDoubleExtra(KEY_AMOUNT, 0.0);
            mCoreToOutputExchangeRate = getIntent().getDoubleExtra(KEY_CORE_TO_OUTPUT_EXCHANGE, -2);
            mEndToEndExchangeRate = getIntent().getDoubleExtra(KEY_INPUT_TO_OUTPUT_EXCHANGE, -3);
            String serializedOrderBook = getIntent().getExtras().getString(KEY_ORDER_BOOK, "");
            if(!serializedOrderBook.equals("")){
                Gson gson = new Gson();
                mOrderBook = gson.fromJson(serializedOrderBook, OrderBook.class);
                hasOrderBook = true;
            }

            inputAsset = database.getAssetBySymbol(inputFiatCurrency);
            _inputFiatAmount = (long) (getIntent().getDoubleExtra(KEY_AMOUNT, 0.0) * Math.pow(10, inputAsset.getPrecision()));

            showDialog();

            /*
            In case the previous activity could not figure out these exchange rates and pass them on to us, we need
            to ask the network for them in order to ask the bridge for the right amount of funds.
             */
            ExchangeRateProvider coreToOutputProvider;
            ExchangeRateProvider inputToOutputProvider;
            int activeExchangeThreads = 0;
            if(mCoreToOutputExchangeRate < 0){
                Log.d(TAG, "Getting exchange rates");
                Asset base = database.fillAssetDetails(coreAsset);
                Asset quote = database.fillAssetDetails(outputAsset);
                Log.d(TAG,String.format("Converting from %s to %s", base.getSymbol(), quote.getSymbol()));
                coreToOutputProvider = new ExchangeRateProvider(this, base, quote, mCoreToOutputExchangeListener);
                coreToOutputProvider.dispatch();
                activeExchangeThreads++;
            }
            if(mEndToEndExchangeRate < 0 && !inputAsset.getObjectId().equals(outputAsset.getObjectId())){
                Asset base = database.getAssetBySymbol(inputFiatCurrency);
                Asset quote = database.fillAssetDetails(outputAsset);
                Log.d(TAG,String.format("Converting from %s to %s", base.getSymbol(), quote.getSymbol()));
                inputToOutputProvider = new ExchangeRateProvider(QRCodeActivity.this, base, quote, mEndToEndExchangeListener);
                inputToOutputProvider.dispatch();
                activeExchangeThreads++;
            } else {
                Log.d(TAG, "Already got exchange rates");
                if(inputAsset.getObjectId().equals(outputAsset.getObjectId())){
                    mEndToEndExchangeRate = 1;
                }
                setupFiatAmount();
            }
            if(activeExchangeThreads == 2){
                mExchangeRateState = EXCHANGE_RATE_WAITING;
            }

            imageLoader = ImageLoader.getInstance();
            String imgUri = "drawable://" + getResources().getIdentifier(Helper.getCoinImagePath(inputCoinType), "drawable", getPackageName());
            imageLoader.displayImage(imgUri, ivCoin);
        }
        isLoaded = true;
    }

    /**
     * Listener that will give us the core-to-output exchange rate
     */
    private ExchangeRateListener mCoreToOutputExchangeListener = new ExchangeRateListener() {
        @Override
        public  void onExchangeRate(ExchangeRateResult exchangeRate) {
            Log.d(TAG,"onExchangeRate. exchangeRate: "+exchangeRate);
            try{
                mCoreToOutputExchangeRate = exchangeRate.conversionRate;
                if(mExchangeRateState == EXCHANGE_RATE_READY){
                    setupFiatAmount();
                }else{
                    mExchangeRateState = EXCHANGE_RATE_READY;
                }
            }catch (Exception e){
                Log.e(TAG,"Exception. Msg: "+e.getMessage());
            }
        }
    };

    /**
     * Listener that will give us how much of a unit of the input currency is valued in the
     * output currency.
     */
    private ExchangeRateListener mEndToEndExchangeListener = new ExchangeRateListener() {

        @Override
        public void onExchangeRate(ExchangeRateResult exchangeRate) {
            try{
                mEndToEndExchangeRate = exchangeRate.conversionRate;
                if(mExchangeRateState == EXCHANGE_RATE_READY){
                    setupFiatAmount();
                }else{
                    mExchangeRateState = EXCHANGE_RATE_READY;
                }
            }catch (Exception e){
                Log.e(TAG, "Exception Msg: "+e.getMessage());
            }
        }
    };

    @Override
    protected void onPause() {
        super.onPause();
        if(progress != null && progress.isShowing()){
            progress.cancel();
        }
        ((SubscriptionHub) getApplication()).removeSubscriptionListener(transferListener);

        if(countDownTimer != null)
            countDownTimer.cancel();
    }

    /**
     * Called once we have the exchange rate from the input fiat currency to the
     * output smartcoin.
     */
    private void setupFiatAmount(){
        if(alreadySetup) {
            return;
        }
        Log.d(TAG,"setupFiatAmount");
        outputFiatAmount = inputFiatAmount * mEndToEndExchangeRate;

        blockpayConverter = new BlockpayConverter(inputAsset, coreAsset, outputAsset, mEndToEndExchangeRate, mCoreToOutputExchangeRate);

        _outputFiatAmount = blockpayConverter.convert(_inputFiatAmount, BlockpayConverter.INPUT_TO_OUTPUT).getAmount().longValue();

        Log.d(TAG,"mEndToEndExchangeRate......: "+mEndToEndExchangeRate);
        Log.d(TAG,"mCoreToOutputExchangeRate..: "+mCoreToOutputExchangeRate);
        Log.d(TAG,"_inputFiatAmount...........: "+_inputFiatAmount);
        Log.d(TAG,"_outputFiatAmount..........: "+_outputFiatAmount);
        if (inputCoinType.equals("smartcoins")) {
            mTransferType = TransferType.PLATFORM_ASSET;
            waitingTime = PAYMENT_WAIT_TIME;

            calculateTransferCosts(true);

            displayCostsBreakdown(false);

            // Converting costs to input values
            AssetAmount networkFeeInput = blockpayConverter.convert(_transferCosts.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);
            AssetAmount exchangeFeeInput = blockpayConverter.convert(_exchangeCosts.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);
            AssetAmount bitsharesMunichCutInput = blockpayConverter.convert(_bitsharesMunichCut.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);

            // Adding all the fees together
            AssetAmount inputValue = new AssetAmount(UnsignedLong.valueOf(_inputFiatAmount), inputAsset);
            AssetAmount totalInputFees =  networkFeeInput.add(exchangeFeeInput).add(bitsharesMunichCutInput);

            displayInvoiceData(Util.fromBase(inputValue), Util.fromBase(totalInputFees));
        } else {
            // Network fees in the output currency
            long _outputFiatAmount = blockpayConverter.convert(_inputFiatAmount, BlockpayConverter.INPUT_TO_OUTPUT).getAmount().longValue();
            long _bitsharesMunichInputCut = blockpayConverter.convert( (long) (_inputFiatAmount * Constants.BITSHARES_MUNICH_FEE), BlockpayConverter.INPUT_TO_OUTPUT).getAmount().longValue();
            long _outputNetworkFee = blockpayConverter.convert((long)(Constants.FIXED_TRANSFER_FEE + Constants.EXTRA_NETWORK_FEE + Constants.LIMIT_ORDER_CREATE_BASIC_FEE), BlockpayConverter.CORE_TO_OUTPUT).getAmount().longValue();
            _totalOutputAmount = _outputFiatAmount + _bitsharesMunichInputCut + _outputNetworkFee;


            Log.d(TAG,"---------------------------");
            Log.d(TAG,"output fiat amount.........: "+_outputFiatAmount);
            Log.d(TAG,"bitsharesMunich cut........: "+_bitsharesMunichInputCut);
            Log.d(TAG,"Network Fee................: "+_outputNetworkFee);
            Log.d(TAG,"_totalOutputAmount.........:>"+ _totalOutputAmount +"<");
            Log.d(TAG,"---------------------------");
            String blocktradeEmail = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_email));
            String blocktradePassword = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_password));
            String transactionLimit = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_limit));
            if(transactionLimit.equals("")) transactionLimit = "0";

            // Recovering stored blocktrades session token
            long currentTimeMillis = System.currentTimeMillis();
            long sessionExpirationTime = Helper.fetchLongSharePref(this, Constants.KEY_BLOCKTRADES_SESSION_EXPIRATION);
            String storedSessionToken = Helper.fetchStringSharePref(this, Constants.KEY_BLOCKTRADES_SESSION_TOKEN);

            if(!storedSessionToken.equals("") && currentTimeMillis < sessionExpirationTime){
                Log.d(TAG,"Using stored session token");
                sessionToken = storedSessionToken;
                checkConversionType();
            }else if(!blocktradeEmail.isEmpty() &&
                    !blocktradePassword.isEmpty() &&
                    !transactionLimit.isEmpty() &&
                    Double.parseDouble(transactionLimit) >= outputFiatAmount) {
                Log.d(TAG,"Obtaining new session token");
                getSessionToken(blocktradeEmail, blocktradePassword);
            }else{
                Log.d(TAG,"Not using session token");
                checkConversionType();
            }

            waitingTime = PAYMENT_WAIT_TIME;
            if(inputCoinType.equals("eth")){
                waitingTime = waitingTime * 3;
            }
        }
        alreadySetup = true;
    }

    /**
     * Private method used to calculate the transaction costs
     * @param isNativeExchange: True if the exchange is not being mediated by an external bridge.
     */
    private void calculateTransferCosts(boolean isNativeExchange){
        AssetAmount inputValue = new AssetAmount(UnsignedLong.valueOf(_inputFiatAmount), inputAsset);

        // Trying to determine if we should ask the user for an extra 2.6 BTS in order to cover for the reward point transfer
        boolean isPointRewardActive = canSendRewardPoints(new AssetAmount(UnsignedLong.valueOf(_outputFiatAmount), outputAsset));

        // Calculating transaction costs
        mTransactionCosts = new TransactionCosts(mTransferType, inputValue, blockpayConverter, isNativeExchange && isPointRewardActive);
        _transferCosts = mTransactionCosts.getTransferFees();
        _exchangeCosts = mTransactionCosts.getExchangeFees();
        _bitsharesMunichCut = mTransactionCosts.getBitsharesMunich();

        // Calculating the extra value to be added to the request due to the bridge uncertainty
        String key = "";
        if(outputAsset.equals(Constants.BTS)){
            key = Constants.KEY_EXTRA_BTS;
        }else if(outputAsset.equals(Constants.bitUSD)){
            key = Constants.KEY_EXTRA_BITUSD;
        }else if(outputAsset.equals(Constants.bitEUR)){
            key = Constants.KEY_EXTRA_BITEUR;
        }else if(outputAsset.equals(Constants.bitCNY)){
            key = Constants.KEY_EXTRA_BITCNY;
        }else if(outputAsset.equals(Constants.bitBTC)){
            key = Constants.KEY_EXTRA_BITBTC;
        }
        long uncertaintyValue = 0;
        if(!key.equals("")){
            try{
                uncertaintyValue = Helper.fetchLongSharePref(this, key, 0);
            }catch (ClassCastException e){
                // Introduced to fix a bug caused by initially storing this
                // key's value as an int value instead of a long value
                uncertaintyValue = Helper.fetchIntSharePref(this, key, 0);
            }
        }
        _bridgeUncertainty = new AssetAmount(UnsignedLong.valueOf(uncertaintyValue), outputAsset);
    }

    /**
     * Private method that should be called after {@link #calculateTransferCosts(boolean)} and will fill the UI
     * with the correct breakdown of requested amount + fees
     * @param isBridgeEnabled : Whether the transfer will be bridge-enabled. In case it is, we should be asking for an extra
     *                        amount currenctly equivalent to a USD cent, but specified in a resource file.
     */
    private void displayCostsBreakdown(boolean isBridgeEnabled){
        long bridgeUncertaintyBase = 0;
        if(isBridgeEnabled){
            bridgeUncertaintyBase = _bridgeUncertainty.getAmount().longValue();
        }
        // Converting costs to input values
        AssetAmount networkFeeInput = blockpayConverter.convert(_transferCosts.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);
        AssetAmount exchangeFeeInput = blockpayConverter.convert(_exchangeCosts.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);
        AssetAmount bitsharesMunichCutInput = blockpayConverter.convert(_bitsharesMunichCut.getAmount().longValue(), BlockpayConverter.CORE_TO_INPUT);
        AssetAmount bridgeUncertainty = blockpayConverter.convert(bridgeUncertaintyBase, BlockpayConverter.OUTPUT_TO_INPUT);

        // Adding all the fees together
        AssetAmount inputValue = new AssetAmount(UnsignedLong.valueOf(_inputFiatAmount), inputAsset);
        AssetAmount totalInputFees =  networkFeeInput.add(exchangeFeeInput).add(bitsharesMunichCutInput).add(bridgeUncertainty);
        AssetAmount totalInput = inputValue.add(totalInputFees);

        // Deciding on the needed precision
        int desiredPrecision = FIAT_PRECISION;
        if(inputAsset.equals(coreAsset)){
            desiredPrecision = CORE_ASSET_PRECISION;
        }

        // Displaying the total input amount in fiat
        NumberFormat numberFormat = Helper.currencyFormat(getApplicationContext(), Currency.getInstance(inputFiatCurrency), locale, desiredPrecision);
        tvAmount.setText(numberFormat.format(Util.fromBase(totalInput)));

        // Displaying the distribution of 'requested value' + 'fees'
        String distributionFormat = "(%.2f + %.2f %s)";
        tvAmountDistribution.setText(String.format(distributionFormat, Util.fromBase(inputValue), Util.fromBase(totalInputFees), getString(R.string.txt_network)));
    }

    /**
     * Private method that will evaluate if the loyalty point settings is correct and if the current
     * account has enough funds in order to successfully perform a reward point transfer.
     *
     * If by any reason the reward point can't be sent to the customer, there's no need
     * to ask for an extra 2.6 BTS for the corresponding transfer fees.
     * @param outputAmount The output asset amount
     * @return True if and only if the reward point system is active and we have enough funds
     */
    private boolean canSendRewardPoints(AssetAmount outputAmount){
        try {
            double numerator = Helper.fetchDoubleSharedPref(this, Constants.KEY_LOYALTY_REWARD_AMOUNT, 0.0);
            double denominator = Helper.fetchDoubleSharedPref(this, Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0.0);
            Asset rewardPointAsset = database.fillAssetDetails(new Asset(Helper.fetchStringSharePref(this, Constants.KEY_REWARD_ASSET_ID)));
            if(numerator > 0 && denominator > 0 && rewardPointAsset != null){
                AssetAmount availableRewardAsset = database.getBalance(mUserAccount, rewardPointAsset);
                double ratio = numerator / denominator;
                double output = Util.fromBase(outputAmount);
                double available = Util.fromBase(availableRewardAsset);
                double required = output * ratio;
                return available > required;
            }else{
                return false;
            }
        }catch(Exception e){
            Log.e(TAG, "canSendRewardPoints.Exception. Msg: "+e.getMessage());
            for(StackTraceElement element : e.getStackTrace()){
                Log.w(TAG, ""+element.getClassName()+":"+element.getMethodName()+":"+element.getLineNumber());
            }
            return false;
        }
    }

    /**
     * Called as part of the setup sequence and used in cases where the platform native transfers
     * are ruled out. Meaning we have to use the bridge.
     *
     * Basically what this method has to do is to decide whether the conversion will be direct or
     * indirect, based on the trading pairs information obtained from the bridge itself.
     */
    private void checkConversionType(){
        String outputCoinType = outputAsset.getSymbol().toLowerCase();
        if(!outputAsset.getObjectId().equals(coreAsset.getObjectId()) && !outputAsset.equals(Constants.BTS)){
            outputCoinType = "bit".concat(outputCoinType);
        }

        // Fee for direct conversions
        double directFee = database.getBridgeFee(inputCoinType, outputCoinType);

        // Fees for 2-step conversions
        double toCoreFee = database.getBridgeFee(inputCoinType, coreAsset.getSymbol().toLowerCase());

        if(directFee != -1){
            // There is a direct trading pair between the desired token and the
            // selected smartcoin
            mTransferType = TransferType.BRIDGE_DIRECT;

            performDirectConversion(inputCoinType, outputCoinType);
        }else if(toCoreFee != -1){
            // There is no direct trading pair between the desired token and the
            // selected smartcoin, so we ask for the core asset (BTS) instead.
            mTransferType = TransferType.BRIDGE_INDIRECT;

            performIndirectConversion();
        }else{
            String warningTemplate = getResources().getString(R.string.warning_no_conversion_support);
            Toast.makeText(this, String.format(warningTemplate, inputCoinType, outputCoinType), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Listener that will be notified of transactions broadcasted to the blockchain.
     */
    private SubscriptionListener transferListener = new SubscriptionListener() {

        @Override
        public ObjectType getInterestObjectType() {
            return ObjectType.TRANSACTION_OBJECT;
        }

        @Override
        public void onSubscriptionUpdate(SubscriptionResponse response) {
            List<Serializable> updatedObjects = (List<Serializable>) response.params.get(1);
            if(updatedObjects.size() > 0){
                for(Serializable update : updatedObjects){
                    if(update instanceof BroadcastedTransaction){
                        BroadcastedTransaction broadcastedTransaction = (BroadcastedTransaction) update;
                        if(!mTransactionMemory.containsKey(broadcastedTransaction.getObjectId())){
                            Transaction tx = broadcastedTransaction.getTransaction();
                            if(tx.getOperations().size() > 0){
                                for(BaseOperation op : tx.getOperations()){
                                    if(op instanceof TransferOperation){
                                        onTransferOperation((TransferOperation) op);
                                    }
                                }
                            }
                            mTransactionMemory.put(broadcastedTransaction.getObjectId(), true);
                        }
                    }
                }
            }
        }
    };

    /**
     * Callback that is fired when we get a response from a transaction broadcast sequence for the
     * second step of the conversion procedure.
     */
    private WitnessResponseListener secondStepConversionListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG, "send.onSuccess.");
        }

        @Override
        public void onError(final BaseResponse.Error error) {
            Log.e(TAG, "send.onError. msg: "+error.message);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), String.format(getString(R.string.error_second_step), error.message), Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    /**
     * Callback that is fired when we get a result from the transfer to the bitshares-munich account.
     */
    private WitnessResponseListener bitsharesMunichTransferListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"bitsharesMunichTransfer.onSuccess.");
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.d(TAG,"bitsharesMunichTransfer.onError. Msg: "+error.message);
            ArrayList<BaseOperation> operationList = new ArrayList<>();
            for(BaseOperation operation : mOperationsCache){
                if(operation instanceof LimitOrderCreateOperation){
                    LimitOrderCreateOperation limitOrderCreateOp = (LimitOrderCreateOperation) operation;
                    AssetAmount amountToSell = limitOrderCreateOp.getAmountToSell();
                    limitOrderCreateOp.setAmountToSell(amountToSell.add((long) (amountToSell.getAmount().longValue() * INCREASE_OFFER_FACTOR)));
                    operationList.add(limitOrderCreateOp);
                }else{
                    operationList.add(operation);
                }
            }
            if(mRetries < MAX_RETRIES){
                broadcastTransaction(operationList, mFeeAssetCache, bitsharesMunichTransferListener);
                mRetries++;
            }
        }
    };

    /**
     * Sets up a waitingTime that will kill this activity when runs out of time.
     */
    private void startTimer() {
        countDownTimer = new CountDownTimer(waitingTime, ON_TICK_INTERVAL) {

            public void onTick(long millisUntilFinished) {
                long second = millisUntilFinished / 1000;
                tvTimer.setText(second + " " + getString(R.string.txt_seconds_remaining));
            }

            public void onFinish() {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                finish();
            }
        };
        countDownTimer.start();
    }

    /**
     * Performs a direct conversion between currencies. If this method is called is because the bridge
     * supports a direct conversion between the desired input and output cryptos.
     */
    private void performDirectConversion(final String inputCoinType, String outputCoinType){
        if(!hasOrderBook && needsOrderBook){
            return;
        }

        // Calculating transaction costs
        calculateTransferCosts(false);

        displayCostsBreakdown(true);

        // Estimating input amount, based on the calculated required output
        ServiceGenerator sg = new ServiceGenerator(getString(R.string.blocktrade_server_url));
        final IWebService service = sg.getService(IWebService.class);

        // Adding the last bridge-uncertainty-related extra amount to the requested value. This will not be considered in the
        // creation of the PendingTransaction
        AssetAmount requiredOutputAmount = new AssetAmount(UnsignedLong.valueOf(_totalOutputAmount), outputAsset).add(_bridgeUncertainty);
        String outputAmount = String.format(new Locale("en"), "%.4f", Util.fromBase(requiredOutputAmount));
        final Call<InputEstimate> inputEstimateCall = service.estimateInputAmount(inputCoinType, outputCoinType, outputAmount);
        Log.d(TAG,"inputCoinType.....: "+inputCoinType);
        Log.d(TAG,"outputCoinType....: "+outputCoinType);
        Log.d(TAG,"totalOuputAmount..: "+outputAmount);

        inputEstimateCall.enqueue(new Callback<InputEstimate>() {
            @Override
            public void onResponse(Call<InputEstimate> call, Response<InputEstimate> response) {
                if(response.isSuccessful()) {
                    InputEstimate inputEstimate = response.body();
                    inputAmount = Double.valueOf(inputEstimate.inputAmount);
                    Log.d(TAG, "Input amount......: "+inputEstimate.inputAmount+" "+ inputCoinType);

                    HashMap<String,String> hm = new HashMap<>();
                    hm.put(INPUT_COIN_TYPE, inputEstimate.inputCoinType);
                    hm.put(OUTPUT_COIN_TYPE, inputEstimate.outputCoinType);
                    hm.put(OUTPUT_ADDRESS, mUserAccount.getName());
                    if (sessionToken != null && !sessionToken.isEmpty()){
                        hm.put(SESSION_TOKEN, sessionToken);
                    }
                    final Call<InitiateTradeResponse> postingService = service.initiateTrade(hm);
                    postingService.enqueue(mInitiateDirectTradeResponse);

                    if (progress != null && progress.isShowing())
                        progress.dismiss();
                }else{
                    displayBlocktradesServerError(response);
                }
            }

            @Override
            public void onFailure(Call<InputEstimate> call, Throwable t) {
                Log.e(TAG,"onFailure. Msg: "+t.getMessage());
                if(progress != null && progress.isShowing())
                    progress.dismiss();
                Toast.makeText(getApplicationContext(), t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * If this method was called, it is because there is no direct direct trading pair between the specified
     * smartcoin and the selected cryptocurrency. This happens a lot with many cryptos.
     *
     * We must ask the bridge to give us the equivalent amount of the required asset in BTS, the core asset
     * of the system. That amount will then be exchanged by accessing the DEX to the user selected smartcoin.
     */
    private void performIndirectConversion(){
        if(!hasOrderBook){
            return;
        }
        Log.d(TAG,"performIndirectConversion");

        calculateTransferCosts(false);
        
        displayCostsBreakdown(true);

        long _requiredCore = mOrderBook.calculateRequiredBase(new AssetAmount(UnsignedLong.valueOf(_totalOutputAmount), outputAsset));
        // Making a small adjustment in order to ask for a slightly higher price than the lowest one
        _requiredCore += 1;

        // Adding a small amount to the requested amount in order to compensate for the bridge uncertainty
        AssetAmount uncertaintyAmount = blockpayConverter.convert(_bridgeUncertainty.getAmount().longValue(), BlockpayConverter.OUTPUT_TO_CORE);
        AssetAmount requiredCoreAmount = new AssetAmount(UnsignedLong.valueOf(_requiredCore), coreAsset).add(uncertaintyAmount);

        String outputCoinType = coreAsset.getSymbol().toLowerCase();
        String coreAmount = String.format("%.5f", Util.fromBase(requiredCoreAmount));
        Log.d(TAG,"estimateInputAmount");
        Log.d(TAG,"_totalOutputAmount.....: "+_totalOutputAmount);
        Log.d(TAG,"_requiredCore..........: "+_requiredCore);

        ServiceGenerator sg = new ServiceGenerator(getString(R.string.blocktrade_server_url));
        final IWebService service = sg.getService(IWebService.class);
        Call<InputEstimate> inputEstimateCall = service.estimateInputAmount(inputCoinType, outputCoinType, coreAmount);
        inputEstimateCall.enqueue(new Callback<InputEstimate>() {

            @Override
            public void onResponse(Call<InputEstimate> call, Response<InputEstimate> response) {
                Log.d(TAG,"estimateInputAmount.onResponse");
                if(response.isSuccessful()){
                    InputEstimate inputEstimate = response.body();
                    inputAmount = Double.valueOf(inputEstimate.inputAmount);
                    Log.d(TAG, "Estimated input amount: "+inputEstimate.inputAmount+" "+ inputCoinType);

                    HashMap<String,String> hm = new HashMap<>();
                    hm.put(INPUT_COIN_TYPE, inputCoinType.toLowerCase());
                    hm.put(OUTPUT_COIN_TYPE, coreAsset.getSymbol().toLowerCase());
                    hm.put(OUTPUT_ADDRESS, mUserAccount.getName());
                    if (sessionToken != null && !sessionToken.isEmpty()){
                        hm.put(SESSION_TOKEN, sessionToken);
                    }
                    final Call<InitiateTradeResponse> postingService = service.initiateTrade(hm);
                    postingService.enqueue(mInitiateIndirectTradeHandler);

                    if (progress != null && progress.isShowing())
                        progress.dismiss();
                }else{
                    displayBlocktradesServerError(response);
                }
            }

            @Override
            public void onFailure(Call<InputEstimate> call, Throwable t) {
                Log.e(TAG,"indirectInputEstimate.onFailure. Msg: "+t.getMessage());
            }
        });
    }

    /**
     * Displays a blocktrades server error message
     * @param response: Server response
     */
    private void displayBlocktradesServerError(Response response){
        hideDialog();
        try {
            String json = response.errorBody().string();
            BlocktradesResponse errorResponse = gson.fromJson(json, BlocktradesResponse.class);
            String errorMessage = String.format("Blocktrades bridge error: %d, Msg: %s", errorResponse.error.code, errorResponse.error.message);
            Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();

            if (progress != null && progress.isShowing())
                progress.dismiss();

        } catch (IOException e) {
            Log.e(TAG, "IOException. Msg: "+e.getMessage());
        }
    }

    /**
     * Callback method fired once we get the response to the initiate-trade blocktrades API call.
     * This response should have the input address the app will display to the user, and thus it should be
     * used to generate the QR-Code to be displayed.
     */
    private Callback<InitiateTradeResponse> mInitiateIndirectTradeHandler = new Callback<InitiateTradeResponse>(){

        @Override
        public void onResponse(Call<InitiateTradeResponse> call, Response<InitiateTradeResponse> response) {
            Log.d(TAG,"mInitiateIndirectTradeHandler.onResponse");
            if(response.isSuccessful()){
                InitiateTradeResponse initiateTradeResponse = response.body();
                Log.d(TAG,"Got address: "+initiateTradeResponse.inputAddress.toString());
                ImageView imageView = (ImageView) findViewById(R.id.ivQR);

                try {
                    Bitmap bitmap = encodeAsBitmap(inputCoinName.toLowerCase() + ":" + initiateTradeResponse.inputAddress.toString() + "?amount=" + inputAmount, "#000000");
                    imageView.setImageBitmap(bitmap);
                    startTimer();

                    long _requiredCore = mOrderBook.calculateRequiredBase(new AssetAmount(UnsignedLong.valueOf(_totalOutputAmount), outputAsset));
                    // Making a small adjustment in order to ask for a slightly higher price than the lowest one
                    _requiredCore += 1;
                    AssetAmount requiredCoreAmount = new AssetAmount(UnsignedLong.valueOf(_requiredCore), coreAsset);

                    pendingTransferOperation = new TransferOperation(null, mUserAccount, requiredCoreAmount);
                    isIndirectConversion = true;

                    Log.d(TAG,"Waiting for pending transaction. value: "+requiredCoreAmount.getAmount().longValue()+", symbol: "+requiredCoreAmount.getAsset().getSymbol());
                } catch (WriterException e) {
                    Log.e(TAG,"WriterException. Msg: "+e.getMessage());
                }catch(Exception e){
                    Log.e(TAG,"Exception while creating pending balance update. Msg: "+e.getMessage());
                }
            }else{
                ResponseBody errorBody = response.errorBody();
                try {
                    Log.d(TAG,"error body: "+errorBody.string());
                    String errorMessage = errorBody.string();
                    Gson gson = new Gson();
                    InitiateTradeResponse errorResponse = gson.fromJson(errorMessage, InitiateTradeResponse.class);
                    if(errorResponse != null)
                        Log.e(TAG, String.format("Error code: %d, Msg: %s", errorResponse.error.code, errorResponse.error.message));
                    else
                        Log.e(TAG,"errorResponse is null!");
                } catch (IOException e) {
                    Log.e(TAG,"IOException. Msg: "+e.getMessage());
                }
            }
        }

        @Override
        public void onFailure(Call<InitiateTradeResponse> call, Throwable t) {
            Log.e(TAG,"mInitiateIndirectTradeHandler.onFailure");
        }
    };

    /**
     * Callback method fired once we get the response to the initiate-trade blocktrades API call.
     * This response should have the input address the app will display to the user, and thus it should be
     * used to generate the QR-Code to be displayed.
     */
    private Callback<InitiateTradeResponse> mInitiateDirectTradeResponse = new Callback<InitiateTradeResponse>(){

        @Override
        public void onResponse(Call<InitiateTradeResponse> call, Response<InitiateTradeResponse> response) {
            Log.d(TAG,"mInitiateDirectTradeResponse.onResponse");
            hideDialog();
            if(response.isSuccessful()) {
                InitiateTradeResponse initiateTradeResponse = response.body();
                Log.d(TAG,"Got address: "+initiateTradeResponse.inputAddress.toString());
                ImageView imageView = (ImageView) findViewById(R.id.ivQR);

                try {
                    Bitmap bitmap = encodeAsBitmap(inputCoinName.toLowerCase() + ":" + initiateTradeResponse.inputAddress.toString() + "?amount=" + inputAmount, "#000000");
                    imageView.setImageBitmap(bitmap);
                    startTimer();

                    pendingTransferOperation = new TransferOperation(null, mUserAccount, new AssetAmount(UnsignedLong.valueOf(_totalOutputAmount), outputAsset));
                } catch (WriterException e) {
                    Log.e(TAG,"WriterException. Msg: "+e.getMessage());
                }
            }else{
                ResponseBody responseBody = response.errorBody();
                try {
                    String errorBody = responseBody.string();
                    Log.e(TAG,"Error. body: "+errorBody);
                } catch (IOException e) {
                    Log.e(TAG,"IOException. Msg: "+e.getMessage());
                }
                Toast.makeText(getApplicationContext(), getApplicationContext().getResources().getString(R.string.error_initiating_trade), Toast.LENGTH_SHORT).show();
            }
        }

        @Override
        public void onFailure(Call<InitiateTradeResponse> call, Throwable t) {
            Log.e(TAG,"onFailure. Msg: "+t.getMessage());
            hideDialog();
        }
    };

    /**
     * Encodes the provided data as a QR-code. Used to provide payment requests.
     * @param data: Data containing payment request data as the recipient's address and the requested amount.
     * @param color: The color used for the QR-code
     * @return Bitmap with the QR-code encoded data
     * @throws WriterException
     */
    Bitmap encodeAsBitmap(String data, String color) throws WriterException {
        BitMatrix result;
        ImageView ivQR = (ImageView) findViewById(R.id.ivQR);
        try {
            Map<EncodeHintType, Object> hints = new HashMap<EncodeHintType, Object>();
            hints.put(EncodeHintType.MARGIN, 0);
            result = new MultiFormatWriter().encode(data,
                    BarcodeFormat.QR_CODE, ivQR.getWidth(), ivQR.getHeight(), hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = ivQR.getWidth();
        int h = ivQR.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * (w);
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? Color.parseColor(color) : Color.WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h);
        return bitmap;
    }

    @Override
    public void onBackPressed() {
        try {
            countDownTimer.cancel();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            try {
                countDownTimer.cancel();
            }
            catch (Exception ex)
            {
             ex.printStackTrace();
            }
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDialog() {
        progress = ProgressDialog.show(this, getString(R.string.txt_please_wait),
                getString(R.string.txt_loading) + "...", true);
    }

    private void hideDialog(){
        if(progress != null && progress.isShowing()){
            progress.dismiss();
        }
    }

    private void getSessionToken(String blocktradeEmail, String blocktradePassword) {
        HashMap hm = new HashMap();
        hm.put("email", blocktradeEmail);
        hm.put("password", blocktradePassword);
        ServiceGenerator sg = new ServiceGenerator(getApplicationContext().getString(R.string.blocktrade_server_url));
        IWebService service = sg.getService(IWebService.class);
        final Call<BlocktradesSessionResponse> postingService = service.getBlocktradeToken(hm);
        postingService.enqueue(new Callback<BlocktradesSessionResponse>() {
            @Override
            public void onResponse(Call<BlocktradesSessionResponse> call, Response<BlocktradesSessionResponse> response) {
                Log.d(TAG, "getBlocktradeToken.onResponse");
                if (response.isSuccessful()) {
                    BlocktradesSessionResponse session = response.body();
                    if (!session.token.isEmpty()){
                        Log.d(TAG,"Session token: "+session.token);
                        Log.d(TAG,"Session expiration: "+session.expirationTime);
                        try {
                            // Ex. 2017-03-31T02:00:51.478918+00:00
                            SimpleDateFormat dateFormat = new SimpleDateFormat(BlocktradesSessionResponse.EXPIRATION_TIME_FORMAT, DateFormatSymbols.getInstance(new Locale("en", "US")));

                            // Storing session token
                            Date date = dateFormat.parse(session.expirationTime);
                            Helper.storeLongSharePref(QRCodeActivity.this, Constants.KEY_BLOCKTRADES_SESSION_EXPIRATION, date.getTime());
                            Helper.storeStringSharePref(QRCodeActivity.this, Constants.KEY_BLOCKTRADES_SESSION_TOKEN, session.token);
                            Log.d(TAG,"Parsed date: "+date.toString());
                        } catch (ParseException e) {
                            Log.e(TAG,"ParseException. Msg: "+e.getMessage());
                        }
                        sessionToken = session.token;
                        checkConversionType();
                    }
                } else {
                    Log.w(TAG, "getBlocktradeToken. is not successful");
                    try {
                        String json = response.errorBody().string();
                        Log.e(TAG, "getSessionToken error. Body: "+json);

                        BlocktradesResponse errorResponse = gson.fromJson(json, BlocktradesResponse.class);
                        Toast.makeText(getApplicationContext(), String.format("Blocktrades bridge error. Msg: %s", errorResponse.error.message) , Toast.LENGTH_LONG).show();
                        if(progress != null && progress.isShowing())
                            progress.dismiss();
                    } catch (IOException e) {
                        Log.e(TAG,"IOException. Msg: "+e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<BlocktradesSessionResponse> call, Throwable t) {
                Log.e(TAG, "getBlocktradeToken.onFailure. Msg: "+t.getMessage());
                Toast.makeText(getApplicationContext(), R.string.txt_no_internet_connection , Toast.LENGTH_SHORT).show();
                if(progress != null && progress.isShowing())
                    progress.dismiss();
            }
        });
    }

    /**
     * Checks if the fast transaction mode is enabled with the blocktrades interface
     * @return: True if fast transaction is enabled, false otherwise.
     */
    boolean checkIsFastTransaction(){
        if(!inputCoinType.equals("smartcoins")) {
            String blocktradeEmail = Helper.fetchStringSharePref(this, getString(R.string.pref_blocktrade_email));
            String blocktradePassword = Helper.fetchStringSharePref(this, getString(R.string.pref_blocktrade_password));
            String transactionUnder = Helper.fetchStringSharePref(this, getString(R.string.pref_blocktrade_limit));
            if (!blocktradeEmail.isEmpty() & !blocktradePassword.isEmpty() & !transactionUnder.isEmpty() & Double.parseDouble(transactionUnder) >= outputFiatAmount) {
                return true;
            }
        }
        return false;
    }

    /**
     * Method that encodes the invoice data and displays it in a QR-Code.
     * @param amount
     * @param fee
     */
    private void displayInvoiceData(double amount, double fee){
        double total = amount + fee;
        String merchantEmail = MerchantEmail.getMerchantEmail(getApplicationContext());
        LineItem[] items = new LineItem[]{ new LineItem("transfer", 1, total)};
        Invoice invoice = new Invoice(mUserAccount.getName(), "", "#blockpay", outputAsset.getSymbol(), items, "", "");

        ImageView imageView = (ImageView) findViewById(R.id.ivQR);
        try {
            Bitmap bitmap = encodeAsBitmap(Invoice.toQrCode(invoice), "#006500");
            imageView.setImageBitmap(bitmap);
            startTimer();

            AssetAmount requestedAmount = new AssetAmount(UnsignedLong.valueOf(Util.toBase(total, outputAsset.getPrecision())), outputAsset);
            Log.d(TAG, "Requested amount...: "+requestedAmount.getAmount().longValue());

            hideDialog();

            pendingTransferOperation = new TransferOperation(null, mUserAccount, requestedAmount);
        } catch (WriterException e) {
            Log.e(TAG, "WriterException. Msg: "+e.getMessage());
        }
    }

    /**
     * Method used to transfer funds to one or more parties.
     * @param operations: The list of operations
     * @param feeAsset: Asset to be used to pay the transfer fees
     * @param listener: Listener used to get notified of the result of the transaction
     */
    private void broadcastTransaction(List<BaseOperation> operations, Asset feeAsset, WitnessResponseListener listener){
        mOperationsCache = operations;
        mFeeAssetCache = feeAsset;
        ECKey currentPrivKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(null, privateKey).getKey().getPrivKeyBytes());
        final Transaction transaction = new Transaction(currentPrivKey, null, operations);
        WebsocketWorkerThread transferBroadcaster = new WebsocketWorkerThread(new TransactionBroadcastSequence(transaction, feeAsset, listener));
        transferBroadcaster.start();
    }

    /**
     * Creates an intent that sends the user to a confirmation activity.
     * In the case of having received a smartcoins transfer, the asset amount and the
     * sender information are also passed as parameters and forwarded to the ThankyouActivity,
     * so that the corresponding reward points can be issued.
     *
     * @param receivedAmount: The received amount
     * @param from: The user that sent us this amount of funds, and who should
     *            be rewarded with loyalty points
     */
    private void confirmationActivity(AssetAmount receivedAmount, UserAccount from){
        countDownTimer.cancel();
        Intent intent = new Intent(getApplicationContext(), ThankYouActivity.class);
        intent.putExtra(ThankYouActivity.INTENT_KEY_COIN_TYPE, inputCoinType);
        intent.putExtra(ThankYouActivity.INTENT_KEY_AMOUNT, tvAmount.getText().toString());
        intent.putExtra(ThankYouActivity.INTENT_KEY_COIN_NAME, inputCoinName);
        intent.putExtra(ThankYouActivity.INTENT_KEY_TEXT_AMOUNT, tvAmount.getText().toString());
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);

        // If we have partial payments, add them together and send the result to the next activity.
        if(partialPayments != null){
            AssetAmount sum = new AssetAmount(UnsignedLong.valueOf(0), receivedAmount.getAsset());
            for(AssetAmount amount : partialPayments){
                sum.add(amount);
            }
            intent.putExtra(ThankYouActivity.INTENT_KEY_RECEIVED_AMOUNT, sum.getAmount().longValue());
        }else if(receivedAmount != null){
            // If we don't have partial payments, it means we should just use the amount passed
            // as a parameter, if we have one.
            intent.putExtra(ThankYouActivity.INTENT_KEY_RECEIVED_AMOUNT, receivedAmount.getAmount().longValue());
        }

        // If the sender information is given, add it to the intent.
        if(from != null){
            intent.putExtra(ThankYouActivity.INTENT_KEY_REWARD_USER_ID, from.getObjectId());
        }

        startActivity(intent);
        finish();
    }

    /**
     * Private method called whenever a TransferOperation has been detected within a given transaction.
     * This method should react to this event based on whether or not the operation is an incoming transfer
     * and if it matches the expected incoming value.
     *
     * @param transferOperation: The transfer operation detected.
     */
    private void onTransferOperation(TransferOperation transferOperation){
        // First, we check if we're currently waiting for an incoming transaction
        Log.v(TAG,"onTransferOperation. id: "+transferOperation.getId());
        if(pendingTransferOperation != null){
            UserAccount from = transferOperation.getFrom();
            UserAccount to = transferOperation.getTo();

            // Second, we check that it is an incoming transfer operation
            if(to.equals(mUserAccount)){
                AssetAmount receivedAmount = transferOperation.getAssetAmount();
                Asset receivedAsset = database.fillAssetDetails(transferOperation.getAssetAmount().getAsset());
                Asset expectedAsset = pendingTransferOperation.getAssetAmount().getAsset();

                // Third, we check that the incoming transfer operation matches the same asset we're waiting for.
                if(receivedAsset.equals(expectedAsset)){
                    Log.d(TAG, "Incoming transaction");
                    Log.d(TAG, String.format("From...........: %s", from.getObjectId()));
                    Log.d(TAG, String.format("Amount.........: %d", receivedAmount.getAmount().longValue()));
                    Log.d(TAG, String.format("Asset symbol...: %s", receivedAsset.getSymbol()));
                    long expectedValue = pendingTransferOperation.getAssetAmount().getAmount().longValue();
                    long receivedValue = transferOperation.getAssetAmount().getAmount().longValue();
                    long valueTolerance = (long) (expectedValue * INCOMING_TRANSACTION_LOWER_TOLERANCE);
                    Log.d(TAG,String.format("Expected value: %d, actual value: %d, tolerance: %d", expectedValue, receivedValue, valueTolerance));
                    if(receivedValue >= expectedValue - valueTolerance){
                        /*
                         This is used just to filter out values that might not correspond to our currently requested
                         amount. Since we are already checking the source account and verifying that we have enough funds
                         it might be redundant. But for now let's just keep it.
                         */
                        if(receivedValue < expectedValue * INCOMING_TRANSACTION_UPPER_TOLERANCE){

                            // Calculate Bitshares Munich cut
                            Asset transferredAsset = pendingTransferOperation.getAssetAmount().getAsset();
                            long bmCut = (long) (receivedValue * Constants.BITSHARES_MUNICH_FEE);
                            AssetAmount bmTransferAmount = new AssetAmount(UnsignedLong.valueOf(bmCut), transferredAsset);

                            // Checking to see which fee multiplier to use
                            int feeMultiplier = 1;
                            if(this.canSendRewardPoints(new AssetAmount(UnsignedLong.valueOf(_outputFiatAmount), outputAsset))){
                                feeMultiplier = 2;
                            }
                            // Trying to determine which asset to use in order to pay for the transfer fee of the Bitshares Munich cut
                            Asset feeAsset;
                            AssetAmount coreAssetBalance = database.getBalance(mUserAccount, coreAsset);
                            if(coreAssetBalance.getAmount().longValue() > feeMultiplier * (Constants.FIXED_TRANSFER_FEE + Constants.EXTRA_NETWORK_FEE)){
                                feeAsset = coreAsset;
                            }else{
                                feeAsset = outputAsset;
                            }

                            // If we are dealing with an indirect conversion, we must bundle a buy order and
                            // a transfer operation in the same transaction
                            if(isIndirectConversion){

                                // Step 1 - Creating the limit order operation
                                long merchantsAmount = (long) (_inputFiatAmount * mEndToEndExchangeRate);
                                Date date = new Date();
                                int expirationTime = (int)(date.getTime() / 1000) + Transaction.DEFAULT_EXPIRATION_TIME;
                                LimitOrderCreateOperation limitOp = mOrderBook.exchange(mUserAccount,
                                        coreAsset,
                                        new AssetAmount(UnsignedLong.valueOf(merchantsAmount), outputAsset),
                                        expirationTime);

                                // Adding an additional base unit in order to compensate for what looks like a rounding error in the
                                // result coming from the OrderBook.exchange method
                                long additional = 1;
                                limitOp.setAmountToSell(limitOp.getAmountToSell().add(additional));

                                Log.d(TAG, "Amount to sell..: "+limitOp.getAmountToSell().getAmount().longValue());
                                Log.d(TAG, "Min to receive..: "+limitOp.getMinToReceive().getAmount().longValue());

                                // Step 2 - Creating a transfer operation
                                TransferOperationBuilder transferBuilder = new TransferOperationBuilder()
                                        .setSource(mUserAccount)
                                        .setDestination(bitsharesMunichAccount)
                                        .setTransferAmount(bmTransferAmount);

                                TransferOperation transferOp = transferBuilder.build();

                                ArrayList<BaseOperation> operations = new ArrayList<>();
                                operations.add(limitOp);
                                operations.add(transferOp);

                                broadcastTransaction(operations, feeAsset, bitsharesMunichTransferListener);
                            }else{
                                ArrayList<BaseOperation> operations = new ArrayList<>();

                                // Limit order used to buy some BTS for Bitshares Munich's cut & the transaction fees
                                Date date = new Date();
                                int expirationTime = (int)(date.getTime() / 1000) + Transaction.DEFAULT_EXPIRATION_TIME;
                                LimitOrderCreateOperation limitOperation = mOrderBook.exchange(mUserAccount,
                                        outputAsset,
                                        _bitsharesMunichCut.add(_transferCosts),
                                        expirationTime);

                                // Adding an additional base unit in order to compensate for what looks like a rounding error in the
                                // result coming from the OrderBook.exchange method
                                long additional = 1;
                                limitOperation.setAmountToSell(limitOperation.getAmountToSell().add(additional));
                                operations.add(limitOperation);

                                // Transferring Bitshares Munich's cut
                                TransferOperationBuilder builder = new TransferOperationBuilder()
                                        .setSource(mUserAccount)
                                        .setDestination(bitsharesMunichAccount)
                                        .setTransferAmount(_bitsharesMunichCut)
                                        .setFee(blockpayConverter.convert(
                                                Constants.FIXED_TRANSFER_FEE + Constants.EXTRA_NETWORK_FEE,
                                                BlockpayConverter.CORE_TO_OUTPUT));

                                operations.add(builder.build());

                                if(mTransferType == TransferType.PLATFORM_ASSET){
                                    // If we are dealing with a platform-asset transfer, we must still find out if we are
                                    // supposed to send some reward tokens.
                                    if(canSendRewardPoints(new AssetAmount(UnsignedLong.valueOf(_outputFiatAmount), outputAsset))){
                                        double rewardNumerator = Helper.fetchDoubleSharedPref(this, Constants.KEY_LOYALTY_REWARD_AMOUNT, 0.0);
                                        double rewardDenominator = Helper.fetchDoubleSharedPref(this, Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0.0);
                                        double rewardRatio = rewardNumerator / rewardDenominator;

                                        String rewardAssetId = Helper.fetchStringSharePref(this, Constants.KEY_REWARD_ASSET_ID);
                                        Asset rewardAsset = database.fillAssetDetails(new Asset(rewardAssetId));

                                        double received = Util.fromBase(pendingTransferOperation.getAssetAmount());
                                        double rewardAmount = received * rewardRatio;
                                        Log.d(TAG, String.format("Standard reward amount: %.8f, precision: %d", rewardAmount, rewardAsset.getPrecision()));
                                        long baseRewardAmount = Util.toBase(rewardAmount, rewardAsset.getPrecision());
                                        AssetAmount rewardAssetAmount = new AssetAmount(UnsignedLong.valueOf(baseRewardAmount), rewardAsset);
                                        Log.d(TAG, String.format("Sending %d of asset: %s", baseRewardAmount, rewardAsset.getObjectId()));
                                        Log.d(TAG,"Fee asset: "+feeAsset.getObjectId());
                                        TransferOperationBuilder transferOpBuilder = new TransferOperationBuilder()
                                                .setDestination(from)
                                                .setSource(mUserAccount)
                                                .setTransferAmount(rewardAssetAmount)
                                                .setFee(new AssetAmount(UnsignedLong.valueOf(1), feeAsset));
                                        operations.add(transferOpBuilder.build());
                                    }
                                }
                                broadcastTransaction(operations, feeAsset, bitsharesMunichTransferListener);
                            }

                            // Jumping to the confirmation activity
                            confirmationActivity(receivedAmount, from);
                        }else{
                            Log.w(TAG, "Incoming value out of tolerance limit");
                            Log.w(TAG, "Expected value: "+expectedValue);
                            Log.w(TAG, "Received value: "+receivedValue);
                        }
                    }else{
                        handlePartialPayment(transferOperation.getAssetAmount(), pendingTransferOperation.getAssetAmount());
                    }
                }else{
                    Log.w(TAG,"Unexpected asset type received");
                    Log.w(TAG,"Received asset: "+receivedAsset.getObjectId());
                    Log.w(TAG,"Expected asset: "+expectedAsset.getObjectId());
                }
            }else{
                Log.w(TAG,"Funds destination don't match");
                Log.w(TAG,"To...............: "+to.getObjectId());
                Log.w(TAG,"My user account..: "+mUserAccount.getObjectId());
            }
        }else{
            Log.w(TAG,"No pending transfer");
        }
    }

    /**
     * Method called when a partial payment has been detected. A sound will be played,
     * the screen will be updated and a small toast will be shown to the user notifying
     * him of the situation.
     *
     * The QR-code will then display a new image requesting the remaining funds.
     *
     * @param receivedAmount: The received value, in base units
     * @param expectedAmount: The expected value, in base units
     */
    private void handlePartialPayment(AssetAmount receivedAmount, AssetAmount expectedAmount){
        countDownTimer.cancel();
        startTimer();

        if(partialPayments == null){
            partialPayments = new ArrayList<AssetAmount>();
        }
        partialPayments.add(receivedAmount);

        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.transaction_error);
            mediaPlayer.start();
        } catch (Exception e) {
            Log.e(TAG,"Exception while playing partial payment sound");
        }

        // Converting the missing amount from the base
        AssetAmount missingAmount = expectedAmount.subtract(receivedAmount);
        double missingStandardAmount = Util.fromBase(missingAmount);
        inputFiatAmount = missingStandardAmount;
        setupFiatAmount();

        Log.d(TAG,"missingBaseAmount.......: "+missingAmount.getAmount());
        Log.d(TAG,"missingStandardAmount...: "+missingStandardAmount);

        Asset transferAsset = pendingTransferOperation.getAssetAmount().getAsset();
        String toastMessage = String.format(getResources().getString(R.string.warning_missing_funds), missingStandardAmount, transferAsset.getSymbol());
        Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show();
    }
}