package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.api.ListAssets;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.DatabaseListener;
import de.bitsharesmunich.models.BlocktradesTradingPair;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.TinyDB;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DatabaseLoadActivity extends AppCompatActivity implements DatabaseListener {
    private final String TAG = this.getClass().getName();

    /* Blockpay database interface */
    private BlockpayDatabase database;

    /* Worker thread used to fetch missing asset data */
    private WebsocketWorkerThread listAssetsThread;

    /**
     * Image placeholder
     */
    @BindView(R.id.imageView)
    ImageView logoView;

    /**
     * ProgressBar used to display the database download progress to the user
     */
    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    /**
     * TextView used to display the title of the current phase
     */
    @BindView(R.id.database_load_title)
    TextView progressTitle;

    /**
     * TextView used to display a more detailed loading message
     */
    @BindView(R.id.database_load_message)
    TextView progressMessage;

    /**
     * Button that will become available once the database loading is over or something has failed
     */
    @BindView(R.id.nextButton)
    Button nextButton;

    /**
     * Handler instance used to schedule tasks back to the main thread
     */
    private Handler mHandler;

    /**
     * Variable used to keep track of the URI that has been tried last
     */
    private int mUriIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_database_load);
        ButterKnife.bind(this);

        mHandler = new Handler();

        /* Getting database interface instance */
        database = new BlockpayDatabase(this);
        List<Asset> witnessFedAssets = database.getWitnessFedAssets();
        List<Asset> allAssets = database.getAssets();

        Log.d(TAG, "Asset size: "+witnessFedAssets.size());
        TinyDB tinyDB = new TinyDB(this);
        long now = System.currentTimeMillis();

        /* In case we have an empty database, we make a network call to fetch all known assets */
        if(allAssets.size() < Constants.MIN_ASSET_COUNT ||
                SettingActivity.DEBUG_ASSET_LIST) {
            listAssetsThread = new WebsocketWorkerThread(new ListAssets("", ListAssets.LIST_ALL, mAssetsListener), mUriIndex);
            listAssetsThread.start();
        }

        /* Check if we need to update the bridge fee rate information */
        long lastFeeRateUpdate = tinyDB.getLong(Constants.KEY_LAST_BRIDGE_RATES_UPDATE, -1);
        if(lastFeeRateUpdate == -1 || now > lastFeeRateUpdate + TimeUnit.HOURS.toMillis(Constants.BRIDGE_RATES_UPDATE_INTERVAL)){
            ServiceGenerator sg = new ServiceGenerator(Constants.BLOCKTRADES_URL);
            IWebService service = sg.getService(IWebService.class);
            Call<List<BlocktradesTradingPair>> tradingPairsCall = service.getTradingPairs();
            tradingPairsCall.enqueue(mTradingPairsCallback);
        }

        Animation fadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fadein);
        logoView.startAnimation(fadeInAnimation);
    }

    /**
     * Listener that will make the progress bar invisible after the shrink animation is over.
     */
    private Animation.AnimationListener mProgressBarDismissListener = new Animation.AnimationListener(){

        @Override
        public void onAnimationStart(Animation animation) {}

        @Override
        public void onAnimationEnd(Animation animation) {
            progressBar.setVisibility(View.INVISIBLE);
        }

        @Override
        public void onAnimationRepeat(Animation animation) {}
    };

    /**
     * Callback method that will be fired when we get a response from the network to
     * the 'get_assets' API call.
     */
    private WitnessResponseListener mAssetsListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mAssetListener.onSuccess");
            List<Asset> assetList = (List<Asset>) response.result;
            database.putAssets(assetList, DatabaseLoadActivity.this);

            List<Asset> witnessFedAssets = database.getWitnessFedAssets();
            ArrayList<String> ids = new ArrayList<>();
            for(Asset asset : witnessFedAssets){
                ids.add(asset.getBitassetId());
            }

            TinyDB tinyDB = new TinyDB(getApplicationContext());

            /* Specifying last asset list update */
            tinyDB.putLong(Constants.KEY_LAST_ASSET_LIST_UPDATE, System.currentTimeMillis());

            /* Specifying the database as loaded */
            tinyDB.putBoolean(Constants.KEY_DATABASE_LOADED, true);

            /* Setting up a message explaining the next step to the user*/
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    progressTitle.setText(getResources().getString(R.string.title_loading_asset_type_data));
                    progressMessage.setText(getResources().getString(R.string.msg_loading_asset_type_data));
                }
            });

            /**
             * Local database is ready
             */
            onAssetsReady();
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "mAssetListener. onError. Msg: "+ error.message);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    if(mUriIndex > BlockpayApplication.urlsSocketConnection.length - 1){
                        displayErrorTitle();
                    }else{
                        mUriIndex++;
                        listAssetsThread = new WebsocketWorkerThread(new ListAssets("", ListAssets.LIST_ALL, mAssetsListener), mUriIndex);
                        listAssetsThread.start();
                    }
                }
            });
        }
    };

    /**
     * Callback fired once we get the response to the trading pairs blocktrades API call.
     */
    private Callback<List<BlocktradesTradingPair>> mTradingPairsCallback = new Callback<List<BlocktradesTradingPair>>() {
        @Override
        public void onResponse(Call<List<BlocktradesTradingPair>> call, Response<List<BlocktradesTradingPair>> response) {
            Log.d(TAG, "tradingPairs.onResponse");
            database.putBridgeFees(response.body());
            TinyDB tinyDB = new TinyDB(getApplicationContext());
            tinyDB.putLong(Constants.KEY_LAST_BRIDGE_RATES_UPDATE, System.currentTimeMillis());
        }

        @Override
        public void onFailure(Call<List<BlocktradesTradingPair>> call, Throwable t) {
            Log.e(TAG,"tradingPairs.onFailure");
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    displayErrorTitle();
                }
            });
        }
    };

    private void onAssetsReady(){
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Animation animation = AnimationUtils.loadAnimation(DatabaseLoadActivity.this, R.anim.shrink);
                animation.setAnimationListener(mProgressBarDismissListener);
                progressBar.startAnimation(animation);
                nextButton.setVisibility(View.VISIBLE);
                progressTitle.setText(getResources().getString(R.string.title_assets_ready));
                progressMessage.setText(getResources().getString(R.string.msg_assets_loaded));
            }
        });
    }

    /**
     * Just sets the title to display error
     */
    private void displayErrorTitle(){
        progressTitle.setText(getResources().getString(R.string.error_title));
    }

    /**
     * Called whenever the user clicks on the 'next' button. This button will only be visible when
     * the database loading procedure is done, OR if there was an error in it.
     *
     * @param view: The button view.
     */
    @OnClick(R.id.nextButton)
    public void onNext(View view){
        Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
        database.close();
    }

    @Override
    public void onDatabaseUpdated(int code, final Bundle data) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                String unformattedMessage = getResources().getString(R.string.msg_loading_asset_in_database);
                progressMessage.setText(String.format(unformattedMessage, data.getInt(DatabaseListener.KEY_ASSET_UPDATE_COUNT)));
            }
        });
    }
}
