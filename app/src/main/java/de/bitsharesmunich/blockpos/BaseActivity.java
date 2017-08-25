package de.bitsharesmunich.blockpos;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.ImageView;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;

import java.util.ArrayList;

import de.bitsharesmunich.graphenej.UserAccount;
import de.bitsharesmunich.models.AccountDetails;
import de.bitsharesmunich.models.Gravatar;
import de.bitsharesmunich.models.MerchantEmail;
import de.bitsharesmunich.utils.Constants;
import de.bitsharesmunich.utils.Helper;
import de.bitsharesmunich.utils.TinyDB;
import io.fabric.sdk.android.Fabric;

/**
 * Created by qasim on 4/4/16.
 */
public class BaseActivity extends LockableActivity
        implements NavigationView.OnNavigationItemSelectedListener {
    private final String TAG = "BaseActivity";
    TextView tvAccountName, tvAccountId, toolbar_title, footer;
    WebView wb;
    ImageView ivGravatar;
    Boolean isReverse = false;
    ImageView ivSocketConnected;

    protected Gson gson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.USE_CRASHLYTICS) {
            Fabric.with(this, new Crashlytics());
            Log.d(TAG, "Using crashlytics");
        } else {
            Log.d(TAG, "Not using crashlytics");
        }

        Helper.getCountryCodes();
        String language = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_LANGUAGE);
        String countryCode = Helper.fetchStringSharePref(getApplicationContext(), Constants.KEY_COUNTRY);

        Log.d(TAG, "language......: "+language);
        Log.d(TAG, "country code..: "+countryCode);

        // If no country has been specified, just use the one from the current locale
        if (countryCode.equals("")) {
            countryCode = getResources().getConfiguration().locale.getCountry();
        }

        // If no language has been specified, just use the one from the current locale
        if (language.equals("")) {
            language = getResources().getConfiguration().locale.getLanguage();
        }

        Helper.setLocale(language, countryCode, getResources());

        SensorManager sensorManager = (SensorManager) this.getSystemService(Context.SENSOR_SERVICE);
        sensorManager.registerListener(new SensorEventListener() {
            int orientation = -1;

            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.values[1] < 6.5 && event.values[1] > -6.5) {
                    if (orientation != 1) {
                        if (isReverse) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        } else {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        }
                    }

                    orientation = 1;
                } else {

                    if (orientation != 0) {
                        if (event.values[1] < -6.5) {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                            isReverse = true;
                        } else {
                            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                            isReverse = false;
                        }
                    }
                    orientation = 0;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
                // TODO Auto-generated method stub

            }
        }, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

        gson = new Gson();
    }

    protected void setToolbarAndNavigationBar(String heading, Boolean navigationBar) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar_title = (TextView) toolbar.findViewById(R.id.toolbar_title);
        toolbar_title.setText(heading);

        setSupportActionBar(toolbar);
        if (navigationBar) {
            NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
            View header = navigationView.getHeaderView(0);
            wb = (WebView) header.findViewById(R.id.wb);
            ivGravatar = (ImageView) header.findViewById(R.id.ivGravatarDrawer);
            tvAccountName = (TextView) header.findViewById(R.id.tvAccountName);
            tvAccountId = (TextView) header.findViewById(R.id.tvAccountId);
            footer = (TextView) this.findViewById(R.id.footer_item_2);
            footer.setText("     " + getResources().getString(R.string.app_name) + " v" + BuildConfig.VERSION_NAME);
            ivSocketConnected = (ImageView) findViewById(R.id.ivSocketIcon);
            navigationView.setNavigationItemSelectedListener(this);
            settingScreen();
            resetGravatarImage();
        }
    }


    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        Intent intent = null;
        item.setCheckable(false);

        if (id == R.id.nav_camara) {
            intent = new Intent(getApplicationContext(), SettingActivity.class);
            intent.putExtra(Constants.KEY_DISPLAY_BACK, true);
        } else if (id == R.id.transactions) {
            intent = new Intent(getApplicationContext(), TransactionListActivity.class);
        }

        if (intent != null) {
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void settingScreen() {
        TinyDB tinyDB = new TinyDB(getApplicationContext());
        UserAccount userAccount = null;
        ArrayList<AccountDetails> accountDetails = tinyDB.getListObject(getString(R.string.pref_wallet_accounts), AccountDetails.class);
        for (int i = 0; i < accountDetails.size(); i++) {
            if (accountDetails.get(i).isSelected) {
                userAccount = new UserAccount(accountDetails.get(i).account_id, accountDetails.get(i).account_name);
            }
        }
        if(userAccount != null){
            bitShareAccountName(180, Helper.md5(userAccount.getName()));
            tvAccountName.setText(userAccount.getName());
            tvAccountId.setText("(" + userAccount.getObjectId() + ")");
        }
    }

    private void bitShareAccountName(int size, String encryptText) {
        String htmlShareAccountName = "<html><head><style>body,html { margin:0; padding:0; text-align:center;}</style><meta name=viewport content=width=" + size + ",user-scalable=no/></head><body><canvas width=" + size + " height=" + size + " data-jdenticon-hash=" + encryptText + "></canvas><script src=https://cdn.jsdelivr.net/jdenticon/1.3.2/jdenticon.min.js async></script></body></html>";
        WebSettings webSettings = wb.getSettings();
        webSettings.setJavaScriptEnabled(true);
        wb.loadData(htmlShareAccountName, "text/html", "UTF-8");
    }

    public void resetGravatarImage() {
        if (Gravatar.gravatarLogo() != null) {
            ivGravatar.setImageBitmap(Gravatar.gravatarLogo());
            ivGravatar.setVisibility(View.VISIBLE);
            wb.setVisibility(View.GONE);
        } else {
            wb.setVisibility(View.VISIBLE);
            ivGravatar.setVisibility(View.GONE);
        }
    }

    public void resetGravatar() {
        String email = MerchantEmail.getMerchantEmail(getApplicationContext());
        Gravatar.getInstance().fetch(email);
    }
}
