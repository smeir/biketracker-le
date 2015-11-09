package com.tapnic.biketrackerle.service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import hugo.weaving.DebugLog;

public class BikeTrackerReceiver extends BroadcastReceiver {
    public BikeTrackerReceiver() {
    }

    @Override
    @DebugLog
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, BluetoothLeService.class));
    }
}
