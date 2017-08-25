package de.bitsharesmunich.blockpos;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.ECKey;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.graphenej.Address;
import de.bitsharesmunich.graphenej.BrainKey;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.api.GetAccounts;
import de.bitsharesmunich.graphenej.api.GetKeyReferences;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.AccountProperties;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.interfaces.InternalMovementListener;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.utils.BinHelper;
import de.bitsharesmunich.utils.BlockpayApplication;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.TinyDB;

public class BrainkeyActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();
    @BindView(R.id.etPin)
    EditText etPin;

    @BindView(R.id.etPinConfirmation)
    EditText etPinConfirmation;

    @BindView(R.id.etBrainKey)
    EditText etBrainKey;

    @BindView(R.id.tvBlockNumberHead_brain_key_activity)
    TextView tvBlockNumberHead;

    @BindView(R.id.tvAppVersion_brain_key_activity)
    TextView tvAppVersion;

    @BindView(R.id.ivSocketConnected_brain_key_activity)
    ImageView ivSocketConnected;

    ProgressDialog progressDialog;
    TinyDB tinyDB;
    Boolean settingScreen = false;

    @BindView(R.id.tvPin)
    TextView tvPin;

    @BindView(R.id.tvPinConfirmation)
    TextView tvPinConfirmation;

    /**
     * Private index used to keep track of the current node
     * that the activity will try to connect with
     */
    private int mUriIndex;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_brainkey);
        ButterKnife.bind(this);
        setToolbarAndNavigationBar("BrainKey", false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
        setTitle(getResources().getString(R.string.app_name));

        progressDialog = new ProgressDialog(this);
        tinyDB = new TinyDB(getApplicationContext());
        tvAppVersion.setText("v" + BuildConfig.VERSION_NAME + getString(R.string.beta));
        updateBlockNumberHead();
        etBrainKey.addTextChangedListener(brainKeyWatcher);

    }

    @Override
    protected void onPause() {
        super.onPause();
        hideDialog();
    }

    private final TextWatcher brainKeyWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

        }

        @Override
        public void afterTextChanged(Editable editable) {        }
    };

    @OnClick(R.id.btnCancel)
    public void cancel(Button button) {
        this.finish();
    }

    @OnClick(R.id.btnWallet)
    public void wallet(Button button) {
        if (etBrainKey.getText().length() == 0) {
            Toast.makeText(getApplicationContext(), R.string.please_enter_brainkey, Toast.LENGTH_SHORT).show();
        } else {
            String trimmedBrainKey = etBrainKey.getText().toString().trim();
            etBrainKey.setText(trimmedBrainKey);
            if (etPin.getText().length() < Constants.MIN_PIN_LENGTH) {
                Toast.makeText(getApplicationContext(), R.string.please_enter_6_digit_pin, Toast.LENGTH_SHORT).show();
            } else if (etPinConfirmation.getText().length() < Constants.MIN_PIN_LENGTH) {
                Toast.makeText(getApplicationContext(), R.string.please_enter_6_digit_pin_confirm, Toast.LENGTH_SHORT).show();
            } else if (!etPinConfirmation.getText().toString().equals(etPin.getText().toString())) {
                Toast.makeText(getApplicationContext(), R.string.mismatch_pin, Toast.LENGTH_SHORT).show();
            } else {
                load(etPin.getText().toString());
            }
        }
    }

    void load(String pinCode) {
        String temp = etBrainKey.getText().toString();
        if (temp.contains(" ")) {
            String arr[] = temp.split(" ");
            if (arr.length >= 12 && arr.length <= 16) {
                if (checkBrainKeyExist(temp)) {
                    Toast.makeText(getApplicationContext(), R.string.account_already_exist, Toast.LENGTH_SHORT).show();
                } else {
                    showDialog("", getString(R.string.importing_your_wallet));
                    getAccountFromBrainkey(temp, pinCode);
                }
            } else {
                Toast.makeText(getApplicationContext(), R.string.please_enter_correct_brainkey, Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(getApplicationContext(), R.string.please_enter_correct_brainkey, Toast.LENGTH_SHORT).show();
        }
    }

    private boolean checkBrainKeyExist(String brainKey) {
        boolean isBrainKey = false;
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);

        for (int i = 0; i < accountDetails.size(); i++) {
            try {
                if (brainKey.equals(accountDetails.get(i).brain_key)) {
                    isBrainKey = true;
                    break;
                }
            } catch (Exception ignored) {
            }
        }
        return isBrainKey;

    }

    public void getAccountFromBrainkey(final String brainKey, final String pinCode) {
        try {
            BrainKey bKey = new BrainKey(brainKey, 0);
            Address address = new Address(ECKey.fromPublicOnly(bKey.getPrivateKey().getPubKey()));
            final String privkey = Crypt.getInstance().encrypt_string(bKey.getWalletImportFormat());
            final String pubkey = address.toString();
            Log.d(TAG,String.format("Brain key: '%s'", bKey.getBrainKey()));
            Log.d(TAG, String.format("Brainkey would generate address: %s", address.toString()));

            new WebsocketWorkerThread(new GetKeyReferences(address, new WitnessResponseListener() {
                @Override
                public void onSuccess(WitnessResponse response) {
                    final List<List<UserAccount>> resp = (List<List<UserAccount>>) response.result;
                    Log.d(TAG, "getAccountByAddress.onSuccess");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(resp.size() > 0){
                                List<UserAccount> accounts = resp.get(0);
                                if(accounts.size() > 0){
                                    for(UserAccount account : accounts) {
                                        getAccountById(account.getObjectId(), privkey, pubkey, brainKey, pinCode);
                                    }
                                }else{
                                    hideDialog();
                                    Toast.makeText(getApplicationContext(), R.string.error_invalid_account, Toast.LENGTH_SHORT).show();
                                }
                            }else{
                                hideDialog();
                                Toast.makeText(getApplicationContext(), R.string.error_invalid_account, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                }

                @Override
                public void onError(BaseResponse.Error error) {
                    hideDialog();
                    if(mUriIndex < BlockpayApplication.urlsSocketConnection.length - 1){
                        Log.e(TAG,"onError. retrying..");
                        mUriIndex++;
                        Log.d(TAG,"mUriIndex: "+mUriIndex);
                        load(etPin.getText().toString());
                    }else{
                        Log.e(TAG,"onError. giving up..");
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(), R.string.unable_to_load_brainkey, Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            }), mUriIndex).start();

        } catch (IllegalBlockSizeException | NoSuchAlgorithmException | InvalidKeyException | BadPaddingException | InvalidAlgorithmParameterException e) {
            hideDialog();
            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
        } catch (NoSuchPaddingException e) {
            hideDialog();
            Toast.makeText(getApplicationContext(), "Error", Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            hideDialog();
            Toast.makeText(getApplicationContext(), R.string.txt_no_internet_connection, Toast.LENGTH_SHORT).show();
        }

    }

    private void getAccountById(String accountId, final String privaKey, final String pubKey, final String brainkey, final String pinCode){
        Log.d(TAG, "getAccountById");
        try {
            new WebsocketWorkerThread((new GetAccounts(accountId, true, new WitnessResponseListener() {
                @Override
                public void onSuccess(WitnessResponse response) {
                    if (response.result.getClass() == ArrayList.class) {
                        List list = (List) response.result;
                        if (list.size() > 0) {
                            if (list.get(0).getClass() == AccountProperties.class) {
                                AccountProperties accountProperties = (AccountProperties) list.get(0);
                                AccountDetails accountDetails = new AccountDetails();
                                accountDetails.account_name = accountProperties.name;
                                accountDetails.account_id = accountProperties.id;
                                accountDetails.wif_key = privaKey;
                                accountDetails.pub_key = pubKey;
                                accountDetails.brain_key = brainkey;
                                accountDetails.isSelected = true;
                                accountDetails.status = "success";
                                accountDetails.pinCode = pinCode;
                                addWallet(accountDetails, brainkey, pinCode);
                            } else {
                                Toast.makeText(getApplicationContext(), "Didn't get Account properties", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            hideDialog();
                            Toast.makeText(getApplicationContext(), R.string.try_again, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        hideDialog();
                        Toast.makeText(getApplicationContext(), R.string.try_again, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(BaseResponse.Error error) {
                    Toast.makeText(getApplicationContext(), R.string.unable_to_load_brainkey, Toast.LENGTH_SHORT).show();
                }
            })), mUriIndex).start();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.txt_no_internet_connection, Toast.LENGTH_SHORT).show();
        }
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

    private void hideDialog() {
        if (progressDialog != null) {
            if (progressDialog.isShowing()) {
                progressDialog.dismiss();
            }
        }
    }

    private void updateBlockNumberHead() {
        final Handler handler = new Handler();

        final Activity myActivity = this;

        final Runnable updateTask = new Runnable() {

            @Override
            public void run() {
//                if (BlockpayApplication.isConnected()) {
//                        ivSocketConnected.setImageResource(R.drawable.icon_connecting);
//                        tvBlockNumberHead.setText(BlockpayApplication.blockHead);
//                        ivSocketConnected.clearAnimation();
//                } else {
//                    ivSocketConnected.setImageResource(R.drawable.icon_disconnecting);
//                    Animation myFadeInAnimation = AnimationUtils.loadAnimation(myActivity.getApplicationContext(), R.anim.flash);
//                    ivSocketConnected.startAnimation(myFadeInAnimation);
//                }
//                handler.postDelayed(this, 1000);
            }
        };
        handler.postDelayed(updateTask, 1000);
    }

    void addWallet(AccountDetails accountDetail, String brainKey, String pinCode) {
        BinHelper myBinHelper = new BinHelper();
        myBinHelper.addWallet(accountDetail, getApplicationContext(),this);


        Intent intent;
        if ( myBinHelper.numberOfWalletAccounts(getApplicationContext()) <= 1 )
        {
            intent = new Intent(getApplicationContext(), BackupBrainkeyActivity.class);
        }
        else
        {
            intent = new Intent(getApplicationContext(), SettingActivity.class);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        ((InternalMovementListener) this).onInternalAppMove();
        startActivity(intent);
        finish();
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
}
