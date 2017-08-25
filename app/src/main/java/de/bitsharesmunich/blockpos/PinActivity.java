package de.bitsharesmunich.blockpos;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.utils.TinyDB;

/**
 * Created by qasim on 5/27/16.
 */
public class PinActivity extends BaseActivity {


    @BindView(R.id.etOldPin)
    EditText etOldPin;

    @BindView(R.id.etPin)
    EditText etPin;

    @BindView(R.id.etPinConfirmation)
    EditText etPinConfirmation;

    @BindView(R.id.btnEdit)
    Button btnEdit;

    TinyDB tinyDB;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_pin);
        ButterKnife.bind(this);
        tinyDB = new TinyDB(getApplicationContext());
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getString(R.string.title_change_pin));

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OnClick(R.id.btnEdit)
    public void create(Button button) {
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        String pin = "";
        for (int i = 0; i < accountDetails.size(); i++) {

            if (accountDetails.get(i).isSelected) {
                pin = accountDetails.get(i).pinCode;
                accountDetails.get(i).pinCode = etPin.getText().toString();
                break;
            }

        }
        if (etOldPin.getText().length() < 6) {
            Toast.makeText(getApplicationContext(), R.string.please_enter_old_pin, Toast.LENGTH_SHORT).show();
        } else if (etPin.getText().length() < 8) {
            Toast.makeText(getApplicationContext(), R.string.please_enter_pin, Toast.LENGTH_SHORT).show();
        } else if (etPinConfirmation.getText().length() < 8) {
            Toast.makeText(getApplicationContext(), R.string.please_enter_pin_confirmation, Toast.LENGTH_SHORT).show();
        } else if (!etPinConfirmation.getText().toString().equals(etPin.getText().toString())) {
            Toast.makeText(getApplicationContext(), R.string.mismatch_pin, Toast.LENGTH_SHORT).show();
        } else if (!etOldPin.getText().toString().equals(pin)) {
            Toast.makeText(getApplicationContext(), R.string.incorrect_old_pin, Toast.LENGTH_SHORT).show();
        } else {
            tinyDB.putListObject(getString(R.string.pref_wallet_accounts), accountDetails);
            Toast.makeText(getApplicationContext(), R.string.pin_changed_successfully, Toast.LENGTH_SHORT).show();
            finish();
        }


    }
}
