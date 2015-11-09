package com.tapnic.biketrackerle.service;

import android.util.Log;

import java.util.TimerTask;

public class RetryScanTask extends TimerTask {
    private BluetoothLeService service;
    private final static String TAG = RetryScanTask.class.getSimpleName();

    public RetryScanTask(BluetoothLeService service) {
        this.service = service;
    }

    @Override
    public void run() {
        Log.d(TAG, "retry scan");
        service.startScan();
    }
}
