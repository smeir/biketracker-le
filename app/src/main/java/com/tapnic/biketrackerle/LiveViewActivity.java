package com.tapnic.biketrackerle;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.tapnic.biketrackerle.events.CSCValueEvent;
import com.tapnic.biketrackerle.settings.Preferences;
import com.tapnic.biketrackerle.util.Util;


public class LiveViewActivity extends BaseActivity {


    private TextView speedTextView;
    private TextView totalDistanceTextView;
    private TextView todayDistanceTextView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_view);

        totalDistanceTextView = (TextView) findViewById(R.id.total_distance_text_view);
        todayDistanceTextView = (TextView) findViewById(R.id.today_distance_text_view);

    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CSCValueEvent event) {
        double wheelCircumference = Preferences.getWheelSize(getApplicationContext());
        CSCValue value = event.getValue();
        double distance = (value.getCumulativeWheelRevolutions() * wheelCircumference) /1000; // distance in km
        speedTextView.setText(String.format("%.2f", value.getKilometersPerSeconds()));
        totalDistanceTextView.setText(Util.formatDistance(distance));
        todayDistanceTextView.setText("ach ja");


    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_live_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
