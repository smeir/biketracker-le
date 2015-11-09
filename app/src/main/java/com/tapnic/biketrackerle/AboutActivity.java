package com.tapnic.biketrackerle;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.mikepenz.aboutlibraries.Libs;
import com.mikepenz.aboutlibraries.ui.LibsActivity;
import com.tapnic.biketrackerle.util.Util;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;


public class AboutActivity extends Activity {


    @InjectView(R.id.version)
    TextView versionTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        ButterKnife.inject(this);
        String versionName = Util.getVersionName(this);
        versionTextView.setText("Version: " + versionName);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.open_source_button)
    void openSourceButtonClicked() {
        Intent i = new Intent(getApplicationContext(), LibsActivity.class);
        final String[] value = Libs.toStringArray(R.string.class.getFields());
        i.putExtra(Libs.BUNDLE_LIBS, new String[]{"Eventbus", "NineOldAndroids"});
        i.putExtra(Libs.BUNDLE_FIELDS, value);
        i.putExtra(Libs.BUNDLE_VERSION, true);
        i.putExtra(Libs.BUNDLE_LICENSE, true);
        i.putExtra(Libs.BUNDLE_TITLE, getResources().getString(R.string.open_source_licenses));
        i.putExtra(Libs.BUNDLE_THEME, R.style.AppThemeCompat);
        startActivity(i);
    }
}
