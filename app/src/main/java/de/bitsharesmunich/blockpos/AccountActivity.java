package de.bitsharesmunich.blockpos;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;

import org.bitcoinj.core.ECKey;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnFocusChange;
import butterknife.OnTextChanged;
import de.bitsharesmunich.graphenej.Address;
import de.bitsharesmunich.graphenej.BrainKey;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.api.GetAccountByName;
import de.bitsharesmunich.graphenej.api.LookupAccounts;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.AccountProperties;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.IAccountID;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.RegisterAccountResponse;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.BinHelper;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import de.bitsharesmunich.utils.SupportMethods;
import de.bitsharesmunich.utils.TinyDB;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;


public class AccountActivity extends BaseActivity implements IAccountID {
    private final String TAG = this.getClass().getName();
    public final static String BRAINKEY_FILE = "brainkeydict.txt";

    public static final int MIN_PIN_LENGTH = 6;

    private TinyDB tinyDB;

    @BindView(R.id.etAccountName)
    EditText etAccountName;

    @BindView(R.id.etPin)
    EditText etPin;

    @BindView(R.id.tvExistingAccount)
    TextView tvExistingAccount;

    @BindView(R.id.etPinConfirmation)
    EditText etPinConfirmation;

    Gson gson;
    ProgressDialog progressDialog;

    @BindView(R.id.tvErrorAccountName)
    TextView tvErrorAccountName;

    @BindView(R.id.tvPin)
    TextView tvPin;

    @BindView(R.id.tvPinConfirmation)
    TextView tvPinConfirmation;

    @BindView(R.id.tvBlockNumberHead)
    TextView tvBlockNumberHead;

    @BindView(R.id.tvAppVersion)
    TextView tvAppVersion;

    @BindView(R.id.ivSocketConnected)
    ImageView ivSocketConnected;

    Boolean hasNumber = false;
    Boolean validAccount = true;
    Boolean checkingValidation = false;

    private String mAddress;
    private String pubKey;
    private String wifPrivKey;
    private String brainPrivKey;

    // icon_setting

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        ButterKnife.bind(this);
        tinyDB = new TinyDB(this);
        setToolbarAndNavigationBar(getResources().getString(R.string.register_account_title), false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        tvAppVersion.setText("v" + BuildConfig.VERSION_NAME + getString(R.string.beta));

//        validationAccountName();
//        gson = new Gson();
//        BlockpayApplication.registerCallbackIAccountID(this);
        progressDialog = new ProgressDialog(this);
//        TransactionUIActivity.stopTransactionsLoading=true;
//        Intent intent = getIntent();
//        Bundle res = intent.getExtras();
//        if (res != null) {
//            if (res.containsKey("activity_id")) {
//                if (res.getInt("activity_id") == 919) {
//                    tvExistingAccount.setVisibility(View.GONE);
//                   // setBackButton(true);
//                }
//            }
//        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    CountDownTimer myLowerCaseTimer = new CountDownTimer(500, 500) {
        public void onTick(long millisUntilFinished) {
        }

        public void onFinish() {
            if (!etAccountName.getText().toString().equals(etAccountName.getText().toString().toLowerCase())) {
                etAccountName.setText(etAccountName.getText().toString().toLowerCase());
                etAccountName.setSelection(etAccountName.getText().toString().length());
            }
        }
    };

    @OnTextChanged(R.id.etAccountName)
    void onTextChanged(CharSequence text) {
        Log.d(TAG, "onTextChanged");
        etAccountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
        hasNumber = false;
        validAccount = true;
        if (text.length() > 5 && containsDigit(text.toString()) && text.toString().contains("-")) {
            Log.d(TAG,"Starting validation check..");
            checkingValidation = true;
            hasNumber = true;
            myLowerCaseTimer.cancel();
            myLowerCaseTimer.start();
            createBitsharesAccount(false);
        }
    }

    @OnFocusChange(R.id.etAccountName)
    void onFocusChanged(boolean hasFocus){
        if(!hasFocus) {
            if (etAccountName.getText().length() <= 5) {
                checkingValidation = false;
                Toast.makeText(getApplicationContext(), R.string.account_name_should_be_longer, Toast.LENGTH_SHORT).show();
            } else if (etAccountName.getText().length() > 5 && !containsDigit(etAccountName.getText().toString())) {
                tvErrorAccountName.setText(getString(R.string.account_name_must_include_dash_and_a_number));
                tvErrorAccountName.setVisibility(View.VISIBLE);
                hasNumber = false;
                checkingValidation = false;
            } else if (etAccountName.getText().length() > 5 && !etAccountName.getText().toString().contains("-")) {
                tvErrorAccountName.setText(getString(R.string.account_name_must_include_dash_and_a_number));
                tvErrorAccountName.setVisibility(View.VISIBLE);
                checkingValidation = false;
            }
        }
    }

    private void createBitsharesAccount(boolean focused) {
        if (!focused) {
            if (etAccountName.getText().length() > 5) {
                if (hasNumber) {
                    tvErrorAccountName.setText("");
                    tvErrorAccountName.setVisibility(View.GONE);
                }
                new WebsocketWorkerThread(new LookupAccounts(etAccountName.getText().toString(), new WitnessResponseListener() {
                    @Override
                    public void onSuccess(WitnessResponse response) {
                        WitnessResponse<List<UserAccount>> accountLookupResponse = response;
                        if (accountLookupResponse.result.size() > 0) {
                            checkAccount(accountLookupResponse.result);
                        } else {
                            hideDialog();
                            Toast.makeText(getApplicationContext(), R.string.try_again, Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(BaseResponse.Error error) {
                        hideDialog();
                        Toast.makeText(getApplicationContext(), R.string.unable_to_load_brainkey, Toast.LENGTH_SHORT).show();
                    }
                }), 0).start();
            }
            else
            {
                Toast.makeText(getApplicationContext(), R.string.account_name_should_be_longer, Toast.LENGTH_SHORT).show();
                checkingValidation = false;
            }
        }
    }

    private void validationAccountName() {
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end, Spanned dest, int dstart, int dend) {

                for (int i = start; i < end; i++) {
                    if ((dstart == 0) && (!Character.isLetter(source.charAt(0)))) {
                        return "";
                    } else if (!Character.isLetterOrDigit(source.charAt(i)) && (source.charAt(i) != '-')) {
                        return "";
                    }
                }

                return null;
            }
        };
        etAccountName.setFilters(new InputFilter[]{filter});
    }

    Boolean checkLastIndex() {
        String name = etAccountName.getText().toString();
        String lastWord = String.valueOf(name.charAt(name.length() - 1));
        return lastWord.equals("-");
    }

    Boolean checkHyphen() {
        String name = etAccountName.getText().toString();
        return name.contains("-");
    }

    /**
     * Method that generates a fresh key that will be controlling the newly created account.
     */
    private void generateKeys() {
        BufferedReader reader = null;
        String dictionary;
        try {
            reader = new BufferedReader(new InputStreamReader(getAssets().open(BRAINKEY_FILE), "UTF-8"));
            dictionary = reader.readLine();

            String brainKeySuggestion = BrainKey.suggest(dictionary);
            BrainKey brainKey = new BrainKey(brainKeySuggestion, 0);
            Address address = new Address(ECKey.fromPublicOnly(brainKey.getPrivateKey().getPubKey()));
            Log.d(TAG, "brain key: "+brainKeySuggestion);
            Log.d(TAG, "address would be: "+address.toString());
            mAddress = address.toString();
            brainPrivKey = brainKeySuggestion;
            try {
                wifPrivKey = Crypt.getInstance().encrypt_string(brainKey.getWalletImportFormat());
                createAccount();
            } catch (Exception e) {
                Toast.makeText(getApplicationContext(), R.string.error_wif , Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            Log.e(TAG, "IOException while trying to generate key. Msg: "+e.getMessage());
            Toast.makeText(getApplicationContext(), R.string.error_read_dict_file , Toast.LENGTH_SHORT).show();
            this.hideDialog();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, "IOException while trying to close BufferedReader. Msg: "+e.getMessage());
                }
            }
        }
    }

    /**
     * Method that sends the account-creation request to the faucet server.
     * Only account name and public address is sent here.
     */
    private void createAccount() {
        final String accountName = etAccountName.getText().toString();
        HashMap<String, Object> hm = new HashMap<>();
        hm.put("name", accountName);
        hm.put("owner_key", mAddress);
        hm.put("active_key", mAddress);
        hm.put("memo_key", mAddress);
        hm.put("refcode", "bitshares-munich");
        hm.put("referrer", "bitshares-munich");
        HashMap<String, HashMap> hashMap = new HashMap<>();
        hashMap.put("account", hm);

        try {
            ServiceGenerator sg = new ServiceGenerator(getString(R.string.account_create_url));
            IWebService service = sg.getService(IWebService.class);
            final Call<RegisterAccountResponse> postingService = service.getReg(hashMap);
            postingService.enqueue(new Callback<RegisterAccountResponse>() {

                @Override
                public void onResponse(Call<RegisterAccountResponse> call, Response<RegisterAccountResponse> response) {
                    Log.d(TAG,"onResponse");
                    if (response.isSuccessful()) {
                        Log.d(TAG,"success");
                        RegisterAccountResponse resp = response.body();
                        if (resp.account != null) {
                            try {
                                if(resp.account.name.equals(accountName)) {
                                    getAccountId(accountName);
                                    tvErrorAccountName.setVisibility(View.GONE);
                                }else{
                                    Log.w(TAG,"Response account name differs from 'accountName'");
                                    Log.w(TAG,"r: "+resp.account.name+", accountName: "+accountName);
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Exception. Msg: "+e.getMessage());
                                Toast.makeText(getApplicationContext(),R.string.try_again , Toast.LENGTH_SHORT).show();
                                hideDialog();
                            }
                        }else{
                            Log.w(TAG,"Faucet response had no account");
                            if(resp.error != null && resp.error.base != null && resp.error.base.length > 0){
                                String errorMessage = getResources().getString(R.string.error_with_message);
                                Toast.makeText(AccountActivity.this, String.format(errorMessage, resp.error.base[0]), Toast.LENGTH_LONG).show();
                            }else{
                                Toast.makeText(AccountActivity.this, getResources().getString(R.string.error_no_message), Toast.LENGTH_SHORT).show();
                            }
                            hideDialog();
                        }
                    }else{
                        Toast.makeText(getApplicationContext(),R.string.try_again , Toast.LENGTH_SHORT).show();
                        hideDialog();
                    }
                }

                @Override
                public void onFailure(Call<RegisterAccountResponse> call, Throwable t) {
                    Log.e(TAG, "onFailure. Msg: "+t.getMessage());
                    hideDialog();
                    for(StackTraceElement element : t.getStackTrace()){
                        Log.e(TAG, "at "+element.getClassName()+":"+element.getMethodName()+":"+element.getLineNumber());
                    }
                    Toast.makeText(getApplicationContext(),R.string.try_again , Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Exception. Msg: "+e.getMessage());
            Toast.makeText(getApplicationContext(),R.string.try_again , Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Retrieves the id for the newly created account.
     * @param accountName: Account name
     */
    private void getAccountId(final String accountName){
        new WebsocketWorkerThread(new GetAccountByName(accountName, new WitnessResponseListener() {
            @Override
            public void onSuccess(WitnessResponse response) {
                AccountProperties accountProperties = (AccountProperties) response.result;
                Log.d(TAG,"onSuccess. account id: "+accountProperties.id);
                addWallet(accountProperties.id);
            }

            @Override
            public void onError(BaseResponse.Error error) {
                Log.e(TAG,"onError. Msg: "+error.message);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        hideDialog();
                        Toast.makeText(getApplicationContext(), R.string.unable_to_find_account_id, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        })).start();
    }

    @OnClick(R.id.btnCreate)
    public void create(Button button) {
        if (checkingValidation) {
            Toast.makeText(getApplicationContext(), R.string.validation_in_progress, Toast.LENGTH_SHORT).show();
        } else if (etAccountName.getText().toString().length() == 0) {
            Toast.makeText(getApplicationContext(), R.string.kindly_create_account, Toast.LENGTH_SHORT).show();
        } else if (etAccountName.getText().toString().length() <= 5) {
            Toast.makeText(getApplicationContext(), R.string.account_name_should_be_longer, Toast.LENGTH_SHORT).show();
        } else if (!hasNumber) {
            Toast.makeText(getApplicationContext(),R.string.account_name_must_include_dash_and_a_number, Toast.LENGTH_SHORT).show();
        } else if (checkLastIndex()) {
            tvErrorAccountName.setVisibility(View.VISIBLE);
            tvErrorAccountName.setText(R.string.last_letter_cannot);
        } else if (!checkHyphen()) {
            tvErrorAccountName.setVisibility(View.VISIBLE);
            tvErrorAccountName.setText(R.string.account_name_must_include_dash_and_a_number);
        } else {
            if (etPin.getText().length() < MIN_PIN_LENGTH) {
                Toast.makeText(getApplicationContext(), R.string.please_enter_pin, Toast.LENGTH_SHORT).show();
            } else if (etPinConfirmation.getText().length() < MIN_PIN_LENGTH) {
                Toast.makeText(getApplicationContext(), R.string.please_enter_pin, Toast.LENGTH_SHORT).show();
            } else if (!etPinConfirmation.getText().toString().equals(etPin.getText().toString())) {
                Toast.makeText(getApplicationContext(), R.string.mismatch_pin, Toast.LENGTH_SHORT).show();
            } else {
                if (validAccount) {
                    if (!checkingValidation) {
                        showDialog("", "");
                        generateKeys();
                    }
                }
            }
        }
    }

    @OnClick(R.id.tvExistingAccount)
    public void existingAccount(TextView textView) {
        Intent intent = new Intent(getApplicationContext(), ExistingAccountActivity.class);
        startActivity(intent);
    }

    private void showDialog(String title, String msg) {
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

    /**
     * Checks if the proposed account name is valid.
     * @param existingAccounts
     */
    public void checkAccount(List<UserAccount> existingAccounts){
        boolean found = false;
        for(UserAccount existingAccount : existingAccounts){
            if(existingAccount.getName().equals(etAccountName.getText().toString())){
                found = true;
                break;
            }
        }
        if(found){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    validAccount = false;
                    String acName = getString(R.string.account_name_already_exist);
                    tvErrorAccountName.setText(acName);
                    tvErrorAccountName.setVisibility(View.VISIBLE);
                }
            });
        }else{
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    validAccount = true;
                    tvErrorAccountName.setVisibility(View.GONE);
                    etAccountName.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_done_24dp, 0);
                }
            });
        }
        checkingValidation = false;
    }

    void addWallet(String account_id) {
        Log.d(TAG,"addWallet. account id: "+account_id);
        AccountDetails accountDetails = new AccountDetails();
        accountDetails.pinCode = etPin.getText().toString();
        accountDetails.wif_key = wifPrivKey;
        accountDetails.account_name = etAccountName.getText().toString();
        accountDetails.pub_key = pubKey;
        accountDetails.brain_key = brainPrivKey;
        accountDetails.isSelected = true;
        accountDetails.status = "success";
        accountDetails.account_id = account_id;

        BinHelper myBinHelper = new BinHelper();
        myBinHelper.addWallet(accountDetails, getApplicationContext(), this);

        Intent intent ;

        if ( myBinHelper.numberOfWalletAccounts(getApplicationContext()) <= 1 )
        {
            intent = new Intent(getApplicationContext(), BackupBrainkeyActivity.class);
        }
        else
        {
            intent = new Intent(getApplicationContext(), SettingActivity.class);
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        hideDialog();
        startActivity(intent);
        BlockpayApplication.timeStamp();
        cleanUpHandler();
        finish();
    }

    final Handler handler = new Handler();

    public void cleanUpHandler()
    {
        if(handler!=null)
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void accountId(String string) {
        Log.d(TAG,"accountId: "+string);
        cleanUpHandler();
        //addWallet(etAccountName.getText().toString());
        String result = SupportMethods.ParseJsonObject(string, "result");
        String id_account = SupportMethods.ParseJsonObject(result, "id");
        addWallet(id_account);
    }

    public boolean containsDigit(String s) {
        if (s != null && !s.isEmpty()) {
            for (char c : s.toCharArray()) {
                if (Character.isDigit(c)) {
                    return true;
                }
            }
        }
        return false;
    }
}
