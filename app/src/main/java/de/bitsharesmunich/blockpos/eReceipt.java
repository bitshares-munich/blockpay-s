package de.bitsharesmunich.blockpos;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.interfaces.GravatarDelegate;
import de.bitsharesmunich.interfaces.IBalancesDelegate;
import de.bitsharesmunich.models.EquivalentFiatStorage;
import de.bitsharesmunich.models.Gravatar;
import de.bitsharesmunich.models.MerchantEmail;
import de.bitsharesmunich.models.TransactionIdResponse;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.SupportMethods;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by Syed Muhammad Muzzammil on 5/26/16.
 */

public class eReceipt extends BaseActivity implements IBalancesDelegate,GravatarDelegate {
    private final String TAG = this.getClass().getName();
    Context context;

    @BindView(R.id.ivOtherGravatar)
    ImageView ivOtherGravatar;

    @BindView(R.id.tvOtherCompany)
    TextView tvOtherCompany;

    @BindView(R.id.TvBlockNum)
    TextView TvBlockNum;

    @BindView(R.id.tvTime)
    TextView tvTime;

    @BindView(R.id.tvOtherName)
    TextView tvOtherName;

    @BindView(R.id.tvUserName)
    TextView tvUserName;

    @BindView(R.id.tvOtherId)
    TextView tvOtherId;

    @BindView(R.id.tvUserId)
    TextView tvUserId;

    @BindView(R.id.tvMemo)
    TextView tvMemo;


    @BindView(R.id.tvAmount)
    TextView tvAmount;

    @BindView(R.id.tvAddress)
    TextView tvAddress;

    @BindView(R.id.tvContact)
    TextView tvContact;


    @BindView(R.id.tvAmountEquivalent)
    TextView tvAmountEquivalent;

    @BindView(R.id.tvBlockNumber)
    TextView tvBlockNumber;

    @BindView(R.id.tvTrxInBlock)
    TextView tvTrxInBlock;

    @BindView(R.id.tvFee)
    TextView tvFee;

    @BindView(R.id.tvFeeEquivalent)
    TextView tvFeeEquivalent;

    @BindView(R.id.tvPaymentAmount)
    TextView tvPaymentAmount;

    @BindView(R.id.tvPaymentEquivalent)
    TextView tvPaymentEquivalent;

    @BindView(R.id.tvTotalEquivalent)
    TextView tvTotalEquivalent;

    @BindView(R.id.tvTotal)
    TextView tvTotal;

    @BindView(R.id.tvUserStatus)
    TextView tvUserStatus;

    @BindView(R.id.ivImageTag)
    ImageView ivImageTag;

    @BindView(R.id.scrollView)
    ScrollView scrollView;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.llall)
    LinearLayout llall;

    @BindView(R.id.buttonSend)
    ImageButton buttonSend;

    //int names_in_work;
    //int names_total_size;
    int assets_id_in_work;
    int assets_id_total_size;

    HashMap<String, String> map = new HashMap<>();
    HashMap<String, String> eReciptmap = new HashMap<>();
    HashMap<String, String> OPmap = new HashMap<>();
    HashMap<String, String> Freemap = new HashMap<>();
    HashMap<String, String> Amountmap = new HashMap<>();
    HashMap<String, String> Memomap = new HashMap<>();
    List<String> Assetid = new ArrayList<>();
    HashMap<String, HashMap<String, String>> SymbolsPrecisions = new HashMap<>();
    String memoMsg;
    String date;
    String otherName;
    String userName;
    Boolean isSent = false;
    String feeSymbol = "";
    String amountSymbol = "";
    String feeAmount = "";
    String amountAmount = "";
    String time = "";
    String timeZone = "";
    ProgressDialog progressDialog;
    boolean loadComplete = false;
    boolean btnPress = false;


    // Storage Permissions
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.e_receipt);
        ButterKnife.bind(this);

        context = getApplicationContext();
        progressDialog = new ProgressDialog(this);
        BlockpayApplication.registerBalancesDelegateEReceipt(this);
       // setTitle(getResources().getString(R.string.e_receipt_activity_name));
        hideProgressBar();
        Intent intent = getIntent();
        String eReciept = intent.getStringExtra(getResources().getString(R.string.e_receipt));

        memoMsg = intent.getStringExtra("Memo");
        date = intent.getStringExtra("Date");
        time = intent.getStringExtra("Time");
        timeZone = intent.getStringExtra("TimeZone");

        if (intent.getBooleanExtra("Sent", false)) {
            userName = intent.getStringExtra("From");
            otherName = intent.getStringExtra("To");
            //tvUserAccount.setText("Recieved From");
            //tvOtherAccount.setText("Sent To");
            tvUserStatus.setText(getString(R.string.sender_account));
            isSent = true;
            ivImageTag.setImageResource(R.drawable.send);
//            email = get_email(to);
        } else {
            // tvOtherAccount.setText("Recieved From");
            // tvUserAccount.setText("Sent To");
            tvUserStatus.setText(getString(R.string.receiver_account));
            userName = intent.getStringExtra("To");
            otherName = intent.getStringExtra("From");
            isSent = false;
            ivImageTag.setImageResource(R.drawable.receive);
//            email = get_email(from);
        }
        tvOtherName.setText(otherName);
        tvUserName.setText(userName);
        TvBlockNum.setText(date);
        tvTime.setText(time + " " + timeZone);



        //  emailOther = get_email(otherName);
        //   emailOther = "fawaz_ahmed@live.com";

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;

        ivOtherGravatar.requestLayout();

        ivOtherGravatar.getLayoutParams().height = (width * 40) / 100;
        ivOtherGravatar.getLayoutParams().width = (width * 40) / 100;


        fetchGravatarInfo(get_email());

        init(eReciept);

        setToolbarAndNavigationBar(getResources().getString(R.string.e_receipt_activity_name), false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

      //  setBackButton(true);
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
    public void OnUpdate(String s, int id) {
        if (id == 18) {
            //String result = SupportMethods.ParseJsonObject(s,"result");
            //String time = SupportMethods.ParseJsonObject(result,"timestamp");
            assets_id_in_work = 0;
            get_asset(Assetid.get(assets_id_in_work), "19");
        } else if (id == 19) {
            if (assets_id_in_work < assets_id_total_size) {
                String result = SupportMethods.ParseJsonObject(s, "result");
                String assetObject = SupportMethods.ParseObjectFromJsonArray(result, 0);
                String symbol = SupportMethods.ParseJsonObject(assetObject, "symbol");
                String precision = SupportMethods.ParseJsonObject(assetObject, "precision");
                HashMap<String, String> de = new HashMap<>();
                de.put("symbol", symbol);
                de.put("precision", precision);
                SymbolsPrecisions.put(Assetid.get(assets_id_in_work), de);
                if (assets_id_in_work == (assets_id_total_size - 1)) {
                    onLastCall();
                }
                assets_id_in_work++;
                if (assets_id_in_work < Assetid.size())
                    get_asset(Assetid.get(assets_id_in_work), "19");
            }
        }
    }


    String transactionIdClipped = "";
    Boolean transactionIdUpdated = false;

    private void getTransactionId(final String block_num, final String trx_in_block)
    {
        try {
            final Handler handler = new Handler();

            final Runnable updateTask = new Runnable() {
                @Override
                public void run() {
                    HashMap<String, String> hashMap = new HashMap<>();
                    hashMap.put("method", "get_transaction_id");
                    hashMap.put("block_num", block_num);
                    hashMap.put("trx_in_block", trx_in_block);

                    ServiceGenerator sg = new ServiceGenerator(""/*getString(R.string.account_from_brainkey_url)*/);
                    IWebService service = sg.getService(IWebService.class);
                    final Call<TransactionIdResponse> postingService = service.getTransactionIdComponent(hashMap);

                    postingService.enqueue(new Callback<TransactionIdResponse>() {

                        @Override
                        public void onResponse(Call<TransactionIdResponse> call, Response<TransactionIdResponse> response) {
                            if (response.isSuccessful()) {
                                TransactionIdResponse resp = response.body();

                                if (resp.status.equals("success")) {
                                    try {
                                        String trx_id = resp.transaction_id;
                                        transactionIdClipped = trx_id.substring(0, 7);
                                        transactionIdUpdated = true;

                                        checkifloadingComplete();

//                                        if (btnPress)
//                                        {
//                                            try
//                                            {
//                                                Handler handlerStop = new Handler();
//                                                handlerStop.postDelayed(new Runnable() {
//                                                    public void run() {
//                                                        //hideDialog();
//                                                       // hideProgressBar();
//                                                    }
//                                                }, 1500);
//                                            }
//                                            catch (Exception e)
//                                            {
//
//                                            }
//                                            //generatePdf();
//                                            generatepdfDoc();
//                                        }



                                    } catch (Exception e) {
                                        //e.printStackTrace();
                                        getTransactionId(block_num, trx_in_block);
                                    }
                                } else {
                                    getTransactionId(block_num, trx_in_block);
                                }

                            } else {
                                getTransactionId(block_num, trx_in_block);
                            }
                        }

                        @Override
                        public void onFailure(Call<TransactionIdResponse> call, Throwable t) {
                            getTransactionId(block_num, trx_in_block);
                        }
                    });
                }
            };
            handler.postDelayed(updateTask, 100);
        }
        catch (Exception e)
        {

        }
    }

    void init(String eRecipt) {
        eReciptmap.put("id", SupportMethods.ParseJsonObject(eRecipt, "id"));
        eReciptmap.put("op", SupportMethods.ParseJsonObject(eRecipt, "op"));
        //eReciptmap.put("result", SupportMethods.ParseJsonObject(eRecipt, "result"));
        String block_num = SupportMethods.ParseJsonObject(eRecipt, "block_num");
        eReciptmap.put("block_num", block_num);
        String trx_in_block = SupportMethods.ParseJsonObject(eRecipt, "trx_in_block");
        eReciptmap.put("trx_in_block", trx_in_block);
//        getTransactionId(block_num, trx_in_block);
        eReciptmap.put("op_in_trx", SupportMethods.ParseJsonObject(eRecipt, "op_in_trx"));
        eReciptmap.put("virtual_op", SupportMethods.ParseJsonObject(eRecipt, "virtual_op"));
        String fetch_OP = SupportMethods.ParseObjectFromJsonArray(eReciptmap.get("op"), 1);
        OPmap.put("fee", SupportMethods.ParseJsonObject(fetch_OP, "fee"));
        OPmap.put("from", SupportMethods.ParseJsonObject(fetch_OP, "from"));
        OPmap.put("to", SupportMethods.ParseJsonObject(fetch_OP, "to"));
        OPmap.put("amount", SupportMethods.ParseJsonObject(fetch_OP, "amount"));
        OPmap.put("memo", SupportMethods.ParseJsonObject(fetch_OP, "memo"));
        OPmap.put("extensions", SupportMethods.ParseJsonObject(fetch_OP, "extensions"));
        Freemap.put("amount", SupportMethods.ParseJsonObject(OPmap.get("fee"), "amount"));
        Freemap.put("asset_id", SupportMethods.ParseJsonObject(OPmap.get("fee"), "asset_id"));
        Amountmap.put("asset_id", SupportMethods.ParseJsonObject(OPmap.get("amount"), "asset_id"));
        Amountmap.put("amount", SupportMethods.ParseJsonObject(OPmap.get("amount"), "amount"));
        Memomap.put("from", SupportMethods.ParseJsonObject(OPmap.get("memo"), "from"));
        Memomap.put("to", SupportMethods.ParseJsonObject(OPmap.get("memo"), "to"));
        Memomap.put("nonce", SupportMethods.ParseJsonObject(OPmap.get("memo"), "nonce"));
        Memomap.put("message", SupportMethods.ParseJsonObject(OPmap.get("memo"), "message"));

        get_Time(eReciptmap.get("block_num"), "18");

        Assetid.add(Freemap.get("asset_id"));
        Assetid.add(Amountmap.get("asset_id"));
        assets_id_total_size = Assetid.size();
    }

    void onLastCall() {
        this.runOnUiThread(new Runnable() {
            public void run() {

                //   createEmail(emailOther, ivOtherGravatar);

                // createEmail(emailUser,ivUserGravatar);


                AssetsSymbols assetsSymbols = new AssetsSymbols(context);

                HashMap<String, String> sym_preFee = SymbolsPrecisions.get(Freemap.get("asset_id"));
                feeAmount = SupportMethods.ConvertValueintoPrecision(sym_preFee.get("precision"), Freemap.get("amount"));
                feeSymbol = sym_preFee.get("symbol");

                HashMap<String, String> sym_preAmount = SymbolsPrecisions.get(Amountmap.get("asset_id"));
                amountAmount = SupportMethods.ConvertValueintoPrecision(sym_preAmount.get("precision"), Amountmap.get("amount"));
                amountSymbol = sym_preAmount.get("symbol");


                EquivalentComponents equivalentAmount = new EquivalentComponents();
                equivalentAmount.Amount = Float.parseFloat(amountAmount);
                equivalentAmount.assetSymbol = amountSymbol;
                equivalentAmount.id = 0;

                EquivalentComponents equivalentFee = new EquivalentComponents();
                equivalentFee.Amount = Float.parseFloat(feeAmount);
                equivalentFee.assetSymbol = feeSymbol;
                equivalentFee.id = 1;

                ArrayList<EquivalentComponents> arrayList = new ArrayList<>();
                arrayList.add(equivalentAmount);
                arrayList.add(equivalentFee);

                getEquivalentComponents(arrayList);

//                if(faitAmount!=null && fiatSymbol!=null) {
//                    tvTotalEquivalent.setText(faitAmount + " " + fiatSymbol);
//                    tvPaymentEquivalent.setText(faitAmount + " " + fiatSymbol);
//                    ifEquivalentFailed();
//                }

                feeSymbol = assetsSymbols.updateString(sym_preFee.get("symbol"));
                amountSymbol = assetsSymbols.updateString(sym_preAmount.get("symbol"));

                tvBlockNumber.setText(eReciptmap.get("block_num"));
                tvTrxInBlock.setText(eReciptmap.get("id"));


                tvAmount.setText(amountAmount + " " + amountSymbol);
                tvFee.setText(feeAmount + " " + feeSymbol);
                tvTotal.setText(tvAmount.getText() + " + " + tvFee.getText());

                tvPaymentAmount.setText(tvTotal.getText());

                if (isSent) {
                    tvOtherId.setText(OPmap.get("to"));
                    tvUserId.setText(OPmap.get("from"));
                } else {
                    tvOtherId.setText(OPmap.get("from"));
                    tvUserId.setText(OPmap.get("to"));
                }

//                fetchGravatarInfo(get_email(tvOtherId.getText().toString()));
                tvMemo.setText(memoMsg);

                loadComplete = true;
                checkifloadingComplete();
               // hideProgressBar();

            }
        });
    }

    void get_Time(String block_num, String id) {
        int db_id = Helper.fetchIntSharePref(context, context.getString(R.string.sharePref_database), 9999);
        //  {"id":4,"method":"call","params":[2,"get_block_header",[6356159]]}
        String getDetails = "{\"id\":" + id + ",\"method\":\"call\",\"params\":[" + db_id + ",\"get_block_header\",[ " + block_num + "]]}";
//        BlockpayApplication.send(getDetails);
    }

    void get_names(String name_id, String id) {
        int db_id = Helper.fetchIntSharePref(context, context.getString(R.string.sharePref_database), 9999);
        //    {"id":4,"method":"call","params":[2,"get_accounts",[["1.2.101520"]]]}
        String getDetails = "{\"id\":" + id + ",\"method\":\"call\",\"params\":[" + db_id + ",\"get_accounts\",[[\"" + name_id + "\"]]]}";
//        BlockpayApplication.send(getDetails);
    }

    void get_asset(String asset, String id) {
        //{"id":1,"method":"get_assets","params":[["1.3.0","1.3.120"]]}
        String getDetails = "{\"id\":" + id + ",\"method\":\"get_assets\",\"params\":[[\"" + asset + "\"]]}";
//        BlockpayApplication.send(getDetails);
    }

//    @OnClick(R.id.buttonSend)
//    public void onSendButton() {
//        btnPress = true;
//        checkifloadingComplete();
////        if(loadComplete) {
////            showProgressBar();
////            if (!transactionIdUpdated) {
//                //showDialog("", getResources().getString(R.string.updating_transaction_id));
//                //showProgressBar();
////            } else {
//             //   showProgressBar();
////                Handler handler = new HatransactionIdUpdatedndler();
////                handler.postDelayed(new Runnable() {
////                    public void run() {
////                        //showDialog("", getResources().getString(R.string.updating_transaction_id));
////                        showProgressBar();
////                    }
////                }, 0);
////                Handler handlerStop = new Handler();
////                handlerStop.postDelayed(new Runnable() {
////                    public void run() {
////                        //  hideDialog();
////                        hideProgressBar();
////
////                    }
////                }, 1500);
//                //generatePdf();
////            }
////
////        }
////        else{
////            showProgressBar();
////            Toast.makeText(context,getString(R.string.updating_transaction_id),Toast.LENGTH_LONG).show();
////        }
//
//    }

    String get_email() {
        return MerchantEmail.getMerchantEmail(context);
    }

    @Override
    public void updateProfile(Gravatar myGravatar) {
        tvOtherCompany.setText(myGravatar.companyName);
        tvAddress.setText(myGravatar.address);
        tvContact.setText(myGravatar.url);
    }

    @Override
    public void updateCompanyLogo(Bitmap logo) {
        isGravatarRecieved = true;
        Helper.saveToInternalStorage(getApplicationContext(), logo);
        Bitmap bitmap = getRoundedCornerBitmap(logo);
        ivOtherGravatar.setImageBitmap(bitmap);
        checkifloadingComplete();
    }

    @Override
    public void failureUpdateProfile() {

    }

    @Override
    public void failureUpdateLogo() {

    }
    Boolean isEmail = false;
    Boolean isGravatarRecieved = false;
    GravatarDelegate instance(){
        return this;
    }
    void fetchGravatarInfo(String email){
        if(!email.isEmpty()) {
            isEmail = true;
            Gravatar.getInstance(instance()).fetch(email);
        }
    }

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
                SupportMethods.testing("alpha", e.getMessage(), "error");
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            if (result == null) bmImage.setVisibility(View.GONE);
            else {
                // Bitmap corner = getRoundedCornerBitmap(result);
                bmImage.setImageBitmap(result);
            }
        }
    }

    String finalFaitCurrency;


    private class EquivalentComponents {
        int id;
        float Amount;
        String assetSymbol;
        float faitAmount;
        Boolean available;
        String faitAssetSymbol;

        float getAmount() {
            return this.Amount;
        }

        String getAssetSymbol() {
            return this.assetSymbol;
        }
    }

//    private void getEquivalentComponents(final ArrayList<EquivalentComponents> equivalentComponentses) {
//        String fiatCurrency = Helper.getFadeCurrency(context);
//
//        if (fiatCurrency.isEmpty()) {
//            fiatCurrency = "EUR";
//        }
//
//        String values = "";
//        for (int i = 0; i < equivalentComponentses.size(); i++) {
//            EquivalentComponents transactionDetails = equivalentComponentses.get(i);
//            if (!transactionDetails.assetSymbol.equals(fiatCurrency)) {
//                values += transactionDetails.assetSymbol + ":" + fiatCurrency + ",";
//            }
//        }
//
//        if (values.isEmpty()) {
//            return;
//        }
//
//        HashMap<String, String> hashMap = new HashMap<>();
//        hashMap.put("method", "equivalent_component");
//        if (values.length() > 1)
//            hashMap.put("values", values.substring(0, values.length() - 1));
//        else hashMap.put("values", "");
//        ServiceGenerator sg = new ServiceGenerator(context.getString(R.string.account_from_brainkey_url));
//        IWebService service = sg.getService(IWebService.class);
//        final Call<EquivalentComponentResponse> postingService = service.getEquivalentComponent(hashMap);
//        finalFaitCurrency = fiatCurrency;
//
//        postingService.enqueue(new Callback<EquivalentComponentResponse>() {
//            @Override
//            public void onResponse(Response<EquivalentComponentResponse> response) {
//                if (response.isSuccess()) {
//                    EquivalentComponentResponse resp = response.body();
//                    if (resp.status.equals("success")) {
//                        try {
//                            JSONObject rates = new JSONObject(resp.rates);
//                            Iterator<String> keys = rates.keys();
//                            HashMap hm = new HashMap();
//
//                            while (keys.hasNext()) {
//                                String key = keys.next();
//                                hm.put(key.split(":")[0], rates.get(key));
//                            }
//
//                            try {
//                                for (int i = 0; i < equivalentComponentses.size(); i++) {
//                                    String asset = equivalentComponentses.get(i).getAssetSymbol();
//                                    String amount = String.valueOf(equivalentComponentses.get(i).getAmount());
//                                    equivalentComponentses.get(i).available = false;
//                                    if (!amount.isEmpty() && hm.containsKey(asset)) {
//                                        equivalentComponentses.get(i).available = true;
//                                        Currency currency = Currency.getInstance(finalFaitCurrency);
//                                        Double eqAmount = Double.parseDouble(amount) * Double.parseDouble(hm.get(asset).toString());
//                                        equivalentComponentses.get(i).faitAssetSymbol = currency.getSymbol();
//                                        equivalentComponentses.get(i).faitAmount = Float.parseFloat(String.format("%.4f", eqAmount));
//                                    }
//                                }
//                            } catch (Exception e) {
//                                // ifEquivalentFailed();
//                            }
//
//                            setEquivalentComponents(equivalentComponentses);
//
//                        } catch (JSONException e) {
//                            ifEquivalentFailed();
//                            //  testing("trasac",e, "found,found");
//                        }
////                        Toast.makeText(getActivity(), getString(R.string.upgrade_success), Toast.LENGTH_SHORT).show();
//                    } else {
//                        ifEquivalentFailed();
//                        //   testing("trasac","1", "found,found");
////                        Toast.makeText(getActivity(), getString(R.string.upgrade_failed), Toast.LENGTH_SHORT).show();
//                    }
//                } else {
//                    ifEquivalentFailed();
//                    //  testing("trasac","2", "found,found");
//                    Toast.makeText(context, context.getString(R.string.upgrade_failed), Toast.LENGTH_SHORT).show();
//                }
//            }
//
//            @Override
//            public void onFailure(Throwable t) {
//                ifEquivalentFailed();
//                Toast.makeText(context, context.getString(R.string.txt_no_internet_connection), Toast.LENGTH_SHORT).show();
//            }
//
//        });
//    }


    private void getEquivalentComponents(final ArrayList<EquivalentComponents> equivalentComponentses) {

        String faitCurrency = Helper.getFadeCurrency(context);

        if (faitCurrency.isEmpty()) {
            faitCurrency = "EUR";
        }

        String values = "";
        for (int i = 0; i < equivalentComponentses.size(); i++) {
            EquivalentComponents transactionDetails = equivalentComponentses.get(i);
            if (!transactionDetails.assetSymbol.equals(faitCurrency)) {
                values += transactionDetails.assetSymbol + ":" + faitCurrency + ",";
            }
        }

        if (values.isEmpty()) {
            return;
        }

        EquivalentFiatStorage equivalentFiatStorage = new EquivalentFiatStorage(context);
        HashMap hm = equivalentFiatStorage.getEqHM(faitCurrency);

        try {
            for (int i = 0; i < equivalentComponentses.size(); i++) {
                String asset = equivalentComponentses.get(i).getAssetSymbol();
                String amount = String.valueOf(equivalentComponentses.get(i).getAmount());
                equivalentComponentses.get(i).available = false;
                if (!amount.isEmpty() && hm.containsKey(asset)) {
                    equivalentComponentses.get(i).available = true;
                    Currency currency = Currency.getInstance(faitCurrency);
                    Double eqAmount = Double.parseDouble(amount) * Double.parseDouble(hm.get(asset).toString());
                    equivalentComponentses.get(i).faitAssetSymbol = currency.getSymbol();
                    equivalentComponentses.get(i).faitAmount = Float.parseFloat(String.format("%.4f", eqAmount));
                } else {
                    equivalentComponentses.get(i).faitAssetSymbol = "";
                    equivalentComponentses.get(i).faitAmount = 0f;
                }
            }
        } catch (Exception e) {
            ifEquivalentFailed();
        }

        setEquivalentComponents(equivalentComponentses);

        // ifEquivalentFailed();


    }

    void setEquivalentComponents(final ArrayList<EquivalentComponents> equivalentComponentse) {

        String value = "";
        Boolean available = false;

        EquivalentComponents equivalentAmount = equivalentComponentse.get(0);
        if (equivalentAmount.id == 0) {
            if (equivalentAmount.available) {
                available = true;
                tvAmountEquivalent.setText(equivalentAmount.faitAmount + " " + equivalentAmount.faitAssetSymbol);
            } else {
                available = false;
                tvAmountEquivalent.setVisibility(View.GONE);
                setWeight(tvAmount);
            }

            EquivalentComponents equivalentFee = equivalentComponentse.get(1);

            if (equivalentFee.id == 1) {
                if (available) {
                    tvFeeEquivalent.setText(equivalentFee.faitAmount + " " + equivalentFee.faitAssetSymbol);
                } else {
                    tvFeeEquivalent.setVisibility(View.GONE);
                    setWeight(tvFee);
                }
            }

            if (equivalentFee.id == 0) {
                if (equivalentAmount.available) {
                    available = true;
                    tvAmountEquivalent.setText(equivalentAmount.faitAmount + " " + equivalentAmount.faitAssetSymbol);
                } else {
                    available = false;
                    tvAmountEquivalent.setVisibility(View.GONE);
                    setWeight(tvAmount);
                }
            }

            if (equivalentAmount.id == 1) {
                if (available) {
                    tvFeeEquivalent.setText(equivalentFee.faitAmount + " " + equivalentFee.faitAssetSymbol);
                } else {
                    tvFeeEquivalent.setVisibility(View.GONE);
                    setWeight(tvFee);
                }
            }


            if (available) {
                tvTotalEquivalent.setText(equivalentAmount.faitAmount + equivalentFee.faitAmount + " " + equivalentAmount.faitAssetSymbol);
            } else {
                tvTotalEquivalent.setText(value);
                setWeight(tvTotal);
            }

            tvPaymentEquivalent.setText(tvTotalEquivalent.getText());
        }

    }

    void ifEquivalentFailed() {
        setWeight(tvAmount);
        setWeight(tvFee);
        setWeight(tvTotal);
    }

//    void createEmail(String email, ImageView logoView) {
//        String emailGravatarUrl = "https://www.gravatar.com/avatar/" + Helper.hash(email, Helper.MD5) + "?s=130&r=pg&d=404";
//        new DownloadImageTask(logoView)
//                .execute(emailGravatarUrl);
//    }

    public static Bitmap getRoundedCornerBitmap(Bitmap bitmap) {
        Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
                bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        final int color = 0xff424242;
        final Paint paint = new Paint();
        final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
        final RectF rectF = new RectF(rect);
        final float roundPx = 90;

        paint.setAntiAlias(true);
        canvas.drawARGB(0, 0, 0, 0);
        paint.setColor(color);
        canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(bitmap, rect, rect, paint);

        return output;
    }

    void setWeight(TextView textView) {
        ViewGroup.LayoutParams params = textView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        textView.setLayoutParams(params);
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    private void generatePdf()
    {
        try
        {
            showProgressBar();
         //   buttonSend.setVisibility(View.INVISIBLE);
            verifyStoragePermissions(this);
            final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getResources().getString(R.string.folder_name) + File.separator + "eReceipt-" + transactionIdClipped;
            final String pathPdf = path + ".pdf";

            Document document = new Document();
            PdfWriter.getInstance(document, new FileOutputStream(pathPdf));
            document.open();
//            buttonSend.setVisibility(View.INVISIBLE);
//            hideProgressBar();

//            Bitmap bitmap = Bitmap.createBitmap(
//                    scrollView.getChildAt(0).getWidth(),
//                    scrollView.getChildAt(0).getHeight(),
//                    Bitmap.Config.ARGB_8888);

            Bitmap bitmap = Bitmap.createBitmap(
                    llall.getWidth(),
                    llall.getHeight(),
                    Bitmap.Config.ARGB_8888);


            Canvas c = new Canvas(bitmap);
           // scrollView.getChildAt(0).draw(c);
            llall.draw(c);

          //  buttonSend.setVisibility(View.VISIBLE);
         //   showProgressBar();
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
            byte[] imageInByte = stream.toByteArray();
            Image myImage = Image.getInstance(imageInByte);
            float documentWidth = document.getPageSize().getWidth();
            float documentHeight = document.getPageSize().getHeight() - document.topMargin() - document.bottomMargin();
            myImage.scaleToFit(documentWidth, documentHeight);
            myImage.setAlignment(Image.ALIGN_CENTER | Image.MIDDLE);
            document.add(myImage);

            //hideDialog();

            document.close();
            pdfCreate = true;

            fileCreatedToast(path);


//            this.runOnUiThread(new Runnable() {
//                public void run() {
//            Intent email = new Intent(Intent.ACTION_SEND);
//            Uri uri = Uri.fromFile(new File(path));
//            email.putExtra(Intent.EXTRA_STREAM, uri);
//            email.putExtra(Intent.EXTRA_SUBJECT, "eReceipt "+date);
//            email.setType("application/pdf");
//            email.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
//            startActivity(email);
//                }
//            });
            hideProgressBar();
        }
        catch (Exception e)
        {
        //    Toast.makeText(getApplicationContext(), getText(R.string.pdf_generated_msg_error) + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void showDialog(String title, String msg)
    {
        if (progressDialog != null) {
            if (!progressDialog.isShowing()) {
                progressDialog.setTitle(title);
                progressDialog.setMessage(msg);
                progressDialog.setCancelable(false);
                progressDialog.show();
            }
        }
    }

    private void hideDialog() {

        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.cancel();
            }
        }


    }

    private void showProgressBar() {
//        this.runOnUiThread(new Runnable() {
//            public void run() {
//                progressBar.setVisibility(View.VISIBLE);
//                buttonSend.setVisibility(View.GONE);
//            }
//        });
    }

    private void hideProgressBar() {
//        this.runOnUiThread(new Runnable() {
//            public void run() {
//                progressBar.setVisibility(View.GONE);
//                buttonSend.setVisibility(View.VISIBLE);
//            }
//        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        checkifloadingComplete();
      //  buttonSend.setVisibility(View.VISIBLE);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkifloadingComplete();
       // buttonSend.setVisibility(View.VISIBLE);
    }
    void generatepdfDoc(){
        Thread t = new Thread(new Runnable() {
            public void run() {
            generatePdf();
            }
        });

        t.start();
    }

    void checkifloadingComplete(){
        if(isEmail) {
            if (transactionIdUpdated && loadComplete && isGravatarRecieved) {
                buttonSend.setVisibility(View.VISIBLE);
                progressBar.setVisibility(View.GONE);
            }
        }else if (transactionIdUpdated && loadComplete){
            buttonSend.setVisibility(View.VISIBLE);
            progressBar.setVisibility(View.GONE);
        }
    }
    void createCSV(){
        Thread t = new Thread(new Runnable() {
            public void run() {
                generateCSV();
            }
        });
        t.start();
    }
    void generateCSV(){
//        addInList("Company",tvOtherCompany.getText().toString());
//        addInList("Name",tvOtherName.getText().toString());
//        addInList("AccountId",tvOtherId.getText().toString());
//        addInList("Address",tvAddress.getText().toString());
//        addInList("Url",tvContact.getText().toString());
//        addInList("Date",TvBlockNum.getText().toString());
//        addInList("Time",tvTime.getText().toString());
//        addInList(tvUserStatus.getText().toString(),tvUserName.getText().toString());
//        addInList("AccountId",tvUserId.getText().toString());

        headers.add("TransactionId");
        headers.add("Block");
        headers.add("From");
        headers.add("FromId");
        headers.add("To");
        headers.add("ToId");
        headers.add("Date");
        headers.add("Time");
        headers.add("Memo");
        headers.add("Amount");
        headers.add("AmountSymbol");
        headers.add("Fee");
        headers.add("FeeSymbol");


        values.add(tvTrxInBlock.getText().toString());
        values.add(tvBlockNumber.getText().toString());

        if(isSent) {
            values.add(userName);
            values.add(tvUserId.getText().toString());
            values.add(otherName);
            values.add(tvOtherId.getText().toString());
        }else{
            values.add(otherName);
            values.add(tvOtherId.getText().toString());
            values.add(userName);
            values.add(tvUserId.getText().toString());
        }

        values.add(TvBlockNum.getText().toString());
        values.add(tvTime.getText().toString());
        values.add("\" "+tvMemo.getText().toString()+" \"");
        values.add(amountAmount);
        values.add(amountSymbol);
        values.add(feeAmount);
        values.add(feeSymbol);
//        headers.add("FaitAmount");
//        headers.add("FaitSymbol");

        verifyStoragePermissions(this);
        final String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getResources().getString(R.string.folder_name) + File.separator + "eReceipt-" + transactionIdClipped;
        final String pathCsv = path + ".csv";

        File file = new File(pathCsv);
        if(!file.exists()) {
            try {
                FileWriter csvWriter = new FileWriter(pathCsv);

                Boolean loop = false;
                for (String header : headers) {
                    if (loop) csvWriter.append(",");
                    csvWriter.append(header);
                    loop = true;
                }
                csvWriter.append('\n');

                loop = false;
                for (String header : values) {
                    if (loop) csvWriter.append(",");
                    csvWriter.append(header);
                    loop = true;
                }

                csvWriter.close();

                csvCreate = true;

                fileCreatedToast(path);
            } catch (Exception c) {

            }
        }
        else {
            csvCreate = true;
            fileCreatedToast(path);
        }
    }
    List<CsvObject> listCsvObjects = new ArrayList<>();
    public class CsvObject{
        String key;
        String value;
    }
    List<String> headers = new ArrayList<>();
    List<String> values = new ArrayList<>();

    void addInList(String key,String value){
        CsvObject csvObject = new CsvObject();
        csvObject.key = key;
        csvObject.value = value;
        listCsvObjects.add(csvObject);
    }
    Boolean csvCreate = false;
    Boolean pdfCreate = false;

    void fileCreatedToast(final String path){
        if(csvCreate && pdfCreate) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), getString(R.string.pdf_csv_stored_at_path)+": " + path, Toast.LENGTH_LONG).show();
                    progressBar.setVisibility(View.GONE);
                    buttonSend.setVisibility(View.VISIBLE);
                    shareFileViaEmail();
                }
            });
        }
    }

    @OnClick(R.id.buttonSend)
    public void onSendButton() {
        progressBar.setVisibility(View.VISIBLE);
        generatepdfDoc();
        createCSV();
        buttonSend.setVisibility(View.GONE);
    }

    void shareFileViaEmail(){
        String path = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + getResources().getString(R.string.folder_name) + File.separator + "eReceipt-" + transactionIdClipped;
        Uri uriPDF = Uri.fromFile(new File(path + ".pdf"));
        Uri uriCSV = Uri.fromFile(new File(path + ".csv"));
        ArrayList<Uri> files = new ArrayList<Uri>();
        files.add(uriCSV);
        files.add(uriPDF);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("plain/text/*|application/pdf");
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, files);
        startActivity(intent);
    }




}