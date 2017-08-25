package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Helper;

public class LicenseActivity extends BaseActivity {
    private final String TAG = this.getClass().getName();

    @BindView(R.id.wbLA) WebView wbLA;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lisence);
        setToolbarAndNavigationBar(getApplicationContext().getString(R.string.license), false);
        Boolean alreadyAgreed = Helper.fetchBoolianSharePref(getApplicationContext(), "license");
        String language = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_LANGUAGE);
        String country = Helper.fetchStringSharePref(this, Constants.KEY_COUNTRY);
        if (!language.equals("") && !country.equals(""))
            Helper.setLocale(language, country, getResources());
        if (alreadyAgreed) {
            agree();
            return;
        }

        String html = getString(R.string.lisence_html);
        ButterKnife.bind(this);
        wbLA.loadData(html, "text/html", "UTF-8");
    }

    @OnClick(R.id.btnAgree)
    public void agree() {
        Intent intent;
        String shareSettingsSaved = Helper.fetchObjectSharePref(getApplicationContext(), "settings_saved");
        Helper.storeBoolianSharePref(this, "license",true);
        boolean isDatabaseLoaded = Helper.fetchBoolianSharePref(getApplicationContext(), Constants.KEY_DATABASE_LOADED);
        if(!isDatabaseLoaded){
            intent = new Intent(getApplicationContext(), DatabaseLoadActivity.class);
        }else{
            if (shareSettingsSaved.equals("")) {
                intent = new Intent(getApplicationContext(), SettingActivity.class);
            }else{
                intent = new Intent(getApplicationContext(), MainActivity.class);
            }
        }
        //intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        finish();
    }

    @OnClick(R.id.btnDisAgree)
    public void disagree() {
        finish();
        System.exit(0);
    }
}