package com.tapnic.biketrackerle;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.tapnic.biketrackerle.db.WheelRevolution;
import com.tapnic.biketrackerle.events.LogEvent;
import com.tapnic.biketrackerle.service.BluetoothLeService;

import java.util.ArrayList;

import butterknife.ButterKnife;
import butterknife.InjectView;
import hugo.weaving.DebugLog;

public class DebugLogActivity extends Activity {

    @InjectView(R.id.logTextView)
    TextView logTextView;
    @InjectView(R.id.logScrollView)
    ScrollView logScrollView;
    private BluetoothLeService bluetoothLeService;
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        @DebugLog
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            bluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            fillLogView();
        }

        @Override
        @DebugLog
        public void onServiceDisconnected(ComponentName componentName) {

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug_log);
        setTitle(R.string.action_debug_log);
        ButterKnife.inject(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, serviceConnection, BIND_AUTO_CREATE);
    }

    private void fillLogView() {
        /*BikeTrackerDatabase database = BikeTrackerDatabase.getInstance(this);
        List<WheelRevolution> allWheelRevolutions = database.getAllCumulativeWheelRevolutions();
        for (WheelRevolution revolution : allWheelRevolutions) {
            revolutionToLogView(revolution);
        }
        */
        ArrayList<LogEntry> logArchive = bluetoothLeService.getLogArchive();
        for (LogEntry logEntry : logArchive) {
            logToLogView(logEntry);
        }
        logScrollView.post(new Runnable() {
                               @Override
                               public void run() {
                                   logScrollView.fullScroll(View.FOCUS_DOWN);
                               }
                           }
        );
    }

    private void revolutionToLogView(WheelRevolution revolution) {
        logTextView.append(revolution.date + ": " + revolution.value + "\n");
    }

    private void logToLogView(LogEntry logEntry) {
        logTextView.append(logEntry.getTime() + ": " + logEntry.getText() + "\n");
    }

    @Override
    protected void onPause() {
        super.onPause();
        unbindService(serviceConnection);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_log_entry, menu);
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

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(LogEvent event) {
        logToLogView(event.getEntry());
    }
}
