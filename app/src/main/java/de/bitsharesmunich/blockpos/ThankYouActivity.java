package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.GravatarDelegate;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.Gravatar;
import de.bitsharesmunich.models.MerchantEmail;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.models.Uia;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.TinyDB;

public class ThankYouActivity extends BaseActivity implements GravatarDelegate{
    private final String TAG = this.getClass().getName();

    public static final String INTENT_KEY_COIN_TYPE = "inputCoinType";
    public static final String INTENT_KEY_COIN_NAME = "amount";
    public static final String INTENT_KEY_AMOUNT = "inputCoinName";
    public static final String INTENT_KEY_RECEIVED_AMOUNT = "reward_amount";
    public static final String INTENT_KEY_REWARD_USER_ID = "reward_user_id";
    public static final String INTENT_KEY_TEXT_AMOUNT = "text_amount";

    @BindView(R.id.txtScreen) TextView tvAmount;
    @BindView(R.id.tvReward) TextView tvReward;
    @BindView(R.id.tvRewardStatic) TextView tvRewardStatic;
    @BindView((R.id.tvMyFable)) TextView tvMyFable;
    @BindView(R.id.ivCoin) ImageView ivCoin;
    @BindView((R.id.tvCoinName)) TextView tvCoinName;
    @BindView(R.id.ivMyFable) ImageView ivMyFable;
    @BindView(R.id.drawer_layout_icon) ImageView drawer_layout_icon;

    String coinType, coinName, amount;
    Smartcoin selectedSmartCoin;
    ImageLoader imageLoader;
    CountDownTimer countDownTimer;

    /**
     * The selected smartcoin asset, which is the one that we just received
     */
    private Asset smartcoinAsset;

    /**
     * Tiny wrapper around Shared Preferences
     */
    private TinyDB tinyDB;

    /**
     * Database interface
     */
    private BlockpayDatabase database;

    /* Current user account */
    private UserAccount mUserAccount;

    /* Private key of the currently selected account */
    private String privateKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thank_you);
        ButterKnife.bind(this);

        setToolbarAndNavigationBar(getString(R.string.txt_thank_you), true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        playSound();
        drawer_layout_icon.setVisibility(View.GONE);

        tinyDB = new TinyDB(this);

        database = new BlockpayDatabase(this);

        String email = MerchantEmail.getMerchantEmail(getApplicationContext());
        fetchGravatarInfo(email);

        /* Instantiating the selected smartcoin */
        String selectedSmartCoinString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin));
        selectedSmartCoin = gson.fromJson(selectedSmartCoinString, Smartcoin.class);
        smartcoinAsset = new Asset(selectedSmartCoin.asset_id, selectedSmartCoin.symbol, Integer.parseInt(selectedSmartCoin.precision));

        String fiatSymbol = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_fiat_symbol));
        String selectedUiaString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_uia_coin));
        Uia selectedUia = null;
        try {
            selectedUia = gson.fromJson(selectedUiaString, Uia.class);
        } catch (Exception e) {

        }

        Intent intent = getIntent();
        coinType = intent.getStringExtra(INTENT_KEY_COIN_TYPE);
        amount = intent.getStringExtra(INTENT_KEY_COIN_NAME);
        coinName = intent.getStringExtra(INTENT_KEY_AMOUNT);
        String textAmount = intent.getStringExtra(INTENT_KEY_TEXT_AMOUNT);

        // Setting the amount received
        tvAmount.setText(textAmount);

        imageLoader = ImageLoader.getInstance();
        String imgUri = "drawable://" + getResources().getIdentifier(Helper.getCoinImagePath(coinType), "drawable", getPackageName());
        imageLoader.displayImage(imgUri, ivCoin);


        if (coinType.equals("smartcoins")) {
            tvCoinName.setText(bitSymbol(selectedSmartCoin.symbol));
            if (intent.hasExtra("rewardAmount")) {
                if (selectedUia != null) {
                    tvRewardStatic.setVisibility(View.VISIBLE);
                    tvReward.setVisibility(View.VISIBLE);
                    tvReward.setText(intent.getFloatExtra("rewardAmount", 0) + " " + selectedUia.symbol.toUpperCase());
                }
            } else {
                tvReward.setText("");
            }
        } else {
            tvCoinName.setText(coinName);
        }

        List<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
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

        String senderUserId = intent.getStringExtra(INTENT_KEY_REWARD_USER_ID);
        long receivedBaseAmount = intent.getLongExtra(INTENT_KEY_RECEIVED_AMOUNT, -1);
        Log.d(TAG,"senderUserId: "+senderUserId);
        Log.d(TAG,"receiverBaseAmount: "+receivedBaseAmount);
//        if(receivedBaseAmount != -1 && senderUserId != null){
//            AssetAmount receivedAmount = new AssetAmount(UnsignedLong.valueOf(receivedBaseAmount), smartcoinAsset);
//            sendRewardPoints(new UserAccount(senderUserId), receivedAmount);
//        }
        countDownTimer();
    }

    /**
     * Method used to transfer the reward points
     * @param customer: The customer to whom we're sending the loyalty points
     * @param receivedAmount: The received amount, used to calculate how much loyalty points to send
     *                      to the customer.
     */
//    private void sendRewardPoints(UserAccount customer, AssetAmount receivedAmount){
//        Log.d(TAG,"sendRewardPoints");
//
//        // Fetching data needed to calculate the ratio
//        double rewardAmount = tinyDB.getDouble(Constants.KEY_LOYALTY_REWARD_AMOUNT, 0.0);
//        double smartcoinAmount = tinyDB.getDouble(Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0.0);
//
//        // If any of these values is equal to zero, it means that the loyalty reward has not been
//        // properly configured.
//        if(rewardAmount > 0 && smartcoinAmount > 0){
//            double rewardRatio = rewardAmount / smartcoinAmount;
//            Asset rewardAsset = database.fillAssetDetails(new Asset(tinyDB.getString(Constants.KEY_REWARD_ASSET_ID)));
//            if(rewardRatio != 0){
//                double standardReceivedAmount = Util.fromBase(receivedAmount);
//                double standardRewardAmount = standardReceivedAmount * rewardRatio;
//                Log.d(TAG, String.format("Standard reward amount: %.8f, precision: %d", standardRewardAmount, rewardAsset.getPrecision()));
//                long baseRewardAmount = Util.toBase(standardRewardAmount, rewardAsset.getPrecision());
//                AssetAmount rewardAssetAmount = new AssetAmount(UnsignedLong.valueOf(baseRewardAmount), rewardAsset);
//                Log.d(TAG, String.format("Sending %d of asset: %s", baseRewardAmount, rewardAsset.getObjectId()));
//                ECKey currentPrivKey = ECKey.fromPrivate(DumpedPrivateKey.fromBase58(null, privateKey).getKey().getPrivKeyBytes());
//
//                TransferTransactionBuilder builder = new TransferTransactionBuilder()
//                        .setSource(mUserAccount)
//                        .setDestination(customer)
//                        .setAmount(rewardAssetAmount)
//                        .setBlockData(BlockpayApplication.blockData)
//                        .setPrivateKey(currentPrivKey);
//                try {
//                    final Transaction transaction = builder.build();
//                    WebsocketWorkerThread transferBroadcaster = new WebsocketWorkerThread(new TransactionBroadcastSequence(transaction, smartcoinAsset, rewardListener));
//                    transferBroadcaster.start();
//                } catch (MalformedTransactionException e) {
//                    Log.e(TAG,"MalformedTransactionException. Msg: "+e.getMessage());
//                }
//            }else{
//                Log.w(TAG,"Reward ratio is 0");
//            }
//        }else{
//            Log.w(TAG,"Either reward or smartcoin amount is missing, can't calculate the ratio");
//        }
//    }

    /**
     * Callback method used to get the result of the loyalty points transfer.
     */
    private WitnessResponseListener rewardListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"onSuccess");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getResources().getString(R.string.msg_loyalty_points_transferred), Toast.LENGTH_SHORT).show();
                }
            });
        }

        @Override
        public void onError(final BaseResponse.Error error) {
            Log.e(TAG,"onError. Msg: "+error.message);
            Log.e(TAG,"error data message: "+error.data.message);
            runOnUiThread(new Runnable(){
                @Override
                public void run() {
                    String errorTemplate = getResources().getString(R.string.error_loyalty_points_transfer_failed);
                    String formattedError = String.format(errorTemplate, error.data.message);
                    Toast.makeText(getApplicationContext(), formattedError, Toast.LENGTH_LONG).show();
                }
            });
        }
    };

    @OnClick(R.id.btnCool)
    public void btnCool() {
        countDownTimer.cancel();
        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void countDownTimer() {
        countDownTimer = new CountDownTimer(15000, 1000) {

            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                btnCool();
            }

        }.start();
    }

    public void playSound() {
        try {
            MediaPlayer mediaPlayer = MediaPlayer.create(this, R.raw.ch_ching);
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onBackPressed() {
        moveToMainScreen();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            moveToMainScreen();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void moveToMainScreen()
    {
        Intent intent=new Intent(getApplicationContext(),MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }
    @Override
    public void updateProfile(Gravatar myGravatar) {
            tvMyFable.setText(getString(R.string.txt_by) + " " +myGravatar.companyName);
    }

    @Override
    public void updateCompanyLogo(Bitmap logo) {
        ivMyFable.setImageBitmap(logo);
    }

    @Override
    public void failureUpdateProfile() {

    }

    @Override
    public void failureUpdateLogo() {

    }
    GravatarDelegate instance(){
        return this;
    }
    void fetchGravatarInfo(String email){
        Gravatar.getInstance(instance()).fetch(email);
    }
    String bitSymbol(String symbol){
        if(!symbol.equals("BTS"))
            return ("bit"+symbol);
        else
            return (symbol);
    }

}
