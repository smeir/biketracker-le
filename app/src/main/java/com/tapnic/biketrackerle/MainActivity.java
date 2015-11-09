package com.tapnic.biketrackerle;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.tapnic.biketrackerle.db.BikeTrackerDatabase;
import com.tapnic.biketrackerle.db.WheelRevolutionPerDay;
import com.tapnic.biketrackerle.events.ActionEvent;
import com.tapnic.biketrackerle.events.ActionWithBGCharacteristic;
import com.tapnic.biketrackerle.events.CSCValueEvent;
import com.tapnic.biketrackerle.service.BluetoothLeService;
import com.tapnic.biketrackerle.settings.Preferences;
import com.tapnic.biketrackerle.settings.SettingsActivity;
import com.tapnic.biketrackerle.util.Util;

import org.eazegraph.lib.charts.BarChart;
import org.eazegraph.lib.models.BarModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;
import butterknife.OnClick;
import butterknife.OnLongClick;
import hugo.weaving.DebugLog;


public class MainActivity extends BaseActivity {

    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeService bluetoothLeService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        @DebugLog
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            updateDistanceSummary();
            updateDistanceChart();
            updateConnectionState();
        }

        @Override
        @DebugLog
        public void onServiceDisconnected(ComponentName componentName) {
            updateConnectionState();
        }
    };
    @InjectView(R.id.today_distance_text_view)
    TextView todayTextView;
    @InjectView(R.id.total_distance_text_view)
    TextView totalTextView;
    @InjectView(R.id.state_button)
    ImageButton stateButton;


    private void updateConnectionState() {
        final boolean trackerConnected = isTrackerConnected();
        if (trackerConnected) {
            stateButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_bluetooth_connected_white_24dp));
            log(getResources().getString(R.string.connected));
        } else {
            stateButton.setImageDrawable(getResources().getDrawable(R.drawable.ic_bluetooth_searching_white_24dp));
            log(getResources().getString(R.string.disconnected));
        }

    }

    private boolean isTrackerConnected() {
        return bluetoothLeService != null && bluetoothLeService.isConnected();
    }


    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ActionEvent event) {
        String action = event.getAction();
        switch (action) {
            case BluetoothLeService.ACTION_GATT_CONNECTED:
                updateConnectionState();
                Toast.makeText(this, Util.getConnectionInfo(true, this), Toast.LENGTH_LONG).show();
                break;
            case BluetoothLeService.ACTION_GATT_DISCONNECTED:
                updateConnectionState();
                break;
            case BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED:
                // Show all the supported services and characteristics on the user interface.
                break;
        }
        log(action);
    }


    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(ActionWithBGCharacteristic event) {
        log(event.getAction());
        log(event.getCharacteristic().getValue().toString());
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(CSCValueEvent event) {
        updateDistanceSummary();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Crashlytics.start(this);
        setContentView(R.layout.activity_main);
        getActionBar().setElevation(0); // disable shadow
        ButterKnife.inject(this);
        Intent intent = new Intent(this, BluetoothLeService.class);
        startService(intent);
    }


    private void initBluetoothAdapter() {
        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();

        // Checks if Bluetooth is supported on the device.
        if (bluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void log(String text) {
        Log.d("MainActivity", text);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.action_debug_log:
                startActivity(new Intent(this, DebugLogActivity.class));
                break;
            case R.id.action_about:


                startActivity(new Intent(this, AboutActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    @DebugLog
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }

    @Override
    @DebugLog
    protected void onResume() {
        super.onResume();
        initBluetoothAdapter();
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);

    }

    private void updateDistanceSummary() {
        final BikeTrackerDatabase database = BikeTrackerDatabase.getInstance(this);
        final double revolutionsForToday = database.getWheelRevolutionsForToday();
        todayTextView.setText("Today " + Util.getDistanceString(revolutionsForToday, this));
        final double totalRevolutions = database.getTotalWheelRevolutions();
        totalTextView.setText("Total " + Util.getDistanceString(totalRevolutions, this));
    }

    private void updateDistanceChart() {
        BarChart distanceChart = (BarChart) findViewById(R.id.distance_chart);
        ArrayList<WheelRevolutionPerDay> wheelRevolutionPerDays = BikeTrackerDatabase.getInstance(this).getWheelRevolutionPerDays();
        distanceChart.clearChart();
        final long todayInMillis = Util.getTodayInMillis();
        double completeDistance = 0;
        for (int i = 0; i < wheelRevolutionPerDays.size(); i++) {
            WheelRevolutionPerDay revolutionPerDay = wheelRevolutionPerDays.get(i);
            final Date date = revolutionPerDay.getDate();
            if (date.getTime() != todayInMillis) {
                long wheelRevolution = revolutionPerDay.getWheelRevolution();
                double distance = Util.getDistance(wheelRevolution, getApplicationContext());
                int oddColor = getResources().getColor(R.color.accent);
                int evenColor = getResources().getColor(R.color.accent_dark);
                int color = (i % 2) == 0 ? evenColor : oddColor;
                SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM");
                double roundedDistance = Double.valueOf(Util.formatDistance(distance)); // round
                if (i != (wheelRevolutionPerDays.size() - 1)) {
                    completeDistance += distance;
                    BarModel model = new BarModel(dateFormat.format(date), (float) roundedDistance, color);
                    distanceChart.addBar(model);
                } else { // last one, if its the biggest distance, just ignore it
                    if (distance < (completeDistance / i) * 3) { // show only if not 3 times greater then average
                        BarModel model = new BarModel(dateFormat.format(date), (float) distance, color);
                        distanceChart.addBar(model);
                    }
                }
            }

        }

        distanceChart.startAnimation();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @SuppressWarnings("UnusedDeclaration")
    @OnClick(R.id.state_button)
    protected void onStateButtonClicked() {
        String message = Util.getConnectionInfo(isTrackerConnected(), this);
        Toast.makeText(
                this, message, Toast.LENGTH_LONG
        ).show();
    }

    @OnLongClick(R.id.state_button)
    protected boolean onStateButtonLongClicked() {
        if (isTrackerConnected() && (bluetoothLeService != null)) {
            bluetoothLeService.disconnect();
        }
        return true;
    }
}


