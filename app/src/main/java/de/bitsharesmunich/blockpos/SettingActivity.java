package de.bitsharesmunich.blockpos;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextWatcher;
import android.transition.Explode;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;
import com.rey.material.widget.CheckBox;

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import ar.com.daidalos.afiledialog.FileChooserDialog;
import ar.com.daidalos.afiledialog.FileChooserLabels;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import butterknife.OnClick;
import butterknife.OnItemSelected;
import de.bitsharesmunich.adapters.CountryAdapter;
import de.bitsharesmunich.adapters.CurrenciesAdapter;
import de.bitsharesmunich.adapters.UserIssuedAssetAdapter;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.AssetAmount;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.api.GetAccountBalances;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.AssetDelegate;
import de.bitsharesmunich.interfaces.BackupBinDelegate;
import de.bitsharesmunich.interfaces.DatabaseListener;
import de.bitsharesmunich.interfaces.GravatarDelegate;
import de.bitsharesmunich.models.Account;
import de.bitsharesmunich.models.AccountAssets;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.AccountUpgrade;
import de.bitsharesmunich.models.Altcoin;
import de.bitsharesmunich.models.ArrayAltCoins;
import de.bitsharesmunich.models.AssetDescription;
import de.bitsharesmunich.models.CCAssets;
import de.bitsharesmunich.models.CapFeedResponse;
import de.bitsharesmunich.models.CapfeedCryptoCurrency;
import de.bitsharesmunich.models.CoinMarketCapItemResponse;
import de.bitsharesmunich.models.Gravatar;
import de.bitsharesmunich.models.LangCode;
import de.bitsharesmunich.models.LtmFee;
import de.bitsharesmunich.models.MerchantEmail;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.models.SortedAltcoin;
import de.bitsharesmunich.models.TransactionDetails;
import de.bitsharesmunich.models.Uia;
import de.bitsharesmunich.utils.BinHelper;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Country;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.PermissionManager;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.SupportMethods;
import de.bitsharesmunich.utils.TinyDB;
import me.grantland.widget.AutofitTextView;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingActivity extends BaseActivity implements AssetDelegate,GravatarDelegate,BackupBinDelegate, DatabaseListener {
    private final String TAG = this.getClass().getName();

    public static final boolean DEBUG_ASSET_LIST = false;

    /**
     * Maximum number of words that a description can have in order to be displayed at the
     * asset list.
     */
    public final int MAX_DESCRIPTION_LENGTH = 4;

    /**
     * Enum used to specify our potential sources of market cap data
     */
    private enum MarketCapSource {
        CAPFEED,
        COIN_MARKETCAP
    }

    @BindView(R.id.ivGravatar)
    ImageView ivGravatar;

    @BindView(R.id.spCountry)
    Spinner spCountry;

    @BindView(R.id.spCurrency)
    Spinner spCurrency;

    @BindView(R.id.spLanguage)
    Spinner spLanguage;

    @BindView(R.id.spDateAndTime)
    Spinner spDateAndTime;

    @BindView(R.id.llBitCoinAndAltCoins)
    LinearLayout llBitCoinAndAltCoins;

    @BindView(R.id.tvRewardFor)
    AutofitTextView tvRewardFor;

    @BindView(R.id.etRewardToGive)
    EditText etRewardToGive;

    @BindView(R.id.etRewardForSpending)
    EditText etRewardForSpending;

    @BindView(R.id.tvCurrencyRewardLeft)
    AutofitTextView tvCurrencyRewardLeft;

    @BindView(R.id.tvCurrencyRewardRight)
    AutofitTextView tvCurrencyRewardRight;

    @BindView(R.id.etScFolder)
    EditText etScFolder;

    @BindView(R.id.rbScDefault)
    RadioButton rbScDefault;

    @BindView(R.id.rbScMyGravatar)
    RadioButton rbScMyGravatar;

    @BindView(R.id.rbScFolder)
    RadioButton rbScFolder;

    @BindView(R.id.rbThreeMint)
    RadioButton rbThreeMint;

    @BindView(R.id.rbFiveMint)
    RadioButton rbFiveMint;

    @BindView(R.id.rbSevenMint)
    RadioButton rbSevenMint;

    @BindView(R.id.spAccounts)
    Spinner spAccounts;

    @BindView(R.id.tvAccounts)
    TextView tvAccounts;

    @BindView(R.id.register_new_account)
    Button registerNewAccount;

    @BindView(R.id.import_new_account)
    Button importNewAccount;

    Gson gson;
    CCAssets ccAssets;
    ArrayAltCoins arrayAltCoins;
    HashMap altcoinsIndexMap = new HashMap();
    ArrayList altcoinsNameArr = new ArrayList();
    ImageLoader myImageLoader;

    TinyDB tinyDB;

    ArrayList<AccountDetails> accountDetails;

    ProgressDialog progressDialog;
    Activity activity;
    String wifKey = "";

    int accountDetailsId;

    @BindView(R.id.ivLifeTime)
    ImageView ivLifeTime;

    @BindView(R.id.upgrade_account)
    Button btnUpgrade;

    @BindView(R.id.btn_blocktrade_account)
    Button btnBlocktradeAccount;

    @BindView(R.id.rewardAsset)
    AutoCompleteTextView mRewardAssetAutocomplete;

    @BindView(R.id.rewardContainer)
    LinearLayout rewardRatioContainer;

    /* Thread used to obtain user's balances */
    private WebsocketWorkerThread getBalancesThread;

    /* Current user account */
    private UserAccount mCurrentAccount;

    /**
     * Blockpay database interface instance
     */
    private BlockpayDatabase database;

    /**
     * Used to ignore the first time the onItemSelectedListener is called
     */
    private boolean mCountrySpinnerEnabled;

    /**
     * Private enum class used to keep track of the reward ratio
     * status
     */
    private enum RewardRatioStatus {
        NOT_NEEDED,
        SPECIFIED,
        NOT_SPECIFIED
    }

    /**
     * Instance of the enum class used to keep track of the reward ratio status
     */
    private RewardRatioStatus mRewardRatioStatus = RewardRatioStatus.NOT_NEEDED;

    /**
     * Cryptocurrencies that are supported by the bridge but we must not display in the list
     */
    private String[] cryptocurrenciesBlacklist = new String[] {
            "Agoras",
            "BitShares Maker",
            "BlockTrades Labor",
            "Brownie Points",
            "Synereo (amp)",
            "Tester Bitcoin",
            "Tester Litecoin",
            "Vests",
            "DAO",
            "Mainstreet Token Offering"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        ButterKnife.bind(this);

        // Database interface
        database = new BlockpayDatabase(this);

        // Shared preference wrapper
        tinyDB = new TinyDB(getApplicationContext());

        // Setting up the reward token autocomplete text view and its adapter
        UserIssuedAssetAdapter adapter = new UserIssuedAssetAdapter(this);
        mRewardAssetAutocomplete.setAdapter(adapter);
        mRewardAssetAutocomplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                // Hiding the keyboard after an UIA item has been selected
                InputMethodManager imm = (InputMethodManager) getSystemService(view.getContext().INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(view.getApplicationWindowToken(), 0);

                Asset rewardToken = database.getAssetBySymbol(((AppCompatTextView)view).getText().toString());
                if(rewardToken != null){
                    tinyDB.putString(Constants.KEY_REWARD_ASSET_ID, rewardToken.getObjectId());
                    verifyRewardTokenOwnership(rewardToken);
                }
            }
        });

        // Setting up text change listener
        mRewardAssetAutocomplete.addTextChangedListener(mRewardTokenSelectionWatcher);

        // Setting up the current UI state depending on the currently selected reward asset
        Asset rewardAsset = database.fillAssetDetails(new Asset(tinyDB.getString(Constants.KEY_REWARD_ASSET_ID)));
        if(rewardAsset != null){
            mRewardAssetAutocomplete.setText(rewardAsset.getSymbol());
        }else{
            mRewardAssetAutocomplete.setText("");
            rewardRatioContainer.setVisibility(View.GONE);
        }

        BlockpayApplication.registerAssetDelegate(this);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);

        setToolbarAndNavigationBar(getString(R.string.txt_settings), false);
        boolean displayBack = getIntent().getBooleanExtra(Constants.KEY_DISPLAY_BACK, false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(displayBack);
        progressDialog = new ProgressDialog(this);
        activity = this;
        accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        PermissionManager Manager = new PermissionManager();
        Manager.verifyStoragePermissions(this);

        myImageLoader = ImageLoader.getInstance();
        myImageLoader.init(ImageLoaderConfiguration.createDefault(this));

        initFileChooserDialog();

        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        arrayAltCoins = new ArrayAltCoins();
        if (accountDetails.size() == 0) {
            btnUpgrade.setEnabled(false);
            btnUpgrade.setBackgroundColor(Color.rgb(211, 211, 211));
        }

        saveTwoImageToSD();

        gson = new Gson();


        String email = MerchantEmail.getMerchantEmail(getApplicationContext());
        fetchGravatarInfo(email);

        double rewardAmount = tinyDB.getDouble(Constants.KEY_LOYALTY_REWARD_AMOUNT, 0);
        double smartcoinAmount = tinyDB.getDouble(Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0);
        NumberFormat nf = new DecimalFormat("##.###");
        if(rewardAmount > 0)
            etRewardToGive.setText(nf.format(rewardAmount));
        if(smartcoinAmount > 0)
            etRewardForSpending.setText(nf.format(smartcoinAmount));

        String currencyReward = Helper.fetchStringSharePref(getApplicationContext(), "currency_reward");
        Boolean orientationSymbol = Helper.fetchBoolianSharePref(getApplicationContext(), getString(R.string.orientation_symbol));
        if (currencyReward != "") {
            if (orientationSymbol) {
                tvCurrencyRewardLeft.setVisibility(View.VISIBLE);
                tvCurrencyRewardRight.setVisibility(View.GONE);
            } else {
                tvCurrencyRewardLeft.setVisibility(View.GONE);
                tvCurrencyRewardRight.setVisibility(View.VISIBLE);
            }
            tvCurrencyRewardLeft.setText(currencyReward);
            tvCurrencyRewardRight.setText(currencyReward + " ");
        }

        String rewardText = Helper.fetchStringSharePref(getApplicationContext(), "reward_text");
        if (rewardText != "") {
            tvRewardFor.setText(rewardText);
        }

        if (!Helper.containKeySharePref(getApplicationContext(), getString(R.string.Pref_screen_saver))) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 3);
            File file = new File(Environment.getExternalStorageDirectory(), getResources().getString(R.string.folder_name));
            Helper.storeStringSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path), file.getAbsolutePath());
            etScFolder.setText(file.getAbsolutePath());
            rbScFolder.setChecked(true);
            etScFolder.setVisibility(View.VISIBLE);
        } else {
            int screenSaver = Helper.fetchIntSharePref(getApplication(), getString(R.string.Pref_screen_saver), 3);
            // This int represents :
            // 1_ Default bitshares logo
            // 2_ Gravatar
            // 3_ Selected Folder Location images
            // 4_ sapos showcase images

            if (screenSaver == 1) {
                rbScDefault.setChecked(true);

            } else if (screenSaver == 2) {
                rbScMyGravatar.setChecked(true);
            } else if (screenSaver == 3) {
                String folderPath = Helper.fetchStringSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path));
                etScFolder.setVisibility(View.VISIBLE);
                etScFolder.setText(folderPath);
                rbScFolder.setChecked(true);
            } else {
                rbScFolder.setChecked(true);
            }
        }

        //Screen Saver Time
        int screenSaverTime = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver_time), 0);
        if (screenSaverTime == 0 || screenSaverTime == 1) {
            rbThreeMint.setChecked(true);
        }
        if (screenSaverTime == 2) {
            rbFiveMint.setChecked(true);
        }
        if (screenSaverTime == 3) {
            rbSevenMint.setChecked(true);
        }

        getCapfeedData();
        getAltcoinsFromServer(this);
        populateDropDowns();

        if (!Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_ltm_amount))) {
            Helper.storeStringSharePref(getApplicationContext(), getString(R.string.pref_ltm_amount), "17611.7");
        }
        generateCCAssets();

        final RadioGroup smartCoinsRadioGroupOne = (RadioGroup) findViewById(R.id.smartCoinsRadioGroupOne);

        smartCoinsRadioGroupOne.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                smartCoinsListRadioSelectionChange(group, checkedId, "smartCoinsRadioGroupOne");
            }
        });

        RadioGroup smartCoinsRadioGroupTwo = (RadioGroup) findViewById(R.id.smartCoinsRadioGroupTwo);

        smartCoinsRadioGroupTwo.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                smartCoinsListRadioSelectionChange(group, checkedId, "smartCoinsRadioGroupTwo");
            }
        });

        /* Storing the new ratio part as soon as it is typed */
        etRewardToGive.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                double rewardAmount = 0;
                try{
                    rewardAmount = Double.parseDouble(s.toString());
                }catch(NumberFormatException e){
                    Log.w(TAG,"NumberFormatException. Msg: "+e.getMessage());
                }
                tinyDB.putDouble(Constants.KEY_LOYALTY_REWARD_AMOUNT, rewardAmount);
                checkRewardRatioStatus();
            }
        });

        /* Storing the new ratio part as soon as it is typed */
        etRewardForSpending.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                double smartCoinAmount = 0;
                try{
                    smartCoinAmount = Double.parseDouble(s.toString());
                }catch(NumberFormatException e){
                    Log.w(TAG,"NumberFormatException. Msg: "+e.getMessage());
                }
                tinyDB.putDouble(Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, smartCoinAmount);
                checkRewardRatioStatus();
            }
        });
    }

    /**
     * TextWatcher used to react to know whether or not we should be displaying
     * the reward ratio section.
     *
     * If the reward token has not been selected, there is no point in making it visible.
     */
    private TextWatcher mRewardTokenSelectionWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {}

        @Override
        public void afterTextChanged(Editable s) {
            Asset asset = database.getAssetBySymbol(s.toString());
            boolean assetExist = asset != null;
            if(s.length() == 0){
                rewardRatioContainer.setVisibility(View.GONE);
                tinyDB.putString(Constants.KEY_REWARD_ASSET_ID, "");
            }else if(assetExist){
                rewardRatioContainer.setVisibility(View.VISIBLE);
                tvRewardFor.setText(asset.getSymbol());
            }
        }
    };

    /**
     * Checks the reward ratio status. Should be called only if a selection other than 'None'
     * and will set the status as 'NOT_SPECIFIED' if any of the parts of the reward ratio
     * are not specified.
     */
    private void checkRewardRatioStatus(){
        if(mRewardRatioStatus != RewardRatioStatus.NOT_NEEDED){
            if(tinyDB.getDouble(Constants.KEY_LOYALTY_REWARD_AMOUNT, 0.0) > 0 &&
                    tinyDB.getDouble(Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0.0) > 0){
                getSupportActionBar().setDisplayHomeAsUpEnabled(true);

                mRewardRatioStatus = RewardRatioStatus.SPECIFIED;
            }else{
                getSupportActionBar().setDisplayHomeAsUpEnabled(false);

                mRewardRatioStatus = RewardRatioStatus.NOT_SPECIFIED;
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(accountDetails.size() > 0){
            importNewAccount.setVisibility(View.GONE);
            registerNewAccount.setVisibility(View.GONE);

            tinyDB = new TinyDB(this);
            ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
            for (int i = 0; i < accountDetails.size(); i++) {
                if (accountDetails.get(i).isSelected) {
                    mCurrentAccount = new UserAccount(accountDetails.get(i).account_id);
                    break;
                }
            }
            ArrayList<Asset> assetList = new ArrayList<>();
            getBalancesThread = new WebsocketWorkerThread(new GetAccountBalances(mCurrentAccount, assetList, mAccountBalancesListener));
            getBalancesThread.start();
        }
    }

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

    @OnItemSelected({R.id.spCountry, R.id.spCurrency, R.id.spLanguage})
    void onItemSelected(Spinner spinner, int position){
        if(spinner.getId() == R.id.spCountry){
            if(mCountrySpinnerEnabled){
                ArrayList<Country> countries = Helper.getCountriesArray();
                Helper.storeStringSharePref(this, Constants.KEY_COUNTRY, countries.get(position).getCountryCode());
            }
            mCountrySpinnerEnabled = true;
        }else if(spinner.getId() == R.id.spCurrency){
            String[] currencies = getResources().getStringArray(R.array.currencies);
            Helper.storeStringSharePref(this, Constants.KEY_CURRENCY, currencies[position]);
        }else if(spinner.getId() == R.id.spLanguage){
            LangCode langSelection = (LangCode) spinner.getAdapter().getItem(position);
            Helper.storeStringSharePref(this, Constants.KEY_LANGUAGE, langSelection.code);
        }
    }

    private void changeGravatarPressed() {
        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.buttonanimate);
        ((ImageView) findViewById(R.id.ivChangeGravatar)).startAnimation(animation1);
        final Activity settingsActivity = this;

        animation1.setAnimationListener(new android.view.animation.Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // here you can play your sound
                showDialogForGravatar(settingsActivity);
            }

        });
    }

    @OnClick(R.id.tvChangeGravatar)
    public void changeGravatar() {

        changeGravatarPressed();
    }

    @OnClick(R.id.ivChangeGravatar)
    public void changeIvGravatar() {
        changeGravatar();
    }

    private void deleteGravatarPressed() {
        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.buttonanimate);
        ((ImageView) findViewById(R.id.ivDeleteGravatar)).startAnimation(animation1);

        animation1.setAnimationListener(new android.view.animation.Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Helper.deleteImage(getApplicationContext());
                MerchantEmail.saveMerchantEmail(getApplicationContext(),"");
                ivGravatar.setImageDrawable(getResources().getDrawable(R.drawable.icon_merchant));
                Gravatar.reset();

                if (rbScMyGravatar.isChecked() == true) {
                    rbScDefault.setChecked(true);
                    Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 1);
                }
            }

        });
    }

    @OnClick(R.id.tvDeleteGravatar)
    public void deleteGravatar() {
        deleteGravatarPressed();
    }

    @OnClick(R.id.ivDeleteGravatar)
    public void deleteIvGravatar() {
        deleteGravatarPressed();
    }

    FileChooserDialog fileDialog;

    private void initFileChooserDialog() {
        if (this.fileDialog == null) {
            this.fileDialog = new FileChooserDialog(this);
            this.fileDialog.addListener(SettingActivity.this.onFileSelectedListener);
            this.fileDialog.setFolderMode(true);
            this.fileDialog.setCanCreateFiles(false);
            this.fileDialog.setShowCancelButton(true);
            this.fileDialog.setShowOnlySelectable(false);
            this.fileDialog.setFilter(".*jpg|.*png|.*JPG|.*PNG");


            // Activate the confirmation dialogs.
            this.fileDialog.setShowConfirmation(true, true);

            // Define the labels.
            FileChooserLabels labels = new FileChooserLabels();
            labels.createFileDialogAcceptButton = getApplicationContext().getString(R.string.accept);
            labels.createFileDialogCancelButton = getApplicationContext().getString(R.string.cancel);
            labels.labelSelectButton = getApplicationContext().getString(R.string.select);
            labels.messageConfirmSelection = getApplicationContext().getString(R.string.are_you_sure);
            labels.labelConfirmYesButton = getApplicationContext().getString(R.string.yes);
            labels.labelConfirmNoButton = getApplicationContext().getString(R.string.no);
            labels.labelCancelButton = getApplicationContext().getString(R.string.cancel);
            this.fileDialog.setLabels(labels);

            this.fileDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    if(dialog != null)
                        dialog.dismiss();

                    String folderPath = Helper.fetchStringSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path));

                    if (folderPath != null && !folderPath.isEmpty()) {
                        etScFolder.setVisibility(View.VISIBLE);
                        etScFolder.setText(folderPath);
                    } else {
                        rbScDefault.setChecked(true);
                        Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 1);
                        etScFolder.setVisibility(View.GONE);
                        etScFolder.setText("");
                    }
                }
            });
        }
    }

    private void folderScreenSaver() {
        // Show the dialog.
        this.fileDialog.show();
    }

    private FileChooserDialog.OnFileSelectedListener onFileSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.hide();
            Helper.storeStringSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path), file.getAbsolutePath());
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 3);
            etScFolder.setVisibility(View.VISIBLE);
            etScFolder.setText(file.getAbsolutePath());
        }

        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
        }
    };

    @OnCheckedChanged(R.id.rbScFolder)
    void onCheckedFolder(boolean checked) {
        if (checked) {
        } else {
            etScFolder.setVisibility(View.GONE);
        }
    }

    @OnClick(R.id.rbScFolder)
    void onCLickedFolder() {
        PermissionManager Manager = new PermissionManager();

        if (!Manager.isStoragePermissionGranted(this)) {
            Manager.verifyStoragePermissions(this);
            return;
        }

        String folderPath = Helper.fetchStringSharePref(getApplicationContext(), getString(R.string.pref_screen_saver_folder_path));

        if ((etScFolder.getVisibility() == View.GONE) && folderPath != null && !folderPath.isEmpty()) {
            etScFolder.setVisibility(View.VISIBLE);
            etScFolder.setText(folderPath);
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 3);
            Toast.makeText(getApplicationContext(), getString(R.string.click_folder_to_change_its_location) + "!", Toast.LENGTH_LONG).show();
        } else {
            folderScreenSaver();
        }
    }

    @OnCheckedChanged(R.id.rbScDefault)
    void onCheckedDefault(boolean checked) {
        if (checked) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 1);
        }
    }

    @OnCheckedChanged(R.id.rbScMyGravatar)
    void onCheckedGravatar(boolean checked) {
        if (checked) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 2);
        }
    }

    @OnCheckedChanged(R.id.rbThreeMint)
    void onCheckedDefaultThreeMinute(boolean checked) {
        if (checked) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver_time), 1);
        }
    }

    @OnCheckedChanged(R.id.rbFiveMint)
    void onCheckedFiveMinute(boolean checked) {
        if (checked) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver_time), 2);
        }
    }

    @OnCheckedChanged(R.id.rbSevenMint)
    void onCheckedSevenMinut(boolean checked) {
        if (checked) {
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver_time), 3);
        }
    }

    private void closeSettingsActivity() {
        if (this.fileDialog != null) {
            this.fileDialog.dismiss();
            this.fileDialog.cancel();
            this.fileDialog = null;
        }
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void saveButtonPressed() {
        Log.d(TAG, "saveButtonPressed");
        boolean isValid = validateSettings();
        if (isValid) {
            Boolean checkCoins = true;

            getAltAndCoinIndexes();

            if ((arrayAltCoins != null) && (arrayAltCoins.arrayList.size() > 0) && (arrayAltCoins.altcoins.length > 0)) {
                Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin), arrayAltCoins);
            } else {
                Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin), null);
            }

            // TODO: Change this later, I don't think we neet it at all
            Boolean orientationSymbol = true;
            Helper.storeBoolianSharePref(getApplicationContext(), getString(R.string.orientation_symbol), orientationSymbol);

            if (!Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin))) {
                Toast.makeText(getApplicationContext(), R.string.txt_please_select_smartcoin, Toast.LENGTH_SHORT).show();
                checkCoins = false;
                Log.w(TAG,"Missing smart coin");
            }

            if (!Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins))) {
                retryGetSortedMarketCap = 5;
                retryGetMarketCap(this);
//                checkCoins = false;
                Log.w(TAG,"Missing sorted altcoin list");
            }
            if (!Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin))) {
                retryGetSortedAltcoins = 5;
                retryGetAltCoins(this);
                checkCoins = false;
                Log.w(TAG,"Missing bitcoin_altcoin pref");
            }
            if (arrayAltCoins.arrayList.size() > 7) {
                checkCoins = false;
                Toast.makeText(getApplicationContext(), R.string.altcoins_should_not_more_than, Toast.LENGTH_SHORT).show();
                Log.w(TAG,"More than 7 selected altcoins");
            }
            if (MerchantEmail.getMerchantEmail(getApplicationContext()).isEmpty() && rbScMyGravatar.isChecked()) {
                checkCoins = false;
                Toast.makeText(getApplicationContext(), R.string.please_create_gravatar, Toast.LENGTH_SHORT).show();
                Log.w(TAG,"No gravatar");
            }
            if (checkCoins) {
                ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
                Account account = new Account();
                for (int i = 0; i < accountDetails.size(); i++) {
                    if (accountDetails.get(i).isSelected) {
                        account.id = accountDetails.get(i).account_id;
                        account.name = accountDetails.get(i).account_name;
                        break;
                    }
                }
                Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.key_account), account);
                //Helper.storeStringSharePref(getApplicationContext(), "country", String.valueOf(spCountry.getSelectedItemPosition()));
                Helper.storeStringSharePref(getApplicationContext(), "settings_saved", "1");
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                closeSettingsActivity();
            }else{
                Log.w(TAG,"checkCoins is false");
            }
        } else {
            Log.e(TAG,"Not valid settings");
        }
    }

    @OnClick(R.id.btnSave)
    public void done() {
        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
        ((Button) findViewById(R.id.btnSave)).startAnimation(animation1);

        animation1.setAnimationListener(new android.view.animation.Animation.AnimationListener() {

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // here you can play your sound
                saveButtonPressed();
            }
        });
    }

    // combined listener event for selected smartcoin columns

    private void smartCoinsListRadioSelectionChange(RadioGroup group, int checkedId, String groupName) {
        Log.d(TAG,"smartCoinsListRadioSelectionChange");
        if (checkedId != -1) {
            int id = checkedId / 2;
            RadioButton btn = (RadioButton) group.getChildAt(id);

            if (btn != null) {
                boolean checked = btn.isChecked();

                if (checked) {
                    Animation anim = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blink);
                    btn.startAnimation(anim);

                    List<Smartcoin> smartCoins = ccAssets.smartcoins;
                    Smartcoin smartCoin = smartCoins.get(checkedId);
                    Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin), smartCoin);
                    Helper.storeIntSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin_index), checkedId);

                    /**
                     * If a reward asset is selected, then reset the reward ratio and set it status
                     * to 'unspecified'. This is important so that we don't allow the user to mess
                     * up and end up specifying an old ratio to a new UIA-Smartcoin pair.
                     */
                    if(!tinyDB.getString(Constants.KEY_REWARD_ASSET_ID).equals("")){
                        mRewardRatioStatus = RewardRatioStatus.NOT_NEEDED;

                        tinyDB.putDouble(Constants.KEY_LOYALTY_REWARD_AMOUNT, 0);
                        tinyDB.putDouble(Constants.KEY_LOYALTY_SMARTCOIN_AMOUNT, 0);
                        etRewardForSpending.setText("");
                        etRewardToGive.setText("");

                        mRewardRatioStatus = RewardRatioStatus.NOT_SPECIFIED;
                    }

                    try {
                        Currency currency = Currency.getInstance(smartCoin.symbol);
                        tvCurrencyRewardLeft.setText(currency.getSymbol());
                        tvCurrencyRewardRight.setText(currency.getSymbol());
                    } catch (Exception e) {
                        tvCurrencyRewardLeft.setText(smartCoin.symbol);
                        tvCurrencyRewardRight.setText(smartCoin.symbol);
                    }

                    Helper.storeStringSharePref(getApplicationContext(), "currency_reward", tvCurrencyRewardLeft.getText().toString());
                    if (groupName == "smartCoinsRadioGroupOne") {
                        RadioGroup smartCoinsRadioGroupTwo = (RadioGroup) findViewById(R.id.smartCoinsRadioGroupTwo);
                        smartCoinsRadioGroupTwo.clearCheck();
                    } else if (groupName == "smartCoinsRadioGroupTwo") {
                        RadioGroup smartCoinsRadioGroupOne = (RadioGroup) findViewById(R.id.smartCoinsRadioGroupOne);
                        smartCoinsRadioGroupOne.clearCheck();
                    }
                }
            }
        }
    }

    @Override
    public void onBackPressed() {
        if(validateSettings()){
            super.onBackPressed();
        }else{
            Log.w(TAG,"incomplete");
        }
    }

    private boolean validateSettings() {
        if (accountDetails.size() == 0) {
            Toast.makeText(getApplicationContext(), R.string.kindly_create_account, Toast.LENGTH_SHORT).show();
            return false;
        }else if(mRewardRatioStatus == RewardRatioStatus.NOT_SPECIFIED){
            Toast.makeText(this, getResources().getString(R.string.warning_missing_reward_ratio), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private Boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if (email.matches(emailPattern)) {
            return true;
        } else {
            return false;
        }
    }

    private void showDialogForInvalidEmailGravatar(final Activity activity) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        final EditText edittext = new EditText(this);
        edittext.setMaxLines(1);
        edittext.setSingleLine(true);
        edittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !Character.toString(source.charAt(i)).equals("_") && !Character.toString(source.charAt(i)).equals("-") && !Character.toString(source.charAt(i)).equals(".") && !Character.toString(source.charAt(i)).equals("@")) {
                        return "";
                    }
                }
                return null;
            }
        };
        edittext.setFilters(new InputFilter[]{filter});
        edittext.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(s.toString().toLowerCase())) {
                    edittext.setText(s.toString().toLowerCase());
                    edittext.setSelection(edittext.getText().length());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        alert.setMessage(R.string.txt_invalid_email);
        alert.setTitle(R.string.gravatar_title);
        alert.setView(edittext);

        alert.setPositiveButton(getString(R.string.go), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (isValidEmail(edittext.getText().toString())) {
                    MerchantEmail.saveMerchantEmail(getApplicationContext(),edittext.getText().toString());
                    fetchGravatarInfo(edittext.getText().toString());
                } else {
                    //alert.setMessage(R.string.txt_invalid_email);
                    showDialogForInvalidEmailGravatar(activity);
                }

            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });
        alert.show();
    }

    private void showDialogForGravatar(final Activity activity) {
        final AlertDialog.Builder alert = new AlertDialog.Builder(activity);
        final EditText edittext = new EditText(this);
        edittext.setMaxLines(1);
        edittext.setSingleLine(true);
        edittext.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i)) && !Character.toString(source.charAt(i)).equals("_") && !Character.toString(source.charAt(i)).equals("-") && !Character.toString(source.charAt(i)).equals(".") && !Character.toString(source.charAt(i)).equals("@")) {
                        return "";
                    }
                }
                return null;
            }
        };
        edittext.setFilters(new InputFilter[]{filter});
        edittext.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (!s.toString().equals(s.toString().toLowerCase())) {
                    edittext.setText(s.toString().toLowerCase());
                    edittext.setSelection(edittext.getText().length());
                }
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        alert.setMessage(R.string.txt_type_email);
        alert.setTitle(R.string.gravatar_title);
        alert.setView(edittext);

        alert.setPositiveButton(getString(R.string.go), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (isValidEmail(edittext.getText().toString())) {

                    MerchantEmail.saveMerchantEmail(getApplicationContext(),edittext.getText().toString());
                    fetchGravatarInfo(edittext.getText().toString());
                } else {
                    showDialogForInvalidEmailGravatar(activity);
                }

            }
        });

        alert.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                dialog.dismiss();
            }
        });

        alert.show();
    }

    /**
     * Populates the smartcoin and UIA sections.
     * @param ccAssets
     */
    private void populateSmartcoins(CCAssets ccAssets) {
        Helper.storeObjectSharePref(getApplicationContext(), "smartcoins", ccAssets.smartcoins);

        int index = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin_index), Helper.DEFAULT_UNSELECTED_VALUE);
        int indexUia = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.pref_uia_coin_index), Helper.DEFAULT_UNSELECTED_VALUE);

        ((ViewGroup) findViewById(R.id.smartCoinsRadioGroupOne)).removeAllViews();
        ((ViewGroup) findViewById(R.id.smartCoinsRadioGroupTwo)).removeAllViews();

        for (int i = 0; i < ccAssets.smartcoins.size(); i++) {
            Smartcoin ob = ccAssets.smartcoins.get(i);
            RadioButton radioButton = new RadioButton(this);
            if (i == index) {
                radioButton.setChecked(true);
            }
            radioButton.setId(i);
            String label = ob.symbol;
            if (!ob.symbol.equals("BTS"))
                label = "bit" + ob.symbol;
            if(ob.description != null && !ob.description.equals("") && ob.description.split(" ").length <= MAX_DESCRIPTION_LENGTH)
                label = label + " (" + ob.description + ")";
            radioButton.setText(label);

            if (i % 2 == 0) {
                ((ViewGroup) findViewById(R.id.smartCoinsRadioGroupOne)).addView(radioButton);
            } else {
                ((ViewGroup) findViewById(R.id.smartCoinsRadioGroupTwo)).addView(radioButton);
            }
        }
    }

    int retryGetSortedMarketCap = 5;

    private void retryGetMarketCap(final Activity activity) {
        if (retryGetSortedMarketCap > 0) {
            getCapfeedData();
            retryGetSortedMarketCap--;
        } else {
            Toast.makeText(activity, activity.getString(R.string.txt_unable_to_get_market_cap), Toast.LENGTH_SHORT).show();
        }
    }

    int retryGetSortedAltcoins = 5;

    private void retryGetAltCoins(final Activity activity) {
        if (retryGetSortedAltcoins > 0) {
            getAltcoinsFromServer(activity);
            retryGetSortedAltcoins--;
        } else {
            Toast.makeText(activity, activity.getString(R.string.txt_unable_to_get_altcoins), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Method used to fetch the market cap data from the capfeed service.
     */
    public void getCapfeedData() {
        Log.d(TAG,"getCapfeedData");
        ServiceGenerator sg = new ServiceGenerator(Constants.CAPFEED_URL);
        IWebService service = sg.getService(IWebService.class);
        final Call<CapFeedResponse> postingService = service.getCapfeedData();
        postingService.enqueue(new Callback<CapFeedResponse>() {
            @Override
            public void onResponse(Call<CapFeedResponse> call, Response<CapFeedResponse> response) {
                if (response.isSuccessful()) {
                    CapFeedResponse capFeedResponse = response.body();
                    ArrayList<SortedAltcoin> sortedList = new ArrayList<>();
                    storeMarketCap(capFeedResponse.result);
                } else if (Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins))) {
                    // Do Nothing
                    Log.d(TAG,"Could not fetch market cap data, using stored one");
                } else {
                    getCoinMarketCapData();
                }
            }

            @Override
            public void onFailure(Call<CapFeedResponse> call, Throwable t) {
                if (Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins))) {
                    Log.d(TAG,"Could not fetch market cap data, using stored one");
                } else {
                    getCoinMarketCapData();
                }
            }
        });
    }

    /**
     * Method used to fetch the market cap data from the coinmarketcap service.
     */
    private void getCoinMarketCapData(){
        Log.d(TAG,"getCoinMarketCapData");
        ServiceGenerator sg = new ServiceGenerator(Constants.COINMARKETCAP_URL);
        IWebService service = sg.getService(IWebService.class);
        Call<List<CoinMarketCapItemResponse>> call = service.getCoinMarketcapData(50);
        call.enqueue(new Callback<List<CoinMarketCapItemResponse>>() {

            @Override
            public void onResponse(Call<List<CoinMarketCapItemResponse>> call, Response<List<CoinMarketCapItemResponse>> response) {
                if(response.isSuccessful()){
                    List<CoinMarketCapItemResponse> coins = response.body();
                    storeMarketCap(coins.toArray(new CoinMarketCapItemResponse[coins.size()]));
                }else{
                    Toast.makeText(getApplicationContext(), "Coin market cap data failed too", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<CoinMarketCapItemResponse>> call, Throwable t) {
                Toast.makeText(getApplicationContext(), "Coin market cap data failed too", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Method used to store the market cap data obtained from any of the supported third-party
     * services into the shared preferences storage.
     *
     * TODO: Move this to a proper database.
     * @param coins: Array of objects that represetn a cryptocurrency in each one of the
     *             supported services.
     */
    private void storeMarketCap(Object[] coins){
        Log.d(TAG,"storeMarketCap");
        if(coins == null || coins.length == 0){
            return;
        }
        ArrayList<SortedAltcoin> sortedList = new ArrayList<>();
        for(Object item : coins){
            SortedAltcoin sortedAltcoin = new SortedAltcoin();
            if(item instanceof CoinMarketCapItemResponse){
                sortedAltcoin.name = ((CoinMarketCapItemResponse) item).name;
                sortedAltcoin.symbol = ((CoinMarketCapItemResponse) item).symbol;
            }else if(item instanceof CapfeedCryptoCurrency){
                sortedAltcoin.name = ((CapfeedCryptoCurrency) item).name;
                sortedAltcoin.symbol = ((CapfeedCryptoCurrency) item).symbol;
            }
            sortedList.add(sortedAltcoin);
        }
        Helper.storeObjectSharePref(getApplicationContext(), getString(R.string.pref_sorted_altcoins), sortedList);
    }


    int altcoinsSelectedCount = 0;
    Boolean altcoinsListPopulated = false;

    private void populateAltcoinsList(final Activity activity, Altcoin[] altcoins) {
        if (!altcoinsListPopulated) {
            arrayAltCoins.altcoins = altcoins;
            String altAndCoin = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin));
            ArrayAltCoins sharePrefAltCoin = gson.fromJson(altAndCoin, ArrayAltCoins.class);
            //For Sorting
            for (int i = 0; i < arrayAltCoins.altcoins.length; i++) {
                Altcoin ob = arrayAltCoins.altcoins[i];
                altcoinsIndexMap.put(ob.name, i);
                altcoinsIndexMap.put(i, ob.name);
                altcoinsNameArr.add(i, ob.name);
            }
            Collections.sort(altcoinsNameArr, String.CASE_INSENSITIVE_ORDER);

            altcoinsSelectedCount = 0;
            for (int i = 0; i < altcoinsNameArr.size(); i++) {

                if(((String)altcoinsNameArr.get(i)).contains("BitShares 1.0")){
                    continue;
                }

                if(((String)altcoinsNameArr.get(i)).startsWith("Trade")){
                    continue;
                }

                if(((String)altcoinsNameArr.get(i)).startsWith("OPEN")){
                    continue;
                }

                boolean isBlacklisted = false;
                for(String blacklisted : cryptocurrenciesBlacklist){
                    if(((String) altcoinsNameArr.get(i)).contains(blacklisted)){
                        isBlacklisted = true;
                        break;
                    }
                }
                if(isBlacklisted) continue;

                CheckBox checkbox = new CheckBox(activity);

                int j = (int) altcoinsIndexMap.get(altcoinsNameArr.get(i));
                if (sharePrefAltCoin != null) {
                    if (sharePrefAltCoin.arrayList.size() > 0) {
                        if (sharePrefAltCoin.arrayList.contains(altcoinsNameArr.get(i).toString().toLowerCase())) {
                            checkbox.setChecked(true);
                            altcoinsSelectedCount++;
                        }
                    }
                }
                checkbox.setId(i);
                checkbox.setDuplicateParentStateEnabled(true);
                checkbox.setClickable(false);
                checkbox.setTag("CheckBox");

                RelativeLayout.LayoutParams checkbox_relativeParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                checkbox_relativeParams.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
                checkbox_relativeParams.addRule(RelativeLayout.ALIGN_PARENT_TOP);
                final RelativeLayout ll = new RelativeLayout(activity);
                ll.setLayoutParams(new ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));
                ll.setClickable(true);
                View.OnClickListener llListner = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        View view = ll.findViewWithTag("CheckBox");
                        CheckBox cBox = (CheckBox) view;

                        if (cBox.isChecked()) {
                            cBox.setChecked(false);
                            altcoinsSelectedCount--;

                        } else {
                            cBox.setChecked(true);
                            altcoinsSelectedCount++;

                            if (altcoinsSelectedCount > 7) {
                                cBox.setChecked(false);
                                altcoinsSelectedCount--;
                                Toast.makeText(activity, activity.getString(R.string.selection_altcoins_beyond_allowed), Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                };
                ll.setOnClickListener(llListner);
                ImageView iv = new ImageView(activity);
                int resIdentifier = getResources().getIdentifier(Helper.getCoinImagePath(arrayAltCoins.altcoins[j].coinType), "drawable", getPackageName());
                String imgUri = "drawable://" + getResources().getIdentifier("blank", "drawable", getPackageName());
                if (resIdentifier != 0) {
                    imgUri = "drawable://" + getResources().getIdentifier(Helper.getCoinImagePath(arrayAltCoins.altcoins[j].coinType), "drawable", getPackageName());
                }
                myImageLoader.displayImage(imgUri, iv);


                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(60, 60);
                layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
                iv.setLayoutParams(layoutParams);
                TextView tv = new TextView(activity);
                RelativeLayout.LayoutParams tvParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                tvParams.addRule(RelativeLayout.CENTER_VERTICAL);
                tv.setLayoutParams(tvParams);
                String temp = altcoinsNameArr.get(i).toString();
                if (!temp.isEmpty()) {
                    if (temp.substring(0, 3).equalsIgnoreCase("Bit") && !temp.equalsIgnoreCase("bitcoin")) {
                        temp = temp.substring(0, 1).toLowerCase() + temp.substring(1, temp.length());
                    }
                }
                tv.setText(temp);
                tv.setPadding(100, 0, 0, 0);
                ll.addView(iv);
                ll.addView(tv);
                ll.addView(checkbox, checkbox_relativeParams);
                llBitCoinAndAltCoins.addView(ll);
            }

            altcoinsListPopulated = true;
        }
    }

    /**
     * Retrieves a list of altcoins from the Blocktrades service.
     * @param activity
     */
    public void getAltcoinsFromServer(final Activity activity) {
        Log.d(TAG,"getAltcoinsFromServer");
        ServiceGenerator sg = new ServiceGenerator(getString(R.string.blocktrade_server_url));
        IWebService service = sg.getService(IWebService.class);
        final Call<Altcoin[]> postingService = service.getAltcoins();
        postingService.enqueue(new Callback<Altcoin[]>() {
            @Override
            public void onResponse(Call<Altcoin[]> call, Response<Altcoin[]> response) {
                if (response.isSuccessful()) {
                    Altcoin[] altcoins = response.body();
                    populateAltcoinsList(activity, altcoins);
                } else if (Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin))) {
                    String altcoinsList = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin));
                    Gson gson = new Gson();
                    arrayAltCoins = gson.fromJson(altcoinsList, ArrayAltCoins.class);
                    if(arrayAltCoins != null){
                        populateAltcoinsList(activity, arrayAltCoins.altcoins);
                    }else{
                        Toast.makeText(getApplicationContext(), "It was not possible to retrieve the list of supported altcoins from the blocktrades API", Toast.LENGTH_LONG).show();
                    }
                } else {
                    retryGetAltCoins(activity);
                }
            }

            @Override
            public void onFailure(Call<Altcoin[]> call, Throwable t) {
                if (Helper.containKeySharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin))) {
                    String altcoinsList = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_bitcoin_altcoin));
                    Gson gson = new Gson();
                    arrayAltCoins = gson.fromJson(altcoinsList, ArrayAltCoins.class);
                    populateAltcoinsList(activity, arrayAltCoins.altcoins);
                } else {
                    retryGetAltCoins(activity);
                }
            }
        });
    }

    public void getGravatarImageFromServer(String imageUri) {

        myImageLoader.loadImage(imageUri, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                Helper.saveToInternalStorage(getApplicationContext(), loadedImage);
                Helper.storeIntSharePref(getApplicationContext(), getString(R.string.Pref_screen_saver), 3);
                ivGravatar.setImageBitmap(loadedImage);
            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {
                String cause = "";
                try {
                    //  Do database operation
                    cause = failReason.getCause().toString();
                } catch (Exception e) {
                    //throw new IOException(e.toString());
                    cause = getResources().getString(R.string.unable_to_get_gravatar);
                }
                Toast.makeText(getApplicationContext(), cause, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void populateDropDowns() {
        initializeCountryDropDownMenu();
        initializeCurrencySpinner();

        ArrayList<LangCode> langArray = new ArrayList<>();
        ArrayList<String> getLangCode = null;
        getLangCode = Helper.getLanguages();

        for (int i = 0; i < getLangCode.size(); i++) {
            if (getLangCode.get(i).equalsIgnoreCase("zh-rTW")) {
                LangCode langCode = new LangCode();
                langCode.code = "zh-rTW";
                langCode.lang = "Chinese" + "; " + "zh-rTW " + "(繁體中文)";
                langArray.add(langCode);
            } else {
                LangCode langCode = new LangCode();
                Locale locale = new Locale(getLangCode.get(i));
                langCode.lang = locale.getDisplayName() + "; " + locale.toString() + " (" + locale.getDisplayLanguage(locale) + ")";
                langCode.code = getLangCode.get(i);
                langArray.add(langCode);
            }
        }


        ArrayAdapter<LangCode> languageAdapter = new ArrayAdapter<LangCode>(this, android.R.layout.simple_spinner_item, langArray);
        languageAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spLanguage.setAdapter(languageAdapter);
        String langCode = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_LANGUAGE);

        if (langCode != "") {
            for (int i = 0; i < langArray.size(); i++) {
                LangCode lc = langArray.get(i);
                if (lc.code.equalsIgnoreCase(langCode)) {
                    spLanguage.setSelection(i);
                }
            }
        } else {
            for (int i = 0; i < langArray.size(); i++) {
                LangCode lc = langArray.get(i);
                if (lc.code.equalsIgnoreCase(Locale.getDefault().getLanguage())) {
                    spLanguage.setSelection(i);
                }
            }
//            spLanguage.setSelection(13);
        }


        //Time Zones
        ArrayList<String> arrayTimeZones = new ArrayList<>();

        String[] ids = TimeZone.getAvailableIDs();
        for (String id : ids) {
            arrayTimeZones.add(displayTimeZone(TimeZone.getTimeZone(id)));
        }

        Collections.sort(arrayTimeZones);

        arrayTimeZones.add(0, getString(R.string.select_timezone));
        ArrayAdapter<String> adapterTimezone = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, arrayTimeZones);
        adapterTimezone.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spDateAndTime.setAdapter(adapterTimezone);

        int indexTimezone = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.pref_timezone), 0);
        spDateAndTime.setSelection(indexTimezone);

        //Account

        ArrayList<String> arrayAccountName = new ArrayList<>();

        if (accountDetails.size() > 1) {
            spAccounts.setVisibility(View.VISIBLE);
            tvAccounts.setVisibility(View.GONE);
        } else {
            tvAccounts.setVisibility(View.VISIBLE);
            spAccounts.setVisibility(View.GONE);
        }
        String accountId = "";
        String accountName = "";
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.size() == 1) {
                accountDetailsId = 0;
            } else {
                accountDetailsId = i;
            }
            arrayAccountName.add(accountDetails.get(i).account_name);
            if (accountDetails.get(i).isSelected) {
                tvAccounts.setText(accountDetails.get(i).account_name);
                accountName = accountDetails.get(i).account_name;
                wifKey = accountDetails.get(i).wif_key;
                accountId = accountDetails.get(i).account_id;
                if (accountDetails.get(i).isLifeTime) {
                    ivLifeTime.setVisibility(View.VISIBLE);
                    btnUpgrade.setEnabled(false);
                    btnUpgrade.setBackgroundColor(Color.rgb(211, 211, 211));

                } else {
                    ivLifeTime.setVisibility(View.GONE);
                    btnUpgrade.setEnabled(true);
                    btnUpgrade.setBackground(getResources().getDrawable(R.drawable.button_border));
                }

            }

        }
//        isLifeTime(accountId, "15");

        Collections.sort(arrayAccountName);
        ArrayAdapter<String> adapterAccountName = new ArrayAdapter<>(this, R.layout.mytextview, arrayAccountName);
        adapterAccountName.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spAccounts.setAdapter(adapterAccountName);
        if (accountName.isEmpty()) {
            spAccounts.setSelection(0);
        } else {
            spAccounts.setSelection(arrayAccountName.indexOf(accountName));
        }


    }

    private String displayTimeZone(TimeZone tz) {

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(tz);
        long hours = TimeUnit.MILLISECONDS.toHours(tz.getRawOffset());
        long minutes = TimeUnit.MILLISECONDS.toMinutes(tz.getRawOffset())
                - TimeUnit.HOURS.toMinutes(hours);
        minutes = Math.abs(minutes);
        String result = "";
        if (hours > 0) {

            result = String.format("%s (GMT+%d:%02d)", tz.getID(), hours, minutes);
        } else {
            result = String.format("%s (GMT%d:%02d)", tz.getID(), hours, minutes);
        }

        return result;
    }

    @OnItemSelected(R.id.spDateAndTime)
    void onItemSelectedTimeZone(int position) {
        if (position > 0) {
            String temp[] = spDateAndTime.getSelectedItem().toString().split(" ");
            Helper.storeStringSharePref(getApplicationContext(), getString(R.string.date_time_zone), temp[0]);
            Helper.storeIntSharePref(getApplicationContext(), getString(R.string.pref_timezone), position);
        }
    }

    @OnClick(R.id.register_new_account)
    void setRegisterNewAccount() {
        if(BlockpayApplication.accountCanCreate()) {
            Intent intent = new Intent(this, AccountActivity.class);
            intent.putExtra("activity_name", "setting_screen");
            intent.putExtra("activity_id", 919);
            startActivity(intent);
        }else {
            Toast.makeText(getApplicationContext(), getResources().getString(R.string.account_create_msg) , Toast.LENGTH_LONG).show();
        }
    }

    @OnClick(R.id.btn_blocktrade_account)
    void setBtnBlocktradeAccount() {
        Intent intent = new Intent(this, BlocktradeActivity.class);
        intent.putExtra("activity_name", "setting_screen");
        intent.putExtra("activity_id", 919);
        startActivity(intent);
    }

    @OnClick(R.id.import_new_account)
    void onImportAccountClicked() {
        Intent intent = new Intent(getApplicationContext(), ExistingAccountActivity.class);
        startActivity(intent);
    }

    @OnClick(R.id.upgrade_account)
    void setUpgradeNewAccount() {

        final boolean[] balanceValid = {true};
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.alert_delete_dialog);
        final Button btnDone = (Button) dialog.findViewById(R.id.btnDone);
        final TextView alertMsg = (TextView) dialog.findViewById(R.id.alertMsg);
        alertMsg.setText(getString(R.string.help_message));
        final Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        btnCancel.setBackgroundColor(Color.RED);
        btnCancel.setText(getString(R.string.txt_back));
        btnCancel.setText(getString(R.string.txt_back));
        btnDone.setText(getString(R.string.next));

        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Check Balance
                String ltmAmount = Helper.fetchStringSharePref(getApplicationContext(), getString(R.string.pref_ltm_amount));
                if (btnDone.getText().equals(getString(R.string.next))) {
                    alertMsg.setText(getString(R.string.upgrade_to_ltm) + ltmAmount + getString(R.string.bts_will_be_deducted) + spAccounts.getSelectedItem().toString() + getString(R.string.account));
                    btnDone.setText(getString(R.string.txt_yes));
                    btnCancel.setText(getString(R.string.txt_no));
                } else {
                    dialog.cancel();
                    ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
                    try {
                        for (int i = 0; i < accountDetails.size(); i++) {
                            if (accountDetails.get(i).isSelected) {
                                wifKey = accountDetails.get(i).wif_key;
                                ArrayList<AccountAssets> arrayListAccountAssets = accountDetails.get(i).AccountAssets;
                                for (int j = 0; j < arrayListAccountAssets.size(); j++) {
                                    AccountAssets accountAssets = arrayListAccountAssets.get(j);
                                    if (accountAssets.symbol.equalsIgnoreCase("BTS")) {
                                        Double amount = Double.valueOf(SupportMethods.ConvertValueintoPrecision(accountAssets.precision, accountAssets.ammount));
                                        if (amount < Double.parseDouble(ltmAmount)) {
                                            balanceValid[0] = false;
                                            Toast.makeText(getApplicationContext(), getString(R.string.insufficient_funds), Toast.LENGTH_LONG).show();
                                        }
                                        break;
                                    }
                                }

                            }
                        }
                    } catch (Exception e) {
                    }
                    if (balanceValid[0]) {
                        showDialog("", getString(R.string.upgrading));
//                        getAccountUpgradeInfo(activity, spAccounts.getSelectedItem().toString());
                    }
                }
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        Window window = dialog.getWindow();
        window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dialog.show();
    }

    @OnClick(R.id.remove_account)
    void setRemoveNewAccount() {
        final Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.alert_delete_dialog);
        Button btnDone = (Button) dialog.findViewById(R.id.btnDone);
        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        TextView textView = (TextView) dialog.findViewById(R.id.alertMsg);
        btnCancel.setText(R.string.txt_no);
        btnDone.setText(R.string.txt_yes);
        String message = getString(R.string.sorry_no_account_remove);
        Log.d(TAG, "account details size: "+accountDetails.size());
        if(accountDetails.size() == 0) {
            btnDone.setVisibility(View.GONE);
            btnCancel.setText(getString(R.string.cancel));
        }else{
            message = String.format(getResources().getString(R.string.txt_account_remove_confirmation), spAccounts.getSelectedItem().toString());
        }
        textView.setText(message);
        btnDone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteCurrentlyActiveAccount();
                dialog.cancel();
            }
        });
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        dialog.show();
    }

    @SuppressLint("NewApi")
    @OnItemSelected(R.id.spAccounts)
    void onItemSelectedAccount(int position) {
        if (position >= 0) {
            ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
            for (int i = 0; i < accountDetails.size(); i++) {

                if (spAccounts.getSelectedItem().toString().equals(accountDetails.get(i).account_name)) {
                    accountDetails.get(i).isSelected = true;
                    if (accountDetails.get(i).isLifeTime) {
                        ivLifeTime.setVisibility(View.VISIBLE);
                        btnUpgrade.setEnabled(false);
                        btnUpgrade.setBackgroundColor(Color.rgb(211, 211, 211));

                    } else {
                        ivLifeTime.setVisibility(View.GONE);
                        btnUpgrade.setEnabled(true);
                        btnUpgrade.setBackground(getResources().getDrawable(R.drawable.button_border));

                    }
                } else {
                    accountDetails.get(i).isSelected = false;
                }

            }
            tinyDB.putListObject(getString(R.string.pref_wallet_accounts), accountDetails);
            Helper.storeStringSharePref(getApplicationContext(), getString(R.string.pref_account_name), spAccounts.getSelectedItem().toString());
        }
    }


    /**
     * Method used to delete the currently active account.
     */
    void deleteCurrentlyActiveAccount() {
        if (accountDetails.size() > 0) {
            for (int i = 0; i < accountDetails.size(); i++) {
                if (spAccounts.getSelectedItem().toString().equals(accountDetails.get(i).account_name)) {
                    accountDetails.remove(i);
                    break;
                }
            }

            for (int i = 0; i < accountDetails.size(); i++) {
                if (i == 0) {
                    accountDetails.get(i).isSelected = true;
                } else {
                    accountDetails.get(i).isSelected = false;
                }
            }

            tinyDB.putListObject(getString(R.string.pref_wallet_accounts), accountDetails);

            accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
            importNewAccount.setVisibility(View.VISIBLE);
            registerNewAccount.setVisibility(View.VISIBLE);

            ArrayAdapter<String> adapterAccountName = new ArrayAdapter<>(this, R.layout.mytextview, new ArrayList<String>());
            adapterAccountName.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spAccounts.setAdapter(adapterAccountName);
            tvAccounts.setText("");

            populateDropDowns();
        }
    }

    private void getAltAndCoinIndexes() {
        arrayAltCoins.arrayList.clear();
        int count = llBitCoinAndAltCoins.getChildCount();
        View view;
        for (int i = 0; i < count; i++) {
            view = llBitCoinAndAltCoins.getChildAt(i).findViewWithTag("CheckBox");
            if (view instanceof CheckBox) {
                CheckBox checkBox = (CheckBox) view;
                if (checkBox.isChecked()) {
                    arrayAltCoins.arrayList.add(altcoinsNameArr.get(checkBox.getId()).toString().toLowerCase());
                }
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            closeSettingsActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void saveTwoImageToSD() {
        String folder_main = getResources().getString(R.string.folder_name);
        final File f = new File(Environment.getExternalStorageDirectory(), folder_main);

        if (!f.exists()) {
            f.mkdirs();

            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file2 = new File(f.getAbsolutePath(), "cryptowallpapertwo.png");

                        if (!file2.exists()) {
                            Bitmap bitmap2 = BitmapFactory.decodeResource(getResources(), R.drawable.cryptowallpapertwo);

                            FileOutputStream outStream2 = new FileOutputStream(file2);
                            bitmap2.compress(Bitmap.CompressFormat.PNG, 80, outStream2);
                            outStream2.flush();
                            outStream2.close();
                        }
                    } catch (Exception e) {
                    }
                }
            });
            AsyncTask.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        File file = new File(f.getAbsolutePath(), "cryptowallpaperone.png");

                        if (!file.exists()) {
                            Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.cryptowallpaperone);
                            FileOutputStream outStream = new FileOutputStream(file);
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, outStream);
                            outStream.flush();
                            outStream.close();
                        }

                    } catch (Exception e) {
                    }
                }
            });


        }

    }

    public void getAccountUpgradeInfo(final Activity activity, final String accountName) {

        ServiceGenerator sg = new ServiceGenerator(""/*getString(R.string.account_from_brainkey_url)*/);
        IWebService service = sg.getService(IWebService.class);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("method", "upgrade_account");
        hashMap.put("account", accountName);
        try {
            hashMap.put("wifkey", Crypt.getInstance().decrypt_string(wifKey));
        } catch (Exception e) {
        }

        final Call<AccountUpgrade> postingService = service.getAccountUpgrade(hashMap);
        postingService.enqueue(new Callback<AccountUpgrade>() {
            @Override
            public void onResponse(Call<AccountUpgrade> call, Response<AccountUpgrade> response) {
                if (response.isSuccessful()) {
                    AccountUpgrade accountDetails = response.body();
                    if (accountDetails.status.equals("success")) {
                        updateLifeTimeModel(accountName);
                        hideDialog();
                        Toast.makeText(activity, getString(R.string.upgrade_success), Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        hideDialog();
                        Toast.makeText(activity, getString(R.string.upgrade_failed), Toast.LENGTH_SHORT).show();
                    }

                } else {
                    hideDialog();
                    Toast.makeText(activity, getString(R.string.upgrade_failed), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<AccountUpgrade> call, Throwable t) {
                hideDialog();
                Toast.makeText(activity, activity.getString(R.string.txt_no_internet_connection), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void hideDialog() {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog != null) {
                    if (progressDialog.isShowing()) {
                        progressDialog.cancel();
                    }
                }
            }
        });
    }

    private void showDialog(String title, String msg) {
        if (progressDialog != null) {
            if (!progressDialog.isShowing()) {
                progressDialog.setTitle(title);
                progressDialog.setMessage(msg);
                progressDialog.show();
            }
        }
    }

    private void updateLifeTimeModel(String accountName) {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        try {
            for (int i = 0; i < accountDetails.size(); i++) {
                if (accountDetails.get(i).account_name.equals(accountName)) {
                    accountDetails.get(i).isLifeTime = true;
                    break;
                }
            }
        } catch (Exception e) {
        }

        tinyDB.putListObject(getString(R.string.pref_wallet_accounts), accountDetails);
    }

    public void getLtmPrice(final Activity activity, final String accountName) {

        ServiceGenerator sg = new ServiceGenerator(""/*getString(R.string.account_from_brainkey_url)*/);
        IWebService service = sg.getService(IWebService.class);
        HashMap<String, String> hashMap = new HashMap<>();
        hashMap.put("method", "upgrade_account_fees");
        hashMap.put("account", accountName);
        try {
            hashMap.put("wifkey", Crypt.getInstance().decrypt_string(wifKey));
        } catch (Exception e) {
        }

        final Call<LtmFee> postingService = service.getLtmFee(hashMap);
        postingService.enqueue(new Callback<LtmFee>() {
            @Override
            public void onResponse(Call<LtmFee> call, Response<LtmFee> response) {
                if (response.isSuccessful()) {
                    hideDialog();
                    LtmFee ltmFee = response.body();
                    if (ltmFee.status.equals("success")) {
                        try {
                            JSONObject jsonObject = new JSONObject(ltmFee.transaction);
                            JSONObject jsonObject1 = jsonObject.getJSONArray("operations").getJSONArray(0).getJSONObject(1);
                            JSONObject jsonObject2 = jsonObject1.getJSONObject("fee");
                            String amount = jsonObject2.getString("amount");
                            //String asset_id = jsonObject2.getString("asset_id");
                            String temp = SupportMethods.ConvertValueintoPrecision("5", amount);
                            Helper.storeStringSharePref(activity, getString(R.string.pref_ltm_amount), temp);
                        } catch (Exception e) {
                        }
                    }
                }

            }

            @Override
            public void onFailure(Call<LtmFee> call, Throwable t) {
                hideDialog();
            }
        });
    }

//    void isLifeTime(final String name_id, final String id) {
//        try {
//            final int db_id = Helper.fetchIntSharePref(getApplicationContext(), getString(R.string.sharePref_database), -1);
//            //{"id":4,"method":"call","params":[2,"get_accounts",[["1.2.101520"]]]}
//
//            final Handler handler = new Handler();
//
//            final Runnable updateTask = new Runnable() {
//                @Override
//                public void run() {
//
//                    try {
//                        if (BlockpayApplication.isReady)
//                        {
//                            String getDetails = "{\"id\":" + id + ",\"method\":\"call\",\"params\":[" + db_id + ",\"get_accounts\",[[\"" + name_id + "\"]]]}";
//                            SupportMethods.testing("getLifetime", getDetails, "getDetails");
//                            BlockpayApplication.send(getDetails);
//                        } else {
//                            isLifeTime(name_id, id);
//                        }
//                    } catch (Exception e) {
//                    }
//                }
//            };
//
//            handler.postDelayed(updateTask, 1000);
//        } catch (Exception e) {
//
//        }
//    }

    @Override
    public void getLifetime(String s, int id) {
        SupportMethods.testing("getLifetime", s, "s");

        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        SupportMethods.testing("getAccountID", s, "s");

        String result = SupportMethods.ParseJsonObject(s, "result");
        String nameObject = SupportMethods.ParseObjectFromJsonArray(result, 0);
        String expiration = SupportMethods.ParseJsonObject(nameObject, "membership_expiration_date");
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        try {
            Date date1 = dateFormat.parse(expiration);
            Date date2 = dateFormat.parse("1969-12-31T23:59:59");
            if (date2.getTime() >= date1.getTime()) {
                SupportMethods.testing("getLifetime", "true", "s");
                //accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
                if (accountDetails.size() > accountDetailsId) {
                    accountDetails.get(accountDetailsId).isLifeTime = true;
                    showHideLifeTime(true);
                } else if (accountDetails.size() == 1) {
                    accountDetails.get(0).isLifeTime = true;
                    showHideLifeTime(true);
                }
                tinyDB.putListObject(getString(R.string.pref_wallet_accounts), accountDetails);

            } else {
                SupportMethods.testing("getLifetime", "false", "s");
            }
        } catch (Exception e) {
            SupportMethods.testing("getLifetime", e, "Exception");

        }
    }

    @Override
    public void transactionsLoadComplete(List<TransactionDetails> transactionDetails)
    {

    }

    @Override
    public void transactionsLoadMessageStatus(String message)
    {
    }

    @Override
    public void transactionsLoadFailure(String reason)
    {

    }

    private void showHideLifeTime(final Boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    ivLifeTime.setVisibility(View.VISIBLE);
                    btnUpgrade.setEnabled(false);
                    btnUpgrade.setBackgroundColor(Color.rgb(211, 211, 211));

                } else {
                    ivLifeTime.setVisibility(View.GONE);
                    btnUpgrade.setEnabled(true);
                    btnUpgrade.setBackground(getResources().getDrawable(R.drawable.button_border));
                }
            }
        });

    }

    void designMethod() {
        if (android.os.Build.VERSION.SDK_INT > 21)
            getWindow().setExitTransition(new Explode());
    }

    private void initializeCurrencySpinner(){
        CurrenciesAdapter currenciesAdapter = new CurrenciesAdapter(this);
        spCurrency.setAdapter(currenciesAdapter);

        String currentCurrency = Helper.fetchStringSharePref(this, Constants.KEY_CURRENCY);
        int index = 0;
        for(String currency : getResources().getStringArray(R.array.currencies)){
            if(currency.equals(currentCurrency)){
                break;
            }
            index++;
        }
        if(index < currenciesAdapter.getCount()){
            spCurrency.setSelection(index);
        }
    }

    private void initializeCountryDropDownMenu() {
        final ArrayList<Country> countries = Helper.getCountriesArray();
        CountryAdapter adapter = new CountryAdapter(this, countries);
        spCountry.setAdapter(adapter);

        new Handler().postDelayed(new Runnable() {
            public void run() {
                CountryAdapter countryAdapter = (CountryAdapter) spCountry.getAdapter();
                String countryCode = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_COUNTRY, "");
                int index;
                if(!countryCode.equals("")){
                    index = countryAdapter.getIndexOf(Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_COUNTRY));
                }else{
                    index = countryAdapter.getIndexOf(getResources().getConfiguration().locale.getCountry());
                }
                spCountry.setSelection(index);
                spCountry.invalidate();
            }
        }, 500);
    }



    @Override
    public void updateProfile(Gravatar myGravatar) {

    }

    @Override
    public void updateCompanyLogo(Bitmap logo) {
        Helper.saveToInternalStorage(getApplicationContext(), logo);
        ivGravatar.setImageBitmap(logo);
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





    @OnClick(R.id.backup_ic)
    public void onClickBackupDotBin()
    {
        String _brnKey = getBrainKey();
        String _accountName = getAccountName();
        String _pinCode = getPin();

        BinHelper myBinHelper = new BinHelper(this, this);
        myBinHelper.createBackupBinFile(_brnKey,_accountName,_pinCode);

    }
    @Override
    public void backupComplete(boolean success) {
        Log.d("Backup Complete",success+"");
    }
    private String getPin()
    {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        for (int i = 0; i < accountDetails.size(); i++)
        {
            if (accountDetails.get(i).isSelected)
            {
                return accountDetails.get(i).pinCode;
            }
        }

        return "";
    }

    private String getBrainKey()
    {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        for (int i = 0; i < accountDetails.size(); i++)
        {
            if (accountDetails.get(i).isSelected)
            {
                return accountDetails.get(i).brain_key;
            }
        }

        return "";
    }

    private String getAccountName()
    {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        for (int i = 0; i < accountDetails.size(); i++)
        {
            if (accountDetails.get(i).isSelected)
            {
                return accountDetails.get(i).account_name;
            }
        }

        return "";
    }
    public void onClickBackupBrainkeybtn(View v) {
        showDialogCopyBrainKey();
    }

    public void onClickSecurePinbtn(View v) {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        if(accountDetails.size()>0) {
            Intent intent = new Intent(getApplicationContext(), PinActivity.class);
            startActivity(intent);
        }

    }
    private void showDialogCopyBrainKey() {

        final Dialog dialog = new Dialog(this, R.style.BlockpayDialog);
        dialog.setTitle(getString(R.string.backup_brainkey));
        dialog.setContentView(R.layout.activity_copybrainkey);
        final EditText etBrainKey = (EditText) dialog.findViewById(R.id.etBrainKey);
        try
        {
            String brainKey = getBrainKey();
            if (brainKey.isEmpty())
            {
                Toast.makeText(getApplicationContext(),getResources().getString(R.string.unable_to_load_brainkey),Toast.LENGTH_LONG).show();
                return;
            }
            else
            {
                etBrainKey.setText(brainKey);
            }
        }
        catch (Exception e) {

        }

        Button btnCancel = (Button) dialog.findViewById(R.id.btnCancel);
        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.cancel();
            }
        });
        Button btnCopy = (Button) dialog.findViewById(R.id.btnCopy);
        btnCopy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(SettingActivity.this, R.string.copied_to_clipboard , Toast.LENGTH_SHORT).show();
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("label", etBrainKey.getText().toString());
                clipboard.setPrimaryClip(clip);
                dialog.cancel();
            }
        });
        dialog.setCancelable(false);

        dialog.show();
    }

    @Override
    public void onDatabaseUpdated(int code, Bundle data) {
        generateCCAssets();
    }

    /**
     * Temporal method that will work to bridge our new database-backed assets source
     * with the legacy implementation that expects a CCAssets object instance, containing
     * two separate lists of smartcoins and UIAs.
     */
    private void generateCCAssets(){
        CCAssets ccAssets = new CCAssets();
        ccAssets.uia = new ArrayList<>();
        ccAssets.smartcoins = new ArrayList<>();

        List<Asset> assetList = database.getWitnessFedAssets();

        Gson gson = new Gson();
        for(Asset asset : assetList){
            if(asset.getAssetType() == Asset.AssetType.UIA){
                Uia uia = new Uia();
                uia.asset_id = asset.getObjectId();
                uia.precision = String.format("%d", asset.getPrecision());
                uia.symbol = asset.getSymbol();
                ccAssets.uia.add(uia);
            }else if(asset.getAssetType() == Asset.AssetType.SMART_COIN){
                // Skipping over those smartcoins not supported by the bridge
                if(!database.bridgeSupportsOuput(asset.getSymbol())){
                    continue;
                }
                Smartcoin smartcoin = new Smartcoin();
                smartcoin.asset_id = asset.getObjectId();
                smartcoin.precision = String.format("%d", asset.getPrecision());
                smartcoin.symbol = asset.getSymbol();
                smartcoin.description = asset.getAssetOptions().getDescription();

                try {
                    AssetDescription assetDescription = gson.fromJson(smartcoin.description, AssetDescription.class);
                    if(assetDescription != null)
                        smartcoin.description = assetDescription.short_name;
                    else
                        smartcoin.description = "";
                }catch(JsonSyntaxException e){
                    Log.v(TAG, "Could not deserialize description: "+smartcoin.description);
                }
                ccAssets.smartcoins.add(smartcoin);
            }
        }

        // Adding BTS as a smartcoin option
        // Even though it is not technically one, we want to display it to the user in between all
        // other smartcoin choices.
        Smartcoin smartcoin = new Smartcoin();
        smartcoin.symbol = "BTS";
        smartcoin.precision = "5";
        smartcoin.asset_id = "1.3.0";
        smartcoin.description = "Bitshares";
        ccAssets.smartcoins.add(smartcoin);

        this.ccAssets = ccAssets;
        populateSmartcoins(ccAssets);
    }

    /**
     * Checks whether a user has some non-zero balance of the selected reward token and flashes
     * a warning toast in case he/she doesn't.
     *
     * @param asset: The asset used as a reward
     */
    private void verifyRewardTokenOwnership(Asset asset){
        UserAccount userAccount = null;
        accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.get(i).isSelected) {
                userAccount = new UserAccount(accountDetails.get(i).account_id, accountDetails.get(i).account_name);
            }
        }
        if(userAccount != null){
            AssetAmount balance = database.getBalance(userAccount, asset);
            if(balance.getAmount().longValue() == 0){
                String template = getResources().getString(R.string.warning_insufficient_reward_asset_balance);
                Toast.makeText(this, String.format(template, asset.getSymbol()), Toast.LENGTH_LONG).show();
            }
        }
    }
}