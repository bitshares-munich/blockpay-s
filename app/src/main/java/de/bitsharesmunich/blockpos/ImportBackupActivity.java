package de.bitsharesmunich.blockpos;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.bitcoinj.core.ECKey;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ar.com.daidalos.afiledialog.FileChooserDialog;
import ar.com.daidalos.afiledialog.FileChooserLabels;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.graphenej.Address;
import de.bitsharesmunich.graphenej.BrainKey;
import de.bitsharesmunich.graphenej.FileBin;
import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.graphenej.api.GetAccounts;
import de.bitsharesmunich.graphenej.api.GetKeyReferences;
import de.bitsharesmunich.graphenej.interfaces.WitnessResponseListener;
import de.bitsharesmunich.graphenej.models.AccountProperties;
import de.bitsharesmunich.graphenej.models.BaseResponse;
import de.bitsharesmunich.graphenej.models.WitnessResponse;
import de.bitsharesmunich.graphenej.models.backup.WalletBackup;
import de.bitsharesmunich.interfaces.InternalMovementListener;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.utils.BinHelper;
import de.bitsharesmunich.utils.Crypt;
import de.bitsharesmunich.utils.PermissionManager;

public class ImportBackupActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();

    @BindView(R.id.tvFileChoosenBin)
    TextView tvFileChoosenBin;

    @BindView(R.id.etExistingPassword)
    EditText etExistingPassword;

    @BindView(R.id.etPinBin)
    EditText etPinBin;

    ArrayList<Integer> bytes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_import_backup);
        ButterKnife.bind(this);

        setTitle(getResources().getString(R.string.app_name));
        setToolbarAndNavigationBar("Account", false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        progressDialog = new ProgressDialog(this);
    }

    @OnClick(R.id.btnChooseFile)
    public void onChooseFile(){
        chooseBinFile();
    }

    @OnClick(R.id.btnWalletBin)
    public void onClickbtnWalletBin() {
        String currentPassword = etExistingPassword.getText().toString();
        String pinText = etPinBin.getText().toString();

        if(currentPassword.length() == 0){
            Toast.makeText(this, getResources().getString(R.string.missing_existing_password), Toast.LENGTH_SHORT).show();
        }else if(pinText.length() == 0){
            Toast.makeText(this, getResources().getString(R.string.pin_number_request), Toast.LENGTH_SHORT).show();
        }else if(pinText.length() < 6){
            Toast.makeText(this, getResources().getString(R.string.pin_number_warning), Toast.LENGTH_SHORT).show();
        }else{
            showDialog("",getString(R.string.importing_keys_from_bin_file));
            recoverAccountFromBackup(currentPassword, pinText);
        }
    }

    FileChooserDialog dialog;

    private void chooseBinFile() {
        if (dialog == null) {
            dialog = new FileChooserDialog(this);
            dialog.addListener(this.onFileSelectedListener);
            dialog.setFolderMode(false);
            dialog.setCanCreateFiles(false);
            dialog.setShowCancelButton(true);
            dialog.setShowOnlySelectable(false);
            dialog.setFilter(".*bin");


            // Activate the confirmation dialogs.
            dialog.setShowConfirmation(true, true);
            // Define the labels.
            FileChooserLabels labels = new FileChooserLabels();
            labels.createFileDialogAcceptButton = getApplicationContext().getString(R.string.ok);
            labels.createFileDialogCancelButton = getApplicationContext().getString(R.string.cancel);
            labels.labelSelectButton = getApplicationContext().getString(R.string.select);
            labels.messageConfirmSelection = getApplicationContext().getString(R.string.are_you_sure);
            labels.labelConfirmYesButton = getApplicationContext().getString(R.string.txt_yes);
            labels.labelConfirmNoButton = getApplicationContext().getString(R.string.txt_no);
            labels.labelCancelButton = getApplicationContext().getString(R.string.cancel);
            dialog.setLabels(labels);
        }

        // Show the dialog.
        dialog.show();

    }

    private FileChooserDialog.OnFileSelectedListener onFileSelectedListener = new FileChooserDialog.OnFileSelectedListener() {
        public void onFileSelected(Dialog source, File file) {
            source.dismiss();
            onSuccess(file.getAbsolutePath());
            tvFileChoosenBin.setText(file.getName());
        }

        public void onFileSelected(Dialog source, File folder, String name) {
            source.hide();
        }
    };

    void onSuccess(String filepath){
        PermissionManager manager = new PermissionManager();
        manager.verifyStoragePermissions(this);

        bytes = new BinHelper().getBytesFromBinFile(filepath);
    }

    ProgressDialog progressDialog;
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
                progressDialog.cancel();
            }
        }

    }

    public void recoverAccountFromBackup(String existingPassword, final String pin) {
        try {
            byte[] byteArray = new byte[bytes.size()];
            for(int i = 0 ; i < bytes.size();i++){
                byteArray[i] = bytes.get(i).byteValue();
            }
            String brainKey;
            brainKey = FileBin.getBrainkeyFromByte(byteArray, existingPassword);
            if(brainKey == null){
                WalletBackup walletBackup = FileBin.deserializeWalletBackup(byteArray, existingPassword);

                if(walletBackup.getKeyCount() > 0){
                    brainKey = walletBackup.getWallet(0).decryptBrainKey(existingPassword);
                }
            }

            BrainKey bKey = new BrainKey(brainKey, 0);
            Address address = new Address(ECKey.fromPublicOnly(bKey.getPrivateKey().getPubKey()));
            final String privkey = Crypt.getInstance().encrypt_string(bKey.getWalletImportFormat());
            final String pubkey = address.toString();
            Log.d(TAG, "Got brain key: "+brainKey);
            Log.d(TAG, "Looking up keys for address: "+address.toString());
            final String finalBrainKey = brainKey;
            new WebsocketWorkerThread(new GetKeyReferences(address, new WitnessResponseListener() {
                @Override
                public void onSuccess(final WitnessResponse response) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.d(TAG, "getAccountByAddress.onSuccess");
                            List<List<UserAccount>> resp = (List<List<UserAccount>>) response.result;
                            if(resp.size() > 0){
                                List<UserAccount> accounts = resp.get(0);
                                if(accounts.size() == 0){
                                    Log.w(TAG, "Found no account using the key given by backup.");
                                    Toast.makeText(ImportBackupActivity.this, getResources().getString(R.string.backup_no_keys_found_error), Toast.LENGTH_LONG).show();
                                }else{
                                    for(UserAccount account : accounts){
                                        getAccountById(account.getObjectId(), privkey, pubkey, finalBrainKey, pin);
                                    }
                                }
                            }else{
                                Log.w(TAG, "Invalid address");
                                Toast.makeText(getApplicationContext(), R.string.error_invalid_account, Toast.LENGTH_SHORT).show();
                            }
                            hideDialog();
                        }
                    });
                }

                @Override
                public void onError(final BaseResponse.Error error) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Log.e(TAG,"onError. Msg: "+error.message);
                            hideDialog();
                            Toast.makeText(getApplicationContext(), R.string.unable_to_load_brainkey, Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }), 0).start();
        } catch (Exception e) {
            hideDialog();
            Toast.makeText(this, getString(R.string.please_make_sure_your_bin_file), Toast.LENGTH_LONG).show();
        }
    }

    private void getAccountById(final String accountId, final String privaKey, final String pubKey, final String brainkey, final String pinCode){
        Log.d(TAG, "getAccountById");
        try {
            new WebsocketWorkerThread((new GetAccounts(accountId, true, new WitnessResponseListener() {
                @Override
                public void onSuccess(WitnessResponse response) {
                    hideDialog();
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
                                accountDetails.isSelected = true;
                                accountDetails.status = "success";
                                accountDetails.brain_key = brainkey;
                                accountDetails.pinCode = pinCode;

                                BinHelper myBinHelper = new BinHelper();
                                myBinHelper.addWallet(accountDetails, getApplicationContext(), ImportBackupActivity.this);
                                Intent intent;
                                int numberOfAccounts = myBinHelper.numberOfWalletAccounts(getApplicationContext());
                                if ( myBinHelper.numberOfWalletAccounts(getApplicationContext()) <= 1 ) {
                                    intent = new Intent(getApplicationContext(), BackupBrainkeyActivity.class);
                                } else {
                                    intent = new Intent(getApplicationContext(), SettingActivity.class);
                                }

                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                                ((InternalMovementListener) ImportBackupActivity.this).onInternalAppMove();
                                startActivity(intent);
                                finish();
                            } else {
                                Toast.makeText(getApplicationContext(), "Didn't get Account properties", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.try_again, Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.try_again, Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onError(BaseResponse.Error error) {
                    Toast.makeText(getApplicationContext(), R.string.unable_to_load_brainkey, Toast.LENGTH_SHORT).show();
                }
            })), 0).start();
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.txt_no_internet_connection, Toast.LENGTH_SHORT).show();
        }


    }

    @OnClick(R.id.btnCancelBin)
    public void OnCancel(Button button){
        this.finish();
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
