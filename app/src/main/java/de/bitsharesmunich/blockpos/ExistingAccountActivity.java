package de.bitsharesmunich.blockpos;

import android.content.Intent;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.MenuItem;
import android.widget.Button;

import butterknife.ButterKnife;
import butterknife.OnClick;

public class ExistingAccountActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_existing_account);
        ButterKnife.bind(this);
        setTitle(getResources().getString(R.string.app_name));
        setToolbarAndNavigationBar(getResources().getString(R.string.import_existing_account), false);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        final Drawable upArrow = ContextCompat.getDrawable(getApplicationContext(), R.drawable.abc_ic_ab_back_mtrl_am_alpha);
        upArrow.setColorFilter(ContextCompat.getColor(getApplicationContext(), R.color.white), PorterDuff.Mode.SRC_ATOP);
        getSupportActionBar().setHomeAsUpIndicator(upArrow);
    }

    @OnClick(R.id.tvImportBrainKey)
    public void importBrainKey(Button button) {
        Intent intent = new Intent(getApplicationContext(), BrainkeyActivity.class);
        this.onInternalAppMove();
        startActivity(intent);
    }

    @OnClick(R.id.tvBackup)
    public void importBackup(Button button) {
        Intent intent = new Intent(getApplicationContext(), ImportBackupActivity.class);
        this.onInternalAppMove();
        startActivity(intent);
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
