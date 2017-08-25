package de.bitsharesmunich.utils;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.gson.GsonBuilder;

import java.io.Serializable;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import butterknife.ButterKnife;
import de.bitsharesmunich.blockpos.WebsocketWorkerThread;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.BlockData;
import de.bitsharesmunich.graphenej.ObjectType;
import de.bitsharesmunich.graphenej.Transaction;
import de.bitsharesmunich.graphenej.api.SubscriptionMessagesHub;
import de.bitsharesmunich.graphenej.interfaces.SubscriptionHub;
import de.bitsharesmunich.graphenej.interfaces.SubscriptionListener;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.DynamicGlobalProperties;
import de.bitsharesmunich.graphenej.models.SubscriptionResponse;
import de.bitsharesmunich.graphenej.operations.LimitOrderCreateOperation;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.interfaces.AssetDelegate;
import de.bitsharesmunich.interfaces.DatabaseListener;
import de.bitsharesmunich.interfaces.DatabaseNotifier;
import de.bitsharesmunich.interfaces.IBalancesDelegate;
import de.bitsharesmunich.interfaces.ITransactionDelegateWrtNumbers;
import de.bitsharesmunich.interfaces.UpdateTransaction;
import de.bitsharesmunich.models.BlocktradesTradingPair;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by qasim on 5/9/16.
 */
public class BlockpayApplication extends Application implements
        DatabaseNotifier,
        Application.ActivityLifecycleCallbacks,
        SubscriptionHub {
    private final static String TAG = "BlockpayApplication";

    private static Context context;
    private static ITransactionDelegateWrtNumbers iTransactionDelegacteWrtNunbers;
    private static AssetDelegate iAssetDelegate;
    private static UpdateTransaction iUpdateTransaction;
    public static String blockHead = "";
    private static Activity currentActivity;
    static IBalancesDelegate iBalancesDelegate_ereceiptActivity;
    public static String monitorAccountId;

    public static boolean screenSaverEnabled;

    /* Dynamic network information, required to broadcast valid transactions */
    public static BlockData blockData;

    /* Database interface instance */
    private BlockpayDatabase database;

    /**
     * List of entities that must be notified on a database change event.
     */
    private LinkedList<DatabaseListener> mDatabaseListeners;

    /**
     * Handler instance used to schedule tasks back to the main thread
     */
    private Handler mHandler;

    /**
     * Constant used to specify how long will the app wait for another activity to go through its starting life
     * cycle events before running the teardownConnectionTask task.
     *
     * This is used as a means to detect whether or not the user has left the app.
     */
    private final int DISCONNECT_DELAY = 1000;

    /**
     * GsonBuilder used to deserialize witness updates into basic graphene object instances
     */
    private GsonBuilder builder;

    /**
     * Custom deserializer for the SubcriptionResponse type that will only deserialize the required
     * object instances.
     */
    private SubscriptionResponse.SubscriptionResponseDeserializer subscriptionResponseDeserializer;

    /**
     * Background thread that will open a websocket connection with the full node
     */
    private WebsocketWorkerThread thread;

    /**
     * Central message broker
     */
    private SubscriptionMessagesHub mSubscriptionHub;

    /**
     * Runnable that will send an 'cancel_all_subscriptions' API call to the full node.
     *
     * This way we keep the connection open, but suspend all incoming messages in order to avoid
     * wasting user's data.
     *
     */
    private Runnable teardownConnectionTask = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG,"Cancelling subscriptions");

            // Cancelling all subscriptions and de-registering all request listeners
            mSubscriptionHub.cancelSubscriptions();

            /* We're out of the app now, so there's no current activity */
            BlockpayApplication.setCurrentActivity(null);
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
        }
    };

    public static String urlsSocketConnection[] =
            {
//                    "wss://de.blockpay.ch/node",               // German node
//                    "wss://fr.blockpay.ch/node",               // France node
                    "ws://128.0.69.157:8090",                  // Custom node
                    "wss://bitshares.openledger.info/ws",      // Openledger node
            };

    /**
     * Private variable used to keep track of the current
     */
    private int socketIndex = 0;

    public static void setCurrentActivity(Activity _activity) {
        BlockpayApplication.currentActivity = _activity;
    }

    public static Activity getCurrentActivity() {
        return BlockpayApplication.currentActivity;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        ButterKnife.setDebug(true);
        context = getApplicationContext();
        blockHead = "";
        accountCreateInit();

        /* Getting database interface instance */
        database = new BlockpayDatabase(this);

        TinyDB tinyDB = new TinyDB(this);
        long now = System.currentTimeMillis();

        /* Check if we need to update the bridge fee rate information */
        long lastFeeRateUpdate = tinyDB.getLong(Constants.KEY_LAST_BRIDGE_RATES_UPDATE, -1);
        if(lastFeeRateUpdate == -1 || now > lastFeeRateUpdate + TimeUnit.HOURS.toMillis(Constants.BRIDGE_RATES_UPDATE_INTERVAL)){
            ServiceGenerator sg = new ServiceGenerator(Constants.BLOCKTRADES_URL);
            IWebService service = sg.getService(IWebService.class);
            Call<List<BlocktradesTradingPair>> tradingPairsCall = service.getTradingPairs();
            tradingPairsCall.enqueue(mTradingPairsCallback);
        }

        mHandler = new Handler();

        subscriptionResponseDeserializer = new SubscriptionResponse.SubscriptionResponseDeserializer();

        /* Creating a builder and setting up the proper deserialization de.bitsharesmunich.adapters */
        builder = new GsonBuilder();
        builder.registerTypeAdapter(Transaction.class, new Transaction.TransactionDeserializer());
        builder.registerTypeAdapter(TransferOperation.class, new TransferOperation.TransferDeserializer());
        builder.registerTypeAdapter(LimitOrderCreateOperation.class, new LimitOrderCreateOperation.LimitOrderCreateDeserializer());
        builder.registerTypeAdapter(AssetAmount.class, new AssetAmount.AssetAmountDeserializer());
        builder.registerTypeAdapter(SubscriptionResponse.class, subscriptionResponseDeserializer);

        /*
        * Registering this class as a listener to all activitie's callback cycle events, in order to
        * better estimate when the user has left the app and it is safe to disconnect the websocket connection
        */
        registerActivityLifecycleCallbacks(this);
    }

    /**
     * This method will start a persistent connection to the full node in a background thread.
     *
     * If there already is a connection, when this method is called, then it probably means the
     * user is returning to the same instance of the app before it was destroyed and we must
     * try to recycle the existing connection by just calling the resubscribe method.
     */
    public void startNodeConnection() {
        if(thread == null || !thread.isConnected()){
            mSubscriptionHub = new SubscriptionMessagesHub("","", true, mErrorListener);
            mSubscriptionHub.addSubscriptionListener(mGlobalPropertiesListener);
            thread = new WebsocketWorkerThread(mSubscriptionHub, socketIndex);
            thread.start();
        }else if(thread.isConnected() &&
                mSubscriptionHub != null &&
                !mSubscriptionHub.isSubscribed()){
            mSubscriptionHub.resubscribe();
        }else{
            Log.w(TAG,"No criteria was satisfied");
        }
    }

    /**
     * Class passed to the SubscriptionMessagesHub in order to get notified of any error in the
     * connection with the full node.
     */
    private WitnessResponseListener mErrorListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG, "onSuccess");
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "errorListener.onError. Msg: " + error.message);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    socketIndex = (socketIndex + 1) % urlsSocketConnection.length;
                    thread.disconnect();
                    startNodeConnection();
                }
            });
        }
    };

    /**
     * The application class itself has a listener that will be registered in order to get
     * frequent updates of the dynamic_global_property_object, which is broadcasted
     * once a new block is created.
     *
     * We use this in order to keep an updated instance of the blockData.
     */
    private SubscriptionListener mGlobalPropertiesListener = new SubscriptionListener() {

        @Override
        public ObjectType getInterestObjectType() {
            return ObjectType.DYNAMIC_GLOBAL_PROPERTY_OBJECT;
        }

        @Override
        public void onSubscriptionUpdate(SubscriptionResponse subscriptionResponse) {
            if(subscriptionResponse != null){
                List<Serializable> subArray = (List<Serializable>) subscriptionResponse.params.get(1);
                for(Serializable serializable : subArray){
                    if(serializable instanceof DynamicGlobalProperties){
                        DynamicGlobalProperties dynamicProperties = (DynamicGlobalProperties) serializable;

                        // Adjusting dynamic block data to every transaction
                        long expirationTime = (dynamicProperties.time.getTime() / 1000) + Transaction.DEFAULT_EXPIRATION_TIME;
                        String headBlockId = dynamicProperties.head_block_id;
                        long headBlockNumber = dynamicProperties.head_block_number;
                        if(blockData == null){
                            blockData = new BlockData(headBlockNumber, headBlockId, expirationTime);
                        }else{
                            blockData.setRefBlockNum(headBlockNumber);
                            blockData.setRefBlockPrefix(headBlockId);
                            blockData.setExpiration(expirationTime);
                        }
                    }
                }
            }else{
                Log.w(TAG, "subscriptionResponse was null!");
            }
        }
    };

    public static void registerBalancesDelegateEReceipt(IBalancesDelegate callbackClass) {
        iBalancesDelegate_ereceiptActivity = callbackClass;
    }

    public static void registerDelegateTransactionWrtNumbers(ITransactionDelegateWrtNumbers callbackClass) {
        iTransactionDelegacteWrtNunbers = callbackClass;
    }

    public static void registerAssetDelegate(AssetDelegate callbackClass) {
        iAssetDelegate = callbackClass;
    }

    public static void registerUpdateTransactionDelegate(UpdateTransaction callbackClass) {
        iUpdateTransaction = callbackClass;
    }

    public static Boolean isReady = false;
    public static int nodeIndex = 0;

    /**
     * Method that will schedule a connection.
     */
    private void scheduleConnection() {
        Log.d(TAG, "scheduleConnection");
        isReady = false;
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                nodeIndex = nodeIndex % urlsSocketConnection.length;
                Log.d(TAG,"Connecting to: "+urlsSocketConnection[nodeIndex]);
                connect(urlsSocketConnection[nodeIndex]);
                nodeIndex++;
            }
        });
    }

    private static boolean mIsConnected = false;

    Handler connectionHandler = new Handler();

    private void connect(String node) {
//        Log.d(TAG, "connect");
//        try {
//            if (!mIsConnected ) {
//                mServerURI = new URI(node);
//                connectedSocket = node;
//                Log.d(TAG, String.format("Connecting to node: %s", mServerURI));
//                mConnection.connect(mServerURI, this);
//            }
//        }
//        catch (URISyntaxException e) {
//            Log.e(TAG, "URISyntaxException. Msg :"+e.getMessage());
//            checkConnection();
//        } catch (WebSocketException e) {
//            Log.e(TAG, "WebSocketException. Mag :"+e.getMessage());
//            if ( !mIsConnected ) {
//                checkConnection();
//            }
//        }
    }

    public void toggleScreensaver(boolean enabled){
//        screenSaverEnabled = enabled;
//        Log.d(TAG, "toggleScreensaver. enabled: " + enabled);
//        if(enabled && mIsConnected && mConnection != null){
//            disconnect();
//        }
    }

    /**
     * Method that checks the current connectivity state and returns true in case it has detected one,
     * and false otherwise.
     * @return
     */
    private boolean checkInternetConnection(){
        ConnectivityManager cm =
                (ConnectivityManager) getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
    }

    public static void timeStamp(){

        Helper.storeBoolianSharePref(context,"account_can_create",false);
        setTimeStamp();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

                Helper.storeBoolianSharePref(context,"account_can_create",true);

            }
        }, 10 * 60000);
    }
    @NonNull
    public static Boolean accountCanCreate(){
        return Helper.fetchBoolianSharePref(context,"account_can_create");
    }
    void accountCreateInit(){
        if(Helper.containKeySharePref(context,"account_can_create")){
            if(!accountCanCreate()){
                getTimeStamp();
            }
        }else {
            Helper.storeBoolianSharePref(context,"account_can_create",true);
        }
    }
    static void setTimeStamp(){
        Calendar c = Calendar.getInstance();
        long time = c.getTimeInMillis();
        Helper.storeLongSharePref(context,"account_create_timestamp",time);
    }
    static void getTimeStamp(){
        try {
            Calendar c = Calendar.getInstance();
            long currentTime = c.getTimeInMillis();;
            long oldTime = Helper.fetchLongSharePref(context, "account_create_timestamp");
            long diff = currentTime-oldTime;
            if(diff < TimeUnit.MINUTES.toMillis(10)){
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        Helper.storeBoolianSharePref(context,"account_can_create",true);
                    }
                }, TimeUnit.MINUTES.toMillis(10)-diff);
            }else {
                Helper.storeBoolianSharePref(context,"account_can_create",true);
            }
        }catch (Exception e){
            Helper.storeBoolianSharePref(context,"account_can_create",true);
        }
    }

    @NonNull
    public boolean isConnected(){
        return thread.isConnected();
    }

    @Override
    public void addListener(DatabaseListener listener) {
        if(mDatabaseListeners == null){
            mDatabaseListeners = new LinkedList<>();
        }
        mDatabaseListeners.add(listener);
    }

    @Override
    public void removeListener(DatabaseListener listener) {
        if(mDatabaseListeners == null) return;
        for(int i = 0; i < mDatabaseListeners.size(); i++){
            if(mDatabaseListeners.get(i) == listener){
                mDatabaseListeners.remove(i);
            }
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        mHandler.removeCallbacks(this.teardownConnectionTask);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        mHandler.removeCallbacks(this.teardownConnectionTask);
        startNodeConnection();
    }

    @Override
    public void onActivityResumed(Activity activity) {
        mHandler.removeCallbacks(this.teardownConnectionTask);
        BlockpayApplication.setCurrentActivity(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        mHandler.postDelayed(this.teardownConnectionTask, DISCONNECT_DELAY);
    }

    @Override
    public void onActivityStopped(Activity activity) {}

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {}

    @Override
    public void onActivityDestroyed(Activity activity) {}

    @Override
    public void addSubscriptionListener(SubscriptionListener listener) {
        mSubscriptionHub.addSubscriptionListener(listener);
    }

    @Override
    public void removeSubscriptionListener(SubscriptionListener listener) {
        mSubscriptionHub.removeSubscriptionListener(listener);
    }

    @Override
    public List<SubscriptionListener> getSubscriptionListeners() {
//        int size = subscriptionResponseDeserializer.getSubscriptionListeners().size();
//        return subscriptionResponseDeserializer.getSubscriptionListeners().toArray(new SubscriptionListener[size]);
        return subscriptionResponseDeserializer.getSubscriptionListeners();
    }
}