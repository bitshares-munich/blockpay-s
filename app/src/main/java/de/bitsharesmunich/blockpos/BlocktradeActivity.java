package de.bitsharesmunich.blockpos;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Currency;
import java.util.HashMap;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.OnTextChanged;
import de.bitsharesmunich.models.BlocktradesSessionResponse;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.IWebService;
import de.bitsharesmunich.utils.ServiceGenerator;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Created by mbilal on 8/5/16.
 */
public class BlocktradeActivity extends BaseActivity {
    @BindView(R.id.etBlocktradeEmail)
    EditText etBlocktradeEmail;
    @BindView(R.id.etBlocktradePassword)
    EditText etBlocktradePassword;
    @BindView(R.id.etTransactionsUnder)
    EditText etTransactionsUnder;

    @BindView(R.id.tvErrorBlocktradeEmail)
    TextView tvErrorBlocktradeEmail;
    @BindView(R.id.tvErrorBlocktradePassword)
    TextView tvErrorBlocktradePassword;
    @BindView(R.id.tvErrorTransactionUnder)
    TextView tvErrorTransactionUnder;

    @BindView(R.id.tvTransactionsUnder)
    TextView tvTransactionsUnder;

    ProgressDialog progress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blocktrade);
        //getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        ButterKnife.bind(this);

        setToolbarAndNavigationBar(getResources().getString(R.string.blocktrades_account_title), false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);

        String blocktradeEmail = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_email));
        String blocktradePassword = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_password));
        String transactionUnder = Helper.fetchStringSharePref(this,getString(R.string.pref_blocktrade_limit));
        if (!blocktradeEmail.isEmpty() & !blocktradePassword.isEmpty() & !transactionUnder.isEmpty()){
            etBlocktradeEmail.setText(blocktradeEmail);
            etBlocktradePassword.setText(blocktradePassword);
            etTransactionsUnder.setText(transactionUnder);
        }

        String currencySymbol = Helper.fetchStringSharePref(this, Constants.KEY_CURRENCY);
        if(!currencySymbol.equals("")){
            Currency currency = Currency.getInstance(currencySymbol);

            String tvTrxUnder = tvTransactionsUnder.getText().toString();
            tvTrxUnder += " " + currency.getSymbol();
            tvTransactionsUnder.setText(tvTrxUnder);

        }
        setInstructions();

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

    @OnTextChanged(R.id.etBlocktradeEmail)
    void onEmailChanged(CharSequence text) {
        tvErrorBlocktradeEmail.setVisibility(View.GONE);
    }
    @OnTextChanged(R.id.etBlocktradePassword)
    void onPasswordChanged(CharSequence text) {
        tvErrorBlocktradePassword.setVisibility(View.GONE);
    }
    @OnTextChanged(R.id.etTransactionsUnder)
    void onTransactionUnderChanged(CharSequence text) {
        tvErrorTransactionUnder.setVisibility(View.GONE);
    }

    @OnClick(R.id.btnSave)
    public void onSave(Button button) {
        if (!isValidEmail(etBlocktradeEmail.getText().toString())){
            tvErrorBlocktradeEmail.setText(getString(R.string.txt_invalid_email));
            tvErrorBlocktradeEmail.setVisibility(View.VISIBLE);
        } else if (etBlocktradePassword.getText().toString().isEmpty()){
            tvErrorBlocktradePassword.setText(R.string.enter_valid_password);
            tvErrorBlocktradePassword.setVisibility(View.VISIBLE);
        } else if (etTransactionsUnder.getText().toString().isEmpty()){
            tvErrorTransactionUnder.setText(R.string.enter_valid_amount);
            tvErrorTransactionUnder.setVisibility(View.VISIBLE);
        } else{ //valid
            validateAccount(this);
        }
    }
    private void validateAccount(final Context context) {
        progress = ProgressDialog.show(context, getString(R.string.txt_please_wait),
                getString(R.string.txt_loading) + "...", true);
        HashMap hm = new HashMap();
        hm.put("email", etBlocktradeEmail.getText().toString());
        hm.put("password", etBlocktradePassword.getText().toString());

        ServiceGenerator sg = new ServiceGenerator(getApplicationContext().getString(R.string.blocktrade_server_url));
        IWebService service = sg.getService(IWebService.class);
        final Call<BlocktradesSessionResponse> postingService = service.getBlocktradeToken(hm);
        postingService.enqueue(new Callback<BlocktradesSessionResponse>() {
            @Override
            public void onResponse(Call<BlocktradesSessionResponse> call, Response<BlocktradesSessionResponse> response) {
                if (response.isSuccessful()) {
                    BlocktradesSessionResponse resp = response.body();
                    if (!resp.token.isEmpty()){
                        Helper.storeStringSharePref(context,getString(R.string.pref_blocktrade_email),etBlocktradeEmail.getText().toString());
                        Helper.storeStringSharePref(context,getString(R.string.pref_blocktrade_password),etBlocktradePassword.getText().toString());
                        Helper.storeStringSharePref(context,getString(R.string.pref_blocktrade_limit),etTransactionsUnder.getText().toString());
                        finish();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), R.string.invalid_email_password , Toast.LENGTH_SHORT).show();
                }
                if (progress.isShowing())
                {
                    progress.dismiss();
                }
            }

            @Override
            public void onFailure(Call<BlocktradesSessionResponse> call, Throwable t) {
                Toast.makeText(getApplicationContext(), R.string.txt_no_internet_connection , Toast.LENGTH_SHORT).show();
                if (progress.isShowing())
                {
                    progress.dismiss();
                }
            }
        });
    }
    private Boolean isValidEmail(String email) {
        String emailPattern = "[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+";

        if (email.matches(emailPattern)) {
            return true;
        } else {
            return false;
        }
    }

    void setInstructions(){

        String instructionsB = getString(R.string.blocktrades_instructions_b);

        String instructionsC = getString(R.string.blocktrades_instructions_c);

        String instructionsD = getString(R.string.blocktrades_instructions_d);

        String instructionsE = getString(R.string.blocktrades_instructions_e);

        String instructionsF = getString(R.string.blocktrades_instructions_f);

        String instructionsG = "https://steemit.com/crypto-news/@blocktrades/how-to-buy-crypto-in-20-seconds-with-your-bitcoin-litecoin-etc";

        TextView tv = (TextView) findViewById(R.id.tvInstructions);
        String instructions = "\n" + instructionsB + "\n" + instructionsC + "\n" + instructionsD + "\n" + instructionsE + "\n" + instructionsF + "\n" + instructionsG ;
        tv.setText(instructions);

        CharSequence text = tv.getText();
        if (text instanceof Spannable) {
            int end = text.length();
            Spannable sp = (Spannable) text;
            URLSpan urls[] = sp.getSpans(0, end, URLSpan.class);
            SpannableStringBuilder style = new SpannableStringBuilder(text);
            style.clearSpans();
            for (URLSpan urlSpan : urls) {
                MyURLSpan myURLSpan = new MyURLSpan(urlSpan.getURL());
                style.setSpan(myURLSpan, sp.getSpanStart(urlSpan),
                        sp.getSpanEnd(urlSpan),
                        Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
            tv.setText(style);
        }

    }

    private class MyURLSpan extends ClickableSpan {

        private String url;

        public MyURLSpan(String url) {
            this.url = url;
        }
        @Override
        public void updateDrawState(TextPaint ds) {
            super.updateDrawState(ds);
            ds.setColor(Color.BLACK);
        }
        @Override
        public void onClick(View arg0) {
            if(!url.contains("TRADE.USDLABOR")) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                startActivity(browserIntent);
            }
        }

    }
}
