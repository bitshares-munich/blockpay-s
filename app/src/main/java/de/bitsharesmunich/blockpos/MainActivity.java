package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.primitives.UnsignedLong;
import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;

import org.bitcoinj.core.DumpedPrivateKey;
import org.bitcoinj.core.ECKey;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.database.HistoricalTransferEntry;
import de.bitsharesmunich.graphenej.Address;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.Converter;
import de.bitsharesmunich.graphenej.PublicKey;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.Util;
import de.bitsharesmunich.graphenej.api.GetAccountBalances;
import de.bitsharesmunich.graphenej.api.GetAccounts;
import de.bitsharesmunich.graphenej.api.GetBlockHeader;
import de.bitsharesmunich.graphenej.api.GetMarketHistory;
import de.bitsharesmunich.graphenej.api.GetObjects;
import de.bitsharesmunich.graphenej.api.GetRelativeAccountHistory;
import de.bitsharesmunich.graphenej.api.ListAssets;
import de.bitsharesmunich.graphenej.api.LookupAssetSymbols;
import de.bitsharesmunich.graphenej.errors.ChecksumException;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.AccountProperties;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.BitAssetData;
import de.bitsharesmunich.graphenej.models.BlockHeader;
import de.bitsharesmunich.graphenej.models.BucketObject;
import de.bitsharesmunich.graphenej.models.HistoricalTransfer;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.objects.Memo;
import de.bitsharesmunich.graphenej.operations.TransferOperation;
import de.bitsharesmunich.interfaces.ExchangeRateListener;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.Altcoin;
import de.bitsharesmunich.models.ArrayAltCoins;
import de.bitsharesmunich.models.CapFeedResponse;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.models.SortedAltcoin;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.TinyDB;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();

    private final boolean DEBUG_SCREEN_SAVER = false;

    private final boolean DEBUG_EXCHANGE_RATE_UPDATE = false;

    private final boolean DEBUG_TRANSACTION_HISTORY = false;

    /* Constant used to fix the number of historical transfers to fetch from the network in one batch */
    private int HISTORICAL_TRANSFER_BATCH_SIZE = 100;

    /* Constant used to split the missing times and equivalent values in batches of constant time */
    private int MISSING_TIMES_BATCH_SIZE = 5;

    @BindView(R.id.ivCoin1) ImageView ivCoin1;
    @BindView(R.id.ivCoin2) ImageView ivCoin2;
    @BindView(R.id.ivCoin3) ImageView ivCoin3;
    @BindView(R.id.ivCoin4) ImageView ivCoin4;
    @BindView(R.id.ivCoin5) ImageView ivCoin5;
    @BindView(R.id.ivCoin6) ImageView ivCoin6;
    @BindView(R.id.ivCoin7) ImageView ivCoin7;
    @BindView(R.id.ivCoin8) ImageView ivCoin8;

    @BindView(R.id.tvCoin2) TextView tvCoin2;
    @BindView(R.id.tvCoin3) TextView tvCoin3;
    @BindView(R.id.tvCoin4) TextView tvCoin4;
    @BindView(R.id.tvCoin5) TextView tvCoin5;
    @BindView(R.id.tvCoin6) TextView tvCoin6;
    @BindView(R.id.tvCoin7) TextView tvCoin7;
    @BindView(R.id.tvCoin8) TextView tvCoin8;

    @BindView(R.id.drawer_layout_icon) ImageView drawer_layout_icon;

    ImageLoader imageLoader;
    Gson gson;
    CountDownTimer countDownTimer;
    private boolean isInFront;

    int screenSaverTime;

    Intent keypadActivityIntent;

    private Animation coinAnimation;

    /**
     * Variable holding the current user's private key in the WIF format
     */
    private String wifkey;

    /* Wrapper around shared preferences */
    private TinyDB tinyDB;

    /* Actual database interface instance */
    private BlockpayDatabase database;

    /* Current user account */
    private UserAccount mCurrentAccount;

    /**
     * List of assets with pending holder data to be updated
     */
    private LinkedList<Asset> mHoldersAssets;

    /* Thread used to obtain user's balances */
    private WebsocketWorkerThread getBalancesThread;

    /* Worker thread used to fetch missing asset data */
    private WebsocketWorkerThread listAssetsThread;

    /* Worker thread used to fetch asset objects */
    private WebsocketWorkerThread getObjectsThread;

    /* Websocket worker threads */
    private WebsocketWorkerThread transferHistoryThread;
    private WebsocketWorkerThread getMissingAccountsThread;
    private WebsocketWorkerThread getMissingAssets;
    private WebsocketWorkerThread getMissingTimes;
    private WebsocketWorkerThread getMarketHistoryThread;

    /**
     * Handler used with the getMarketHistoryThread thread
     */
    private GetRelativeAccountHistory mGetRelativeAccountHistory;

    /* List of block numbers with missing date information in the database */
    private LinkedList<Long> missingTimes;

    /* List of transactions for which we don't have the equivalent value data */
    private LinkedList<HistoricalTransferEntry> missingEquivalentValues;

    /* Counter used to keep track of the transfer history batch count */
    private int historicalTransferCount = 1;

    /**/
    private HistoricalTransfer lastHistoricalTransfer;

    /* Parameters to be used as the start and stop arguments in the 'get_relative_account_history' API call */
    private int start = 1;

    private Asset bitUSD = Constants.bitUSD;
    private Asset BTS = Constants.BTS;
    private Asset bitEUR = Constants.bitEUR;
    private Asset bitCNY = Constants.bitCNY;
    private Asset bitBTC = Constants.bitBTC;

    /**
     *  Currently selected output smartcoin
     */
    private Asset mSmartcoin;

    /**
    * Attribute used when trying to make a 2-step equivalent value calculation
    * This variable will hold the equivalent value of the UIA in BTS, that will in turn
    * have to be converted to the smartcoin of choice for the user
    */
    private AssetAmount coreCurrencyEqValue;

    /**
     * Exchange rate provider used to obtain current equivalent values in different assets
     */
    private ExchangeRateProvider mExchangeRateProvider;

    /* Websocket handler */
    private GetMarketHistory getMarketHistory;

    /**
     * Callback fired once we get the current account balances update
     */
    WitnessResponseListener mAccountBalancesListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            List<AssetAmount> balances = (List<AssetAmount>) response.result;
            database.putBalances(mCurrentAccount, balances);
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "balances.onError. Msg: "+error.message);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        gson = new Gson();
        setToolbarAndNavigationBar(getString(R.string.txt_pay_with), true);

        Helper.initImageLoader(getApplicationContext());
        imageLoader = ImageLoader.getInstance();

        database = new BlockpayDatabase(this);

        bitUSD = database.fillAssetDetails(bitUSD);
        BTS = database.fillAssetDetails(BTS);
        bitEUR = database.fillAssetDetails(bitEUR);
        bitCNY = database.fillAssetDetails(bitCNY);
        bitBTC = database.fillAssetDetails(bitBTC);

        String selectedSmartCoinString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin));
        Smartcoin selectedSmartCoin = gson.fromJson(selectedSmartCoinString, Smartcoin.class);
        mSmartcoin = new Asset(selectedSmartCoin.asset_id, selectedSmartCoin.symbol, Integer.parseInt(selectedSmartCoin.precision));
        Log.d(TAG,"Selected smartcoin id: "+mSmartcoin.getObjectId());

        long lastExchangeRateUpdate = Helper.fetchLongSharePref(this, Constants.KEY_LAST_EXTRA_BRIDGE_AMOUNT_UPDATE, 0);
        long now = System.currentTimeMillis();
        if(now - lastExchangeRateUpdate > Constants.EXCHANGE_RATES_UPDATE_PERIOD || DEBUG_EXCHANGE_RATE_UPDATE){
            mExchangeRateProvider = new ExchangeRateProvider(this, bitUSD, BTS, bitUSD2CoreListener);
            mExchangeRateProvider.dispatch();
        }

        try {
            String altAndCoin = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin));
            if (!altAndCoin.equals("null")) {
                ArrayAltCoins arrayAltCoins = gson.fromJson(altAndCoin, ArrayAltCoins.class);
                String sortedAltcoinsString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins));
                SortedAltcoin[] sortedAltcoins = gson.fromJson(sortedAltcoinsString, SortedAltcoin[].class);
                ImageView[] ivArray = {ivCoin2, ivCoin3, ivCoin4, ivCoin5, ivCoin6, ivCoin7, ivCoin8};
                TextView[] tvArray = {tvCoin2, tvCoin3, tvCoin4, tvCoin5, tvCoin6, tvCoin7, tvCoin8};
                ArrayList altcoinsDone = new ArrayList();
                int j = 0;
                for (int i = 0; i < sortedAltcoins.length; i++) {
                    Altcoin altcoin = getAltcoinFromSymbol(arrayAltCoins, sortedAltcoins[i].symbol);
                    if (altcoin == null){
                        Log.w(TAG,"Altcoin is null");
                        continue;
                    }
                    ivArray[j].setVisibility(View.VISIBLE);
                    tvArray[j].setVisibility(View.VISIBLE);
                    tvArray[j].setTag(altcoin.coinType);
                    tvArray[j].setText(altcoin.name.toUpperCase());
                    String identifier = altcoin.coinType;
                    if(identifier.equals("sbd")){
                        identifier = "steem";
                    }
                    String uri = "drawable://" + getResources().getIdentifier(identifier, "drawable", getPackageName());
                    imageLoader.displayImage(uri, ivArray[j]);


                    altcoinsDone.add(altcoin.symbol);
                    j++;
                }


                if (j < arrayAltCoins.altcoins.length) {
                    ArrayList<String> arrayList = arrayAltCoins.arrayList;
                    for (int i = 0; i < arrayAltCoins.altcoins.length; i++) {
                        Altcoin altcoins = arrayAltCoins.altcoins[i];
                        if (arrayList.contains(altcoins.name.toLowerCase())) {
                            if (altcoinsDone.contains(altcoins.symbol)) continue;

                            ivArray[j].setVisibility(View.VISIBLE);
                            tvArray[j].setVisibility(View.VISIBLE);
                            tvArray[j].setTag(altcoins.coinType);
                            tvArray[j].setText(altcoins.name.toUpperCase());
                            String uri = "drawable://" + getResources().getIdentifier(Helper.getCoinImagePath(altcoins.coinType), "drawable", getPackageName());
                            Log.d(TAG,"uri: "+uri);
                            imageLoader.displayImage(uri, ivArray[j]);
                            j++;
                        }
                    }
                }
            }
            keypadActivityIntent = new Intent(getApplicationContext(), KeypadActivity.class);
            coinAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.coin_animation);

            coinAnimation.setAnimationListener(new android.view.animation.Animation.AnimationListener() {

                @Override
                public void onAnimationEnd(Animation animation) {
                    startActivity(keypadActivityIntent);
                    overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
        } catch (Exception e) {
            exceptionEncountered(e);
        }
        getScreenSaverTimer();
        getSortedAltcoins();
        countDownTimer(screenSaverTime);
        updateBlockNumberHead();
        resetGravatar();

        tinyDB = new TinyDB(this);
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.get(i).isSelected) {
                mCurrentAccount = new UserAccount(accountDetails.get(i).account_id);
                wifkey = accountDetails.get(i).wif_key;
                break;
            }
        }

        // Retrieving account balances
        ArrayList<Asset> assetList = new ArrayList<>();
        getBalancesThread = new WebsocketWorkerThread(new GetAccountBalances(mCurrentAccount, assetList, mAccountBalancesListener));
        getBalancesThread.start();

        if(DEBUG_TRANSACTION_HISTORY){
            database.clearTransfers();
        }

        // Retrieving account transactions
        start = (historicalTransferCount * HISTORICAL_TRANSFER_BATCH_SIZE);
        List<HistoricalTransferEntry> transactions = database.getTransactions(mCurrentAccount, 0, Long.MAX_VALUE, BlockpayDatabase.ALL_TRANSACTIONS);
        if(transactions != null){
            Log.d(TAG, String.format("Got %d tx from database", transactions.size()));
            start += transactions.size() + HISTORICAL_TRANSFER_BATCH_SIZE;
        }else{
            Log.w(TAG,"transaction list is null");
        }
        Log.d(TAG,"GetRelativeAccountHistory. start: "+start+", current account: "+mCurrentAccount.getObjectId());
        mGetRelativeAccountHistory = new GetRelativeAccountHistory(mCurrentAccount, 0, HISTORICAL_TRANSFER_BATCH_SIZE, start, mTransferHistoryListener);
        transferHistoryThread = new WebsocketWorkerThread(mGetRelativeAccountHistory);
        transferHistoryThread.start();

        long lastAssetsUpdate = tinyDB.getLong(Constants.KEY_LAST_ASSET_LIST_UPDATE, 0);
        if(now > lastAssetsUpdate + TimeUnit.DAYS.toMillis(Constants.ASSETS_UPDATE_INTERVAL)){
            listAssetsThread = new WebsocketWorkerThread(new ListAssets("", ListAssets.LIST_ALL, mAssetsListener));
            listAssetsThread.start();
        }
    }

    /**
     * Callback activated once we get a response back from the full node telling us about the
     * transfer history of the current account.
     */
    private WitnessResponseListener mTransferHistoryListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mTransferHistoryListener.onSuccess");
            historicalTransferCount++;
            WitnessResponse<List<HistoricalTransfer>> resp = response;
            List<HistoricalTransferEntry> historicalTransferEntries = new ArrayList<>();
            boolean foundRepeated = false;
            Log.d(TAG, String.format("Got %d operations", resp.result.size()));

            // Getting decrypted private key in WIF format
            String wif = decryptWif();
            ECKey privateKey = DumpedPrivateKey.fromBase58(null, wif).getKey();
            PublicKey publicKey = new PublicKey(ECKey.fromPublicOnly(privateKey.getPubKey()));
            Address myAddress = new Address(publicKey.getKey());

            for (HistoricalTransfer historicalTransfer : resp.result) {
                HistoricalTransferEntry entry = new HistoricalTransferEntry();
                TransferOperation op = historicalTransfer.getOperation();
                if(op != null){
                    Memo memo = op.getMemo();

                    if (memo.getByteMessage() != null) {
                        Address destinationAddress = memo.getDestination();
                        try {
                            if (destinationAddress.toString().equals(myAddress.toString())) {
                                String decryptedMessage = Memo.decryptMessage(privateKey, memo.getSource(), memo.getNonce(), memo.getByteMessage());
                                memo.setPlaintextMessage(decryptedMessage);
                            }
                        } catch (ChecksumException e) {
                            Log.e(TAG, "ChecksumException. Msg: " + e.getMessage());
                        } catch (NullPointerException e) {
                            // This is expected in case the decryption fails, so no need to log this event.
                        }
                    }
                }else{
                    Log.v(TAG,"Non-transfer operation");
                    continue;
                }
                entry.setHistoricalTransfer(historicalTransfer);
                historicalTransferEntries.add(entry);

                // Looking for a repeated operation in the current batch
                if(lastHistoricalTransfer != null && historicalTransfer.getId().equals(lastHistoricalTransfer.getId())){
                    foundRepeated = true;
                }
            }

            // Inserting historical transfers into the database
            database.putTransactions(historicalTransferEntries);

            if(foundRepeated){
                Log.w(TAG,"Got repeated data, not requesting more operations!");
                // If we got a repeated historical transfer, it means we are done importing old
                // transactions. We can proceed to get other missing attributes like transaction
                // timestamps, asset references and equivalent values.
                handleMissingTransferData();
            }else{
                Log.d(TAG,"Got all new operations, requesting again..");
                start += HISTORICAL_TRANSFER_BATCH_SIZE;
                mGetRelativeAccountHistory.retry(0, HISTORICAL_TRANSFER_BATCH_SIZE, start);
//                transferHistoryThread = new WebsocketWorkerThread(new GetRelativeAccountHistory(mCurrentAccount, 0, HISTORICAL_TRANSFER_BATCH_SIZE, start, mTransferHistoryListener));
//                transferHistoryThread.start();

                // Storing the first historical transfer we got in a sort of cache used to know when to
                // stop requesting new historical transfers to the network. Basically we keep asking for
                // new transaction batches until we get a repeated HistoricalTransfer instance
                lastHistoricalTransfer = resp.result.get(0);
            }
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG,"mTransferHistoryListener.onError. Msg: "+error.message);
        }
    };

    /**
     * Checks the transfers table in order to find a potentially incomplete transfer information.
     * In case a missing information is found, we start a worker thread that will fetch that information.
     */
    private void handleMissingTransferData(){
        List<UserAccount> missingAccountNames = database.getMissingAccountNames();
        if (missingAccountNames.size() > 0) {
            // Got some missing user names, so we request them to the network.
            getMissingAccountsThread = new WebsocketWorkerThread(new GetAccounts(missingAccountNames, true, mGetmissingAccountsListener));
            getMissingAccountsThread.start();
        }

        List<Asset> missingAssets = database.getMissingAssets();
        if (missingAssets.size() > 0) {
            // Got some missing asset symbols, so we request them to the network.
            getMissingAssets = new WebsocketWorkerThread(new LookupAssetSymbols(missingAssets, mLookupAssetsSymbolsListener));
            getMissingAssets.start();
        }

        missingTimes = database.getMissingTransferTimes(MISSING_TIMES_BATCH_SIZE);
        if (missingTimes.size() > 0) {
            Long blockNum = missingTimes.peek();
            getMissingTimes = new WebsocketWorkerThread(new GetBlockHeader(blockNum, mGetMissingTimesListener));
            getMissingTimes.start();
        }
    }

    /**
     * Callback activated once we get a response back from the full node telling us about missing
     * user account data we previously requested.
     */
    private WitnessResponseListener mGetmissingAccountsListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(final WitnessResponse response) {
            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    List<AccountProperties> missingAccounts = (List<AccountProperties>) response.result;
                    database.putUserAccounts(missingAccounts);
                }
            });
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.d(TAG, "missing accounts. onError");
        }
    };

    /**
     * Callback activated whenever we get information about missing assets in the database.
     * If the missing asset happens to be the user's current base smartcoin, we update the
     * mSmartcoin reference, since this will be a new and complete Asset instance.
     */
    private WitnessResponseListener mLookupAssetsSymbolsListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(final WitnessResponse response) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "assetsUpdater.onSuccess");
                    List<Asset> assets = (List<Asset>) response.result;
                    // Updating the database
                    int count = database.putAssets(assets, null);

                    // If we has missing equivalent values that could not be processed until
                    // we had all the missing assets in the database, start processing them now.
                    if (missingEquivalentValues != null) {
                        processNextEquivalentValue();
                    }
                }
            });
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "assetsUpdater.onError");
        }
    };

    /**
     * Callback activated once we get a block header response.
     */
    private WitnessResponseListener mGetMissingTimesListener = new WitnessResponseListener() {

        @Override
        public void onSuccess(final WitnessResponse response) {
            if (missingTimes.size() > 1) {
                Log.d(TAG, "getMissingTime. onSuccess. remaining: " + (missingTimes.size() - 1));
            }

            BlockHeader blockHeader = (BlockHeader) response.result;
            boolean updated = database.setBlockTime(blockHeader, missingTimes.peek());
            if (!updated) {
                Log.w(TAG, "Failed to update time from transaction at block: " + missingTimes.peek());
            }
            missingTimes.poll();

            // If we still have missing times in the queue, work on them
            if (missingTimes.size() > 0) {
                long blockNum = missingTimes.peek();
                getMissingTimes = new WebsocketWorkerThread(new GetBlockHeader(blockNum, mGetMissingTimesListener));
                getMissingTimes.start();
            } else {
                // If we're done loading missing transfer times, we check for missing equivalent values.
                // By calling the 'getMarketHistoryThread' method we should get a list of all transfer
                // entries that are missing just the equivalent values, but DO HAVE time information.
                missingEquivalentValues = database.getMissingEquivalentValues();
                if (missingEquivalentValues.size() > 0) {
                    Log.i(TAG, "Finished loading missing times, now we can safely proceed to missing eq values");
                    processNextEquivalentValue();
                } else {
                    Log.w(TAG, "Got no missing equivalent value to fetch");
                }
            }
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "missingTimes. onError. Msg: "+error.message);
            missingTimes.poll();

            // If we still have missing times in the queue, work on them
            if (missingTimes.size() > 0) {
                long blockNum = missingTimes.peek();
                getMissingTimes = new WebsocketWorkerThread(new GetBlockHeader(blockNum, mGetMissingTimesListener));
                getMissingTimes.start();
            }
        }
    };

    /**
     * Assuming we have a list of missing equivalent values, this method will be called
     * to start the procedure needed to resolve a single missing equivalent value.
     * <p>
     * Since this procedure might have to be called repeated times, it was better isolated
     * in a private method.
     */
    private void processNextEquivalentValue() {
        if (missingEquivalentValues.size() > 0) {
            Log.d(TAG,"Missing equivalent value list size: "+missingEquivalentValues.size());
            List<Asset> missingAssets = database.getMissingAssets();
            if (missingAssets.size() == 0) {
                HistoricalTransferEntry transferEntry = missingEquivalentValues.peek();
                Asset transferredAsset = transferEntry.getHistoricalTransfer()
                        .getOperation()
                        .getAssetAmount()
                        .getAsset();

                while (transferredAsset.equals(mSmartcoin) && !mSmartcoin.equals(Constants.BTS)) {
                    // In case the transferred asset is the smartcoin itself, there is no need for
                    // a equivalent value calculation, and as such we just fill in the equivalent
                    // value fields and proceed to fetch the next HistoricalTransferEntry from our
                    // missingEquivalentValues list
                    Log.d(TAG,"No need to lookup value of smartcoin: "+mSmartcoin.getObjectId());
                    transferEntry.setEquivalentValue(new AssetAmount(transferEntry.getHistoricalTransfer().getOperation().getAssetAmount().getAmount(), transferredAsset));
                    database.updateEquivalentValue(transferEntry);

                    // DEBUG
                    TransferOperation op = transferEntry.getHistoricalTransfer().getOperation();
                    long blockNum = transferEntry.getHistoricalTransfer().getBlockNum();
                    Log.d(TAG,"Transfer from: "+op.getFrom()+", to: "+op.getTo()+", amount: "+op.getAssetAmount().getAmount().longValue()+", block num: "+blockNum);

                    missingEquivalentValues.poll();

                    transferEntry = missingEquivalentValues.peek();
                    if(transferEntry == null){
                        // If we get a null value, it means we're done with this batch of missing equivalent values
                        processNextMissingTime();
                        return;
                    }else{
                        transferredAsset = transferEntry.getHistoricalTransfer()
                                .getOperation()
                                .getAssetAmount()
                                .getAsset();
                    }
                }

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(transferEntry.getTimestamp() * 1000);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                Date startDate = calendar.getTime();
                Date endDate = calendar.getTime();

                Asset base = null;
                Asset quote = null;
                if (transferredAsset.equals(Constants.BTS)) {
                    // Directly fetch the BTS <-> smartcoin
                    base = database.fillAssetDetails(transferredAsset);
                    quote = database.fillAssetDetails(mSmartcoin);
                } else {
                    // We need to perform 2 conversions, namely
                    // Token <-> BTS <-> smartcoin
                    base = database.fillAssetDetails(transferredAsset);
                    quote = database.fillAssetDetails(Constants.BTS);
                }
                Log.d(TAG, String.format("initial times. start: %d, end: %d. %s -> %s", startDate.getTime(), endDate.getTime(), base.getObjectId(), quote.getObjectId()));
                if (base != null && quote != null) {
                    getMarketHistory = new GetMarketHistory(
                            base,
                            quote,
                            Constants.DEFAULT_BUCKET_SIZE,
                            startDate,
                            endDate,
                            mHistoricalMarketListener);
                    getMarketHistoryThread = new WebsocketWorkerThread(getMarketHistory);
                    getMarketHistoryThread.start();
                } else {
                    Log.w(TAG, "Either base or quote is null");
                }
            } else {
                // Don't do anything, the lookup_asset_symbol callback will check for missing
                // equivalent values again and pick up this series of tasks.
                Log.w(TAG, "We have some missing assets");
            }
        } else {
            // In case we're done loading missing times and equivalent values for this batch,
            // we can check if we have another batch of times and consequently missing equivalent
            // values to process.
            processNextMissingTime();
        }
    }

    /**
     * Checks to see if we have a new batch of unprocessed missing times in our transfer list.
     * If so, it loads a list of block numbers whose time we're missing and starts a thread that will
     * sequentially get those block's headers in order to find out their universal time timestamp.
     */
    private void processNextMissingTime(){
        missingTimes = database.getMissingTransferTimes(MISSING_TIMES_BATCH_SIZE);
        if (missingTimes.size() > 0) {
            Log.d(TAG, String.format("Got a new batch of %d missing times, so we're now going to process them", missingTimes.size()));
            Long blockNum = missingTimes.peek();
            getMissingTimes = new WebsocketWorkerThread(new GetBlockHeader(blockNum, mGetMissingTimesListener));
            getMissingTimes.start();
        } else {
            Log.d(TAG, "We're done with missing times, so this must be it...");
        }
    }

    /**
     * Called when we get a response from the 'get_market_history' API call
     */
    private WitnessResponseListener mHistoricalMarketListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG, "mHistoricalMarketListener.onSuccess");
//            if (getActivity() == null) {
//                Log.w(TAG, "Got no activity, quitting..");
//                return;
//            }
            List<BucketObject> buckets = (List<BucketObject>) response.result;
            HistoricalTransferEntry transferEntry = missingEquivalentValues.peek();
            if (buckets.size() > 0) {
                BucketObject bucket = buckets.get(buckets.size() - 1);

                AssetAmount transferAmount = transferEntry.getHistoricalTransfer().getOperation().getAssetAmount();

                Asset base = database.fillAssetDetails(bucket.key.base);
                Asset quote = database.fillAssetDetails(bucket.key.quote);

                if (quote.equals(mSmartcoin)) {
                    // Doing conversion and updating the database
                    Converter converter = new Converter(base, quote, bucket);
                    long convertedBaseValue = converter.convert(transferAmount, Converter.CLOSE_VALUE);
                    AssetAmount equivalentValue = new AssetAmount(UnsignedLong.valueOf(convertedBaseValue), mSmartcoin);
                    Log.d(TAG,"Setting converted value to: "+ Util.fromBase(equivalentValue)+", of asset: "+mSmartcoin.getObjectId());

                    transferEntry.setEquivalentValue(equivalentValue);
                    database.updateEquivalentValue(transferEntry);

                    // Removing the now solved equivalent value
                    missingEquivalentValues.poll();

                    // Process the next equivalent value, in case we have one
                    processNextEquivalentValue();
                } else {
                    AssetAmount originalTransfer = transferEntry.getHistoricalTransfer().getOperation().getAssetAmount();

                    // Doing conversion and updating the database
                    Converter converter = new Converter(base, quote, bucket);
                    long convertedBaseValue = converter.convert(originalTransfer, Converter.CLOSE_VALUE);
                    coreCurrencyEqValue = new AssetAmount(UnsignedLong.valueOf(convertedBaseValue), base);

                    base = database.fillAssetDetails(Constants.BTS);
                    quote = database.fillAssetDetails(mSmartcoin);

                    Log.d(TAG, String.format("Requesting conversion from %s -> %s", base.getSymbol(), quote.getSymbol()));

                    Calendar calendar = Calendar.getInstance();
                    calendar.setTimeInMillis(transferEntry.getTimestamp() * 1000);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    Date startDate = calendar.getTime();
                    Date endDate = calendar.getTime();

                    // Performing the 2nd step of the equivalent value calculation. We already hold the
                    // relationship UIA -> BTS, now we need the BTS -> Smartcoin for this time bucket.
                    getMarketHistory = new GetMarketHistory(
                            base,
                            quote,
                            Constants.DEFAULT_BUCKET_SIZE,
                            startDate,
                            endDate,
                            mHistoricalMarketSecondStepListener);
                    getMarketHistoryThread = new WebsocketWorkerThread(getMarketHistory);
                    getMarketHistoryThread.start();
                }
            } else {
                // Got no bucket for the specified time window. In this case we just expand the time
                // window by pushing the 'start' field further into the past using exponential increments.
                // The idea is not to waste time and network data transfer performing a sequential time search
                // of what seems to be a very inactive asset market.
                Asset transferAsset = transferEntry.getHistoricalTransfer().getOperation().getAssetAmount().getAsset();
                Log.w(TAG, String.format("Got no bucket from the requested time period for asset: %s , id: %s", transferAsset.getSymbol(), transferAsset.getObjectId()));
                Date currentStart = getMarketHistory.getStart();
                Calendar calendar = Calendar.getInstance();
                int previousCount = getMarketHistory.getCount() > 0 ? getMarketHistory.getCount() - 1 : 0;
                int currentCount = getMarketHistory.getCount() + 1;
                long previousExponentialFactor = (long) Math.pow(2, previousCount) * Constants.DEFAULT_BUCKET_SIZE * 1000;
                long newExponentialFactor = (long) Math.pow(2, currentCount) * Constants.DEFAULT_BUCKET_SIZE * 1000;
                Log.d(TAG, "Old start: "+currentStart.getTime());
                long adjustedStartValue = currentStart.getTime() + previousExponentialFactor - newExponentialFactor;
                Log.d(TAG, "New start: "+adjustedStartValue);
                calendar.setTimeInMillis(adjustedStartValue);
                getMarketHistory.setStart(calendar.getTime());
                Log.d(TAG,String.format("adjustedStartValue: %d, previousEF: %d, currentEF: %d", adjustedStartValue, previousExponentialFactor, newExponentialFactor));
                getMarketHistory.retry();
            }
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "historicalMarketListener.onError. Msg: " + error.message);
            // Removing this equivalent value task, even though it was not resolved
            missingEquivalentValues.poll();

            // Process the next equivalent value, in case we have one
            processNextEquivalentValue();
        }
    };

    private WitnessResponseListener mHistoricalMarketSecondStepListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG, "historicalMarketSecondStepListener.onSuccess");
//            if (getActivity() == null) {
//                Log.w(TAG, "Got no activity, quitting..");
//                return;
//            }
            List<BucketObject> buckets = (List<BucketObject>) response.result;
            HistoricalTransferEntry transferEntry = missingEquivalentValues.peek();

            if (buckets.size() > 0) {
                // Fetching the last bucket, just in case we have more than one.
                BucketObject bucket = buckets.get(buckets.size() - 1);

                Asset base = database.fillAssetDetails(bucket.key.base);
                Asset quote = database.fillAssetDetails(bucket.key.quote);

                // Doing conversion and updating the database
                Converter converter = new Converter(base, quote, bucket);
                long convertedBaseValue = converter.convert(coreCurrencyEqValue, Converter.CLOSE_VALUE);
                AssetAmount equivalentValue = new AssetAmount(UnsignedLong.valueOf(convertedBaseValue), mSmartcoin);

                // Updating equivalent value entry
                transferEntry.setEquivalentValue(equivalentValue);
                database.updateEquivalentValue(transferEntry);

                // Removing the now solved equivalent value
                missingEquivalentValues.poll();

                // Processing next value, if there is one.
                // Process the next equivalent value, in case we have one
                processNextEquivalentValue();
            } else {
                Date currentStart = getMarketHistory.getStart();
                int previousCount = getMarketHistory.getCount() > 0 ? getMarketHistory.getCount() - 1 : 0;
                int currentCount = getMarketHistory.getCount();
                long previousExponentialFactor = (long) Math.pow(2, previousCount) * Constants.DEFAULT_BUCKET_SIZE * 1000;
                long newExponentialFactor = (long) Math.pow(2, currentCount) * Constants.DEFAULT_BUCKET_SIZE * 1000;
                long adjustedStartValue = currentStart.getTime() + previousExponentialFactor - newExponentialFactor;

                Calendar calendar = Calendar.getInstance();
                calendar.setTimeInMillis(adjustedStartValue);
                getMarketHistory.setStart(calendar.getTime());
                getMarketHistory.retry();
            }
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "mHistoricalMarketSecondStepListener.onError. Msg: " + error.message);

            // Removing the now solved equivalent value
            missingEquivalentValues.poll();

            // Processing next value, if there is one.
            // Process the next equivalent value, in case we have one
            processNextEquivalentValue();
        }
    };

    /**
     * Listener that will get the bitUSD -> BTS exchange rate
     */
    private ExchangeRateListener bitUSD2CoreListener = new ExchangeRateListener() {
        @Override
        public void onExchangeRate(ExchangeRateResult exchangeRate) {
            Log.d(TAG,"onExchangeRate. USD -> BTS conversion rate: "+exchangeRate.conversionRate);
            int extraUSD = getResources().getInteger(R.integer.extra_usd_amount);
            double precisionFactor = Math.pow(10, BTS.getPrecision() - bitUSD.getPrecision());
            int extraBTS = (int) (precisionFactor * extraUSD * exchangeRate.conversionRate);
            Log.d(TAG,"extra BTS: "+extraBTS);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BITUSD, extraUSD);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BTS, extraBTS);
            mExchangeRateProvider = new ExchangeRateProvider(MainActivity.this, BTS, bitEUR, core2bitEURListner);
            mExchangeRateProvider.dispatch();
        }
    };

    /**
     * Listener that will get the BTS -> bitEUR exchange rate
     */
    private ExchangeRateListener core2bitEURListner = new ExchangeRateListener() {
        @Override
        public void onExchangeRate(ExchangeRateResult exchangeRate) {
            Log.d(TAG,"onExchangeRate. BTS -> EUR conversion rate: "+exchangeRate.conversionRate);
            long extraBTS = Helper.fetchLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BTS);
            double precisionFactor = Math.pow(10, bitEUR.getPrecision() - BTS.getPrecision());
            int extraEUR = (int) (precisionFactor * extraBTS * exchangeRate.conversionRate);
            Log.d(TAG,"extra bitEUR: "+extraEUR);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BITEUR, extraEUR);
            mExchangeRateProvider = new ExchangeRateProvider(MainActivity.this, BTS, bitCNY, core2bitCNYListener);
            mExchangeRateProvider.dispatch();
        }
    };

    /**
     * Listener that will get the BTS -> bitCNY exchange rate
     */
    private ExchangeRateListener core2bitCNYListener = new ExchangeRateListener() {
        @Override
        public void onExchangeRate(ExchangeRateResult exchangeRate) {
            Log.d(TAG,"onExchangeRate. BTS -> CNY conversion rate: "+exchangeRate.conversionRate);
            long extraBTS = Helper.fetchLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BTS);
            double precisionFactor = Math.pow(10, bitCNY.getPrecision() - BTS.getPrecision());
            int extraCNY = (int) (precisionFactor * extraBTS * exchangeRate.conversionRate);
            Log.d(TAG,"extra bitCNY: "+extraCNY);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BITCNY, extraCNY);
            mExchangeRateProvider = new ExchangeRateProvider(MainActivity.this, BTS, bitBTC, core2bitBTCListener);
            mExchangeRateProvider.dispatch();
        }
    };

    /**
     * Listener that will get the BTS -> bitBTC exchange rate
     */
    private ExchangeRateListener core2bitBTCListener = new ExchangeRateListener() {
        @Override
        public void onExchangeRate(ExchangeRateResult exchangeRate) {
            Log.d(TAG,"onExchangeRate. BTS -> BTC conversion rate: "+exchangeRate.conversionRate);
            long extraBTS = Helper.fetchLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BTS);
            int precisionFactor = (int) Math.pow(10, bitBTC.getPrecision() - BTS.getPrecision());
            int extraBTC = (int) (precisionFactor * extraBTS * exchangeRate.conversionRate);
            Log.d(TAG,"extra btc: "+extraBTC);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_EXTRA_BITBTC, extraBTC);
            Helper.storeLongSharePref(MainActivity.this, Constants.KEY_LAST_EXTRA_BRIDGE_AMOUNT_UPDATE, System.currentTimeMillis());
        }
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
            database.putAssets(assetList, null);

            List<Asset> witnessFedAssets = database.getWitnessFedAssets();
            ArrayList<String> ids = new ArrayList<>();
            for(Asset asset : witnessFedAssets){
                ids.add(asset.getBitassetId());
            }

            TinyDB tinyDB = new TinyDB(getApplicationContext());

            /* Specifying last asset list update */
            tinyDB.putLong(Constants.KEY_LAST_ASSET_LIST_UPDATE, System.currentTimeMillis());

            /*
             * Proceeding with the next step, which is to perform the 'get_object' API call on all
             * smartcoins in order to tell the smartcoins from the prediction market tokens apart
             */
            getObjectsThread = new WebsocketWorkerThread(new GetObjects(ids, mGetObjectListener));
            getObjectsThread.start();
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG, "mAssetListener. onError. Msg: "+ error.message);
        }
    };

    /**
     * Callback method that will be fired when we get a response from the network to
     * the 'get_objects' API call.
     */
    private WitnessResponseListener mGetObjectListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mGetObjectListener. onSuccess");
            List<BitAssetData> bitAssetDataArray = (List<BitAssetData>) response.result;
            for(BitAssetData assetData : bitAssetDataArray){
                Asset asset = database.getAssetByProperty(assetData.getObjectId());
                if(assetData.is_prediction_market){
                    database.updateAssetType(asset, Asset.AssetType.PREDICTION_MARKET);
                }else{
                    database.updateAssetType(asset, Asset.AssetType.SMART_COIN);
                }
            }

            Asset coreAsset = new Asset("1.3.0");
            database.updateAssetType(coreAsset, Asset.AssetType.CORE_ASSET);

            /* Database updated, we now look for missing holders data */
//            sendAssetHoldersRequest();
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG,"mGetObjectListener. onError. Msg: "+error.message);
        }
    };

    /**
     * Method that will pick the firs asset from the 'mHoldersAssets' list and make an HTTP request
     * to the cryptofresh API in order to obtain the number of account holders for that specific asset.
     */
    private void sendAssetHoldersRequest(){

        /* Retrieving a list of all assets whithout holders information if we don't already have that */
        if(mHoldersAssets == null){
            List<Asset> assetList = database.getAssets(-2, 0);
            Log.d(TAG, String.format("Got a list of %d assets whose holders data we need to obtain", assetList.size()));
            mHoldersAssets = new LinkedList<>();
            mHoldersAssets.addAll(assetList);
        }

        if(mHoldersAssets.peek() != null){
            ServiceGenerator sg = new ServiceGenerator(Constants.CRYPTOFRESH_URL);
            IWebService service = sg.getService(IWebService.class);
            Call<ResponseBody> postingService = service.getHolders(mHoldersAssets.peek().getSymbol());
            postingService.enqueue(mHoldersCallback);
        }
    }

    /**
     * Callback fired when we get a response from the cryptofresh API with details regarding
     * a specific asset's holders.
     */
    private Callback<ResponseBody> mHoldersCallback = new Callback<ResponseBody>() {
        @Override
        public void onResponse(Call<ResponseBody> call, final Response<ResponseBody> response) {
            Log.d(TAG,"mHoldersCallback.onResponse");
            if(response.isSuccessful()){
                Asset asset = mHoldersAssets.peek();
                try {
                    JSONObject holders = new JSONObject(response.body().string());
                    Log.d(TAG, "Got "+holders.length()+" holders");
                    database.updateAssetHolders(asset, holders.length());

                    // Removing this asset from the list
                    mHoldersAssets.poll();

//                    if(mHoldersAssets.size() > 0){
//                        sendAssetHoldersRequest();
//                    }else{
//                        TinyDB tinyDB = new TinyDB(getApplicationContext());
//                        tinyDB.putLong(Constants.KEY_LAST_ASSETS_HOLDERS_UPDATE, System.currentTimeMillis());
//                    }
                } catch (JSONException e) {
                    Log.e(TAG,"JsonException");
                } catch(IOException e){
                    Log.e(TAG,"IOException");
                }
            }else{
                try{
                    String errorBody = response.errorBody().string();
                    Log.e(TAG,"Error body: "+errorBody);
                }catch(IOException e){
                    Log.e(TAG,"IOException while retrieving server response. Msg: "+e.getMessage());
                }
            }
        }

        @Override
        public void onFailure(Call<ResponseBody> call, Throwable t) {
            Log.e(TAG,"mHoldersCallback.onFailure. Msg: "+t.getMessage());
        }
    };

    private void getScreenSaverTimer() {
        screenSaverTime = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver_time), ScreenSaverActivity.DEFAULT_SCREENSAVER_TRIGGER_TIME);
        if(DEBUG_SCREEN_SAVER == true){
            screenSaverTime = 1000;
        }else if (screenSaverTime == 0 || screenSaverTime == 1 || screenSaverTime == 9999) {
            screenSaverTime = 60000;
        } else if (screenSaverTime == 2) {
            screenSaverTime = 180000;
        } else if (screenSaverTime == 3) {
            screenSaverTime = 300000;
        }

    }

    @OnClick(R.id.ivCoin1)
    public void ivCoin1() {
        keypadActivityIntent.putExtra("inputCoinType", "smartcoins");
        keypadActivityIntent.putExtra("inputCoinName", "smartcoins");
        ivCoin1.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin2)
    public void ivCoin2() {
        String coin2 = tvCoin2.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin2);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin2.getText().toString());
        ivCoin2.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin3)
    public void ivCoin3() {
        String coin3 = tvCoin3.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin3);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin3.getText().toString());
        ivCoin3.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin4)
    public void ivCoin4() {
        String coin4 = tvCoin4.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin4);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin4.getText().toString());
        ivCoin4.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin5)
    public void ivCoin5() {
        String coin5 = tvCoin5.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin5);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin5.getText().toString());
        ivCoin5.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin6)
    public void ivCoin6() {
        String coin6 = tvCoin6.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin6);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin6.getText().toString());
        ivCoin6.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin7)
    public void ivCoin7() {
        String coin7 = tvCoin7.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin7);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin7.getText().toString());
        ivCoin7.startAnimation(coinAnimation);
    }

    @OnClick(R.id.ivCoin8)
    public void ivCoin8() {
        String coin8 = tvCoin8.getTag().toString();
        keypadActivityIntent.putExtra("inputCoinType", coin8);
        keypadActivityIntent.putExtra("inputCoinName", tvCoin8.getText().toString());
        ivCoin8.startAnimation(coinAnimation);
    }

    public Altcoin getAltcoinFromSymbol(ArrayAltCoins arrayAltCoins, String symbol) {
        Log.d(TAG,"getAltcoinFromSymbol. symbol: "+symbol);

        try {
            ArrayList<String> arrayList = arrayAltCoins.arrayList;
            for (int i = 0; i < arrayAltCoins.altcoins.length; i++) {
                Altcoin altcoin = arrayAltCoins.altcoins[i];
                if (arrayList.contains(altcoin.name.toLowerCase())) {
                    if (altcoin.symbol.equals(symbol))
                        return altcoin;
                }
            }
        } catch (Exception e) {
            Log.e(TAG,"Exception. Msg: "+e.getMessage());
            exceptionEncountered(e);
        }
        return null;
    }

    //count down time change
    private void countDownTimer(int minutes) {
        countDownTimer = new CountDownTimer(minutes, 1) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (isInFront) {
                    Intent intent = new Intent(getApplicationContext(), ScreenSaverActivity.class);
                    startActivity(intent);
                }
            }

        }.start();
    }

    @Override
    public void onResume() {
        super.onResume();
        resetGravatar();
        settingScreen();
        isInFront = true;
        countDownTimer.cancel();
        countDownTimer(screenSaverTime);
    }

    @Override
    public void onPause() {
        super.onPause();
        isInFront = false;
        countDownTimer.cancel();
    }

    @Override
    public void onUserInteraction() {
        countDownTimer.cancel();
        countDownTimer(screenSaverTime);
    }

    private void exceptionEncountered(Exception e) {
        Toast.makeText(getApplicationContext(), getApplicationContext().getString(R.string.exception_encountered_msg) + " :" + e.getMessage(), Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(), SettingActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @OnClick(R.id.drawer_layout_icon)
    public void drawer_layout_iconClick() {

        resetGravatarImage();
        settingScreen();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.openDrawer(GravityCompat.START);
    }

    public void getSortedAltcoins() {
        ServiceGenerator sg = new ServiceGenerator(Constants.CAPFEED_URL);
        IWebService service = sg.getService(IWebService.class);
        final Call<CapFeedResponse> postingService = service.getCapfeedData();
        postingService.enqueue(new Callback<CapFeedResponse>() {
            @Override
            public void onResponse(Call<CapFeedResponse> call, Response<CapFeedResponse> response) {
                if (response.isSuccessful()) {
//                    SortedAltcoinsList sortedAltcoinsList = response.body();
//                    Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins), sortedAltcoinsList.result);
                }
            }
            @Override
            public void onFailure(Call<CapFeedResponse> call, Throwable t) {
                Log.e(TAG, "capfeed.onFailure");
            }
        });
    }

    private String decryptWif() {
        String wif = null;
        try {
            wif = Crypt.getInstance().decrypt_string(wifkey);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "InvalidKeyException. Msg: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, "NoSuchAlgorithmException. Msg: " + e.getMessage());
        } catch (NoSuchPaddingException e) {
            Log.e(TAG, "NoSuchPaddingException. Msg: " + e.getMessage());
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, "InvalidAlgorithmParameterException. Msg: " + e.getMessage());
        } catch (IllegalBlockSizeException e) {
            Log.e(TAG, "IllegalBlockSizeException. Msg: " + e.getMessage());
        } catch (BadPaddingException e) {
            Log.e(TAG, "BadPaddingException. Msg: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            Log.e(TAG, "ClassNotFoundException. Msg: " + e.getMessage());
        } catch (IOException e) {
            Log.e(TAG, "IOException. Msg: " + e.getMessage());
        }
        return wif;
    }

    private void updateBlockNumberHead() {
        final Handler handler = new Handler();

        final Runnable updateTask = new Runnable() {

            @Override
            public void run() {
                BlockpayApplication app = (BlockpayApplication) getApplication();
                if (app.isConnected()) {
                    ivSocketConnected.setImageResource(R.drawable.icon_connected);
                    ivSocketConnected.clearAnimation();
                } else {
                    ivSocketConnected.setImageResource(R.drawable.icon_disconnected);
                    Animation myFadeInAnimation = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.flash);
                    ivSocketConnected.startAnimation(myFadeInAnimation);
                }
                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateTask, 1000);
    }
}
