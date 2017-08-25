package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.gson.Gson;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Currency;
import java.util.List;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.database.BlockpayDatabase;
import de.bitsharesmunich.graphenej.Asset;
import de.bitsharesmunich.graphenej.LimitOrder;
import de.bitsharesmunich.graphenej.OrderBook;
import de.bitsharesmunich.graphenej.api.GetLimitOrders;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.ExchangeRateListener;
import de.bitsharesmunich.models.Smartcoin;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.TinyDB;


public class KeypadActivity extends BaseActivity implements View.OnClickListener {
    private final String TAG = this.getClass().getName();

    /**
     * Constant used to define a fixed maximum precision for all fiat currencies.
     */
    private final int MAX_FIAT_DECIMAL_PRECISION = 2;

    /* The maximum number of limit orders to retrieve */
    private static final int MAX_LIMIT_ORDERS = 100;

    @BindView(R.id.btnOne)
    Button btnOne;

    @BindView(R.id.btnTwo)
    Button btnTwo;

    @BindView(R.id.btnThree)
    Button btnThree;

    @BindView(R.id.btnFour)
    Button btnFour;

    @BindView(R.id.btnFive)
    Button btnFive;

    @BindView(R.id.btnSix)
    Button btnSix;

    @BindView(R.id.btnSeven)
    Button btnSeven;

    @BindView(R.id.btnEight)
    Button btnEight;

    @BindView(R.id.btnNine)
    Button btnNine;

    @BindView(R.id.btnZero)
    Button btnZero;

    @BindView(R.id.btnDoubleZero)
    TextView btnDoubleZero;

    @BindView(R.id.txtScreen)
    TextView txtScreen;

    @BindView(R.id.txtTotal)
    TextView txtTotal;

    @BindView(R.id.ivCoin)
    ImageView ivCoin;

    @BindView((R.id.btnForward))
    Button btnForward;

    @BindView(R.id.btnDot)
    Button btnDot;

    @BindView(R.id.btnPlus)
    Button btnPlus;

    String coinType = "";
    String coinName = "";
    ImageLoader imageLoader;
    Locale locale;
    NumberFormat format;
    CountDownTimer countDownTimer;

    /**
     * User's selected input currency
     */
    private Currency mFiatCurrency;

    /**
     * String representing the current decimal separator
     */
    private String dot_string;

    /**
     * List of prices entered
     */
    private ArrayList<Double> prices;

    /**
     * Variable holding the total sum of individual entries
     */
    private double mTotal;

    /* Database interface */
    private BlockpayDatabase database;

    /* Assets used in the app */
    private Asset inputAsset;
    private Asset coreAsset;
    private Asset outputAsset;

    /* Important exchange rates */
    private double mCoreToOutputExchangeRate = -1.0;
    private double mInputToOutputExchangeRate = -1.0;

    /**
     * Order book between the 2 assets of interest
     */
    private OrderBook mOrderBook;

    /**
     * Worker thread in charge of getting the order book data
     */
    private WebsocketWorkerThread orderBookThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keypad);
        ButterKnife.bind(this);

        dot_string = btnDot.getText().toString();

        setToolbarAndNavigationBar(getString(R.string.txt_amount_due), true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        findViewById(R.id.toolbar).findViewById(R.id.drawer_layout_icon).setVisibility(View.GONE);

        Intent intent = getIntent();
        coinType = intent.getStringExtra("inputCoinType");
        coinName = intent.getStringExtra("inputCoinName");

        Helper.initImageLoader(getApplicationContext());
        imageLoader = ImageLoader.getInstance();
        String imgUri = "drawable://" + getResources().getIdentifier(Helper.getCoinImagePath(coinType), "drawable", getPackageName());
        imageLoader.displayImage(imgUri, ivCoin);

        String fiatCurrency = Helper.fetchStringSharePref(this, Constants.KEY_CURRENCY, Constants.DEFAULT_CURRENCY);
        if (fiatCurrency.equals("")) {
            fiatCurrency = Constants.DEFAULT_CURRENCY;
        }
        mFiatCurrency = Currency.getInstance(fiatCurrency);

        locale = getResources().getConfiguration().locale;
        format = NumberFormat.getInstance(locale);

        btnOne.setOnClickListener(this);
        btnTwo.setOnClickListener(this);
        btnThree.setOnClickListener(this);
        btnFour.setOnClickListener(this);
        btnFive.setOnClickListener(this);
        btnSix.setOnClickListener(this);
        btnSeven.setOnClickListener(this);
        btnEight.setOnClickListener(this);
        btnNine.setOnClickListener(this);
        btnZero.setOnClickListener(this);
        btnDoubleZero.setOnClickListener(this);
        btnPlus.setOnClickListener(this);

        keypadNumbersLocalization();

        // Creating list of prices, an initial way to support multiple items in a purchase
        prices = new ArrayList<>();

        /* Obtaining an instance of the database interface */
        database = new BlockpayDatabase(this);

        /* Instantiating input asset */
        final TinyDB tinyDB = new TinyDB(getApplicationContext());
        inputAsset = database.getAssetBySymbol(tinyDB.getString(Constants.KEY_CURRENCY));

        /* Instantiating output asset */
        String selectedSmartCoinString = Helper.fetchObjectSharePref(getApplicationContext(), getString(R.string.pref_desired_smart_coin));
        Smartcoin selectedSmartCoin = gson.fromJson(selectedSmartCoinString, Smartcoin.class);
        outputAsset = database.fillAssetDetails(new Asset(selectedSmartCoin.asset_id));

        /* Setting up core asset and deducing the network fees */
        coreAsset = database.fillAssetDetails(new Asset(Constants.CORE_ASSET_ID));

        /* Requesting the order book */
        orderBookThread = new WebsocketWorkerThread(new GetLimitOrders(coreAsset.getObjectId(), outputAsset.getObjectId(), MAX_LIMIT_ORDERS, mLimitOrdersListener));
        orderBookThread.start();

        /* Setting up exchange rate providers and using them in order to get data */
        ExchangeRateProvider coreToOutputProvider;
        ExchangeRateProvider inputToOutputProvider;

        /* Requesting core to output exchange rate only if these two assets are different */
        if(coreAsset.equals(outputAsset)){
            mCoreToOutputExchangeRate = 1.0;
        }else{
            coreToOutputProvider = new ExchangeRateProvider(this, coreAsset, outputAsset, new ExchangeRateListener() {
                @Override
                public void onExchangeRate(ExchangeRateResult exchangeRate) {
                    mCoreToOutputExchangeRate = exchangeRate.conversionRate;
                }
            });
            coreToOutputProvider.dispatch();
        }

        /* Requesting input to output exchange rate only if these two assets are different */
        if(tinyDB.getString(Constants.KEY_CURRENCY).equals(outputAsset.getSymbol())){
            mInputToOutputExchangeRate = 1.0;
        }else{
            inputToOutputProvider = new ExchangeRateProvider(KeypadActivity.this, inputAsset, outputAsset, new ExchangeRateListener() {

                @Override
                public void onExchangeRate(ExchangeRateResult exchangeRate) {
                    mInputToOutputExchangeRate = exchangeRate.conversionRate;
                }
            });
            inputToOutputProvider.dispatch();
        }

        /* Setting up countdown timer */
        countDownTimer();
    }

    /**
     * Listener called when we get a response from the network with the full order book.
     */
    private WitnessResponseListener mLimitOrdersListener = new WitnessResponseListener() {
        @Override
        public void onSuccess(WitnessResponse response) {
            Log.d(TAG,"mLimitOrdersListener.onSuccess");
            List<LimitOrder> orders = (List<LimitOrder>) response.result;
            mOrderBook = new OrderBook(orders);
        }

        @Override
        public void onError(BaseResponse.Error error) {
            Log.e(TAG,"mLimitOrdersListener.onError. Msg: "+error.message);
        }
    };

    private void nextButtonPressed() {
        Intent qrintent = new Intent(getApplicationContext(), QRCodeActivity.class);
        qrintent.putExtra(QRCodeActivity.KEY_INPUT_COIN_TYPE, coinType);
        qrintent.putExtra(QRCodeActivity.KEY_AMOUNT, mTotal);
        qrintent.putExtra(QRCodeActivity.KEY_INPUT_COIN_NAME, coinName);
        qrintent.putExtra(QRCodeActivity.KEY_CORE_TO_OUTPUT_EXCHANGE, mCoreToOutputExchangeRate);
        qrintent.putExtra(QRCodeActivity.KEY_INPUT_TO_OUTPUT_EXCHANGE, mInputToOutputExchangeRate);
        if(mOrderBook != null){
            Gson gson = new Gson();
            String serializedOrderBook = gson.toJson(mOrderBook);
            qrintent.putExtra(QRCodeActivity.KEY_ORDER_BOOK, serializedOrderBook);
        }
        startActivity(qrintent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    @OnClick(R.id.btnForward)
    public void forward() {
        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blinkfast);
        (findViewById(R.id.btnForward)).startAnimation(animation1);

        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                nextButtonPressed();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // here you can play your sound
            }
        });


    }

    private void cancelButtonPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    @OnClick(R.id.btnBackward)
    public void backward() {
        Animation animation1 = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.blinkfast);
        (findViewById(R.id.btnBackward)).startAnimation(animation1);

        animation1.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                cancelButtonPressed();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                // here you can play your sound
            }
        });
    }

    @OnClick(R.id.ibCancel)
    public void backSpace() {
        String currentDisplay = txtScreen.getText().toString();
        if (currentDisplay.length() > 0) {
            currentDisplay = currentDisplay.substring(0, currentDisplay.length() - 1);
            txtScreen.setText(currentDisplay);
        }
        calculateTotal();
        checkIfEnableForward();
    }

    @Override
    public void onClick(View v) {
        Button button = (Button) v;
        String currentkey = button.getText().toString();
        String currentDisplay = txtScreen.getText().toString();

        if (currentkey.equals("0") || currentkey.equals("00")) {
            if (!currentDisplay.isEmpty()) {
                if (currentkey.equals("00")) {
                    String[] pricesStr = currentDisplay.split("\\+");
                    if (pricesStr[pricesStr.length - 1].indexOf(dot_string) != -1) {
                        txtScreen.append(currentkey);
                    } else {
                        Log.w(TAG, "Should not be adding zeroes");
                    }
                } else {
                    txtScreen.append(currentkey);
                }
            }
        } else if (currentkey.equals("+")) {
            if (currentDisplay.length() > 0 &&
                    currentDisplay.lastIndexOf("+") != currentDisplay.length() - 1 &&       // Not trailing '+'
                    currentDisplay.lastIndexOf(dot_string) != currentDisplay.length() -1) { // Not trailing ',|.'
                txtScreen.append(currentkey);
            }
        } else {
            txtScreen.append(currentkey);
        }

        calculateTotal();
        checkIfEnableForward();
    }

    private void calculateTotal() {
        // Updating current display
        String currentDisplay = txtScreen.getText().toString();

        try {
            String[] pricesTxt = currentDisplay.split("\\+");
            prices.clear();
            mTotal = 0.0;
            for (String p : pricesTxt) {
                if (!p.isEmpty()) {
                    Number number = format.parse(removeSpecialCharacters(p));
                    prices.add(number.doubleValue());
                    mTotal += number.doubleValue();
                }
            }

            NumberFormat nf = Helper.currencyFormat(this, mFiatCurrency, locale, MAX_FIAT_DECIMAL_PRECISION);
            Log.d(TAG,"Got number formatter for currency with locale: "+locale);
            txtTotal.setText(nf.format(mTotal));
        } catch (ParseException e) {
            Log.e(TAG, "ParseException. Msg: " + e.getMessage());
        }
    }

    @OnClick(R.id.btnDot)
    void onDotPressed() {
        String currentDisplay = txtScreen.getText().toString();
        String[] entries = currentDisplay.split("\\+");
        if(currentDisplay.isEmpty()){
            txtScreen.append(dot_string);
        }else{
            if(currentDisplay.indexOf(dot_string) == -1){
                txtScreen.append(dot_string);
            } else if (entries.length > 0) {
                if(currentDisplay.lastIndexOf("+") == currentDisplay.length() - 1){
                    txtScreen.append(dot_string);
                }else if(entries[entries.length - 1].indexOf(dot_string) == -1){
                    txtScreen.append(dot_string);
                }
            }
        }
    }

    /**
     * Returns the number of decimal places used so far, in order to prevent the user
     * from getting past the 2 decimal limit imposed for fiat currencies.
     *
     * @return
     */
    private int getDecimalPlaces() {
        String number = txtScreen.getText().toString();
        String[] splitted = number.split("[.,]");
        if (splitted.length > 1) {
            return splitted[1].length();
        } else {
            return 0;
        }
    }

    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }

    private void checkIfEnableForward() {
        if (mTotal > 0) {
            btnForward.setEnabled(true);
        } else {
            btnForward.setEnabled(false);
        }
    }

    private void keypadNumbersLocalization() {
        Log.d(TAG, "Locale. language: "+locale.getLanguage()+", country: "+locale.getCountry()+", locale: "+locale);
        Log.d(TAG, "Decimal separator: "+Helper.getDecimalSeparator(locale));

        btnOne.setText(Helper.setLocaleNumberFormat(locale, 1));
        btnTwo.setText(Helper.setLocaleNumberFormat(locale, 2));
        btnThree.setText(Helper.setLocaleNumberFormat(locale, 3));
        btnFour.setText(Helper.setLocaleNumberFormat(locale, 4));
        btnFive.setText(Helper.setLocaleNumberFormat(locale, 5));
        btnSix.setText(Helper.setLocaleNumberFormat(locale, 6));
        btnSeven.setText(Helper.setLocaleNumberFormat(locale, 7));
        btnEight.setText(Helper.setLocaleNumberFormat(locale, 8));
        btnNine.setText(Helper.setLocaleNumberFormat(locale, 9));
        btnZero.setText(Helper.setLocaleNumberFormat(locale, 0));
        btnDot.setText(String.valueOf(Helper.getDecimalSeparator(locale)));
        dot_string = String.valueOf(Helper.getDecimalSeparator(locale));
        btnDoubleZero.setText(Helper.setLocaleNumberFormat(locale, 0) + "" + Helper.setLocaleNumberFormat(locale, 0));

    }

    private String removeSpecialCharacters(String inputNumber) {
        inputNumber = inputNumber.replaceAll("[^\\d.,]", "");

        inputNumber = inputNumber.replace("٬", "");
        inputNumber = inputNumber.replace(String.valueOf((char) 160), "");
        if (dot_string.equals(",")) {
            inputNumber = inputNumber.replace(".", "");
        } else if (dot_string.equals(".")) {
            inputNumber = inputNumber.replace(",", "");
        }
        return inputNumber;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onUserInteraction() {
        countDownTimer.cancel();
        countDownTimer();
    }

    private void countDownTimer() {
        countDownTimer = new CountDownTimer(15000, 1) {
            public void onTick(long millisUntilFinished) {
            }

            public void onFinish() {
                if (!isFinishing()) {
                    finish();
                    overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
                }
            }
        }.start();
    }
}