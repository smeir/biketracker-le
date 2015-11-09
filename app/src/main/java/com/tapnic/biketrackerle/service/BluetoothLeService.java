/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tapnic.biketrackerle.service;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;

import com.tapnic.biketrackerle.CSCValue;
import com.tapnic.biketrackerle.LogEntry;
import com.tapnic.biketrackerle.MainActivity;
import com.tapnic.biketrackerle.R;
import com.tapnic.biketrackerle.db.BikeTrackerDatabase;
import com.tapnic.biketrackerle.events.ActionEvent;
import com.tapnic.biketrackerle.events.ActionWithBGCharacteristic;
import com.tapnic.biketrackerle.events.CSCValueEvent;
import com.tapnic.biketrackerle.events.DataImportedEvent;
import com.tapnic.biketrackerle.events.LogEvent;
import com.tapnic.biketrackerle.events.SettingsChangedEvent;
import com.tapnic.biketrackerle.settings.Preferences;
import com.tapnic.biketrackerle.util.Util;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import de.greenrobot.event.EventBus;
import hugo.weaving.DebugLog;

/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 * <p/>
 * csc characteristic calculation from
 * https://github.com/deadfalkon/android-simple-bike-computer
 * from android.bluetooth.BluetoothGattCallback.onCharacteristicChanged()
 */
public class BluetoothLeService extends Service {
    public static final int FIVE_MINUTES_IN_MILLISECONDS = 300000;
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private static final int NOTIFICATION_ID = 1;

    private BluetoothManager bluetoothManager;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothGatt bluetoothGatt;
    private STATE connectionState = STATE.DISCONNECTED;
    private ArrayList<LogEntry> logArchive = new ArrayList<>();
    public static final int NOT_SET = Integer.MIN_VALUE;
    int lastWheelTime = NOT_SET;
    long lastWheelCount = NOT_SET;



    enum STATE {
        DISCONNECTED,
        CONNECTING,
        CONNECTED,
    }

    public final static String ACTION_GATT_CONNECTED =
            "bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "bluetooth.le.ACTION_DATA_AVAILABLE";

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback();
    private BluetoothDevice currentDevice;
    private BikeTrackerDatabase database;

    private void broadcastUpdate(final String action) {
        ActionEvent actionEvent = new ActionEvent(action);
        EventBus.getDefault().post(actionEvent);
    }

    private void handleCharacteristicChanged(final String action,
                                             final BluetoothGattCharacteristic characteristic) {
        final byte[] value = characteristic.getValue();
        if (BLEConstants.CSC_CHARACTERISTIC_UUID.equals(characteristic.getUuid())) { // implement parsing the csc value
            parseCSCCharacteristicValue(value);
        } else {
            // For all other profiles, writes the data formatted in HEX.
            if (value != null && value.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(value.length);
                for (byte byteChar : value)
                    stringBuilder.append(String.format("%02X ", byteChar));
                log("non csc characteristic: " + stringBuilder.toString());
            }
        }
        ActionWithBGCharacteristic event = new ActionWithBGCharacteristic(action, characteristic);
        EventBus.getDefault().post(event);
    }


    private void parseCSCCharacteristicValue(byte[] value) {
        CSCValue cscValue = new CSCValue();
        ByteBuffer bb = ByteBuffer.wrap(value);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        // todo handle value[0], FLAGS
        byte flags = bb.get();
        int cumulativeWheelRevolutions = bb.getInt(); // byte 1-4 todo convert to unsigned int <>long
        int lastWheelTimeEvent = bb.getChar(); // byte 5-6
        int cumulativeCrankRevolutions = bb.getChar(); // byte 6-7
        int lastCrankTimeEvent = bb.getChar(); // byte 8-9

        Log.d(TAG, "parse CSC value " + cumulativeWheelRevolutions + ":" + lastWheelTimeEvent + ":" + cumulativeCrankRevolutions + ":" + lastCrankTimeEvent);
        cscValue.setCumulativeWheelRevolutions(cumulativeWheelRevolutions);

        if (lastWheelTime == NOT_SET) {
            lastWheelTime = lastWheelTimeEvent;
        }
        if (lastWheelCount == NOT_SET) {
            lastWheelCount = cumulativeWheelRevolutions;
        }
        // Circumference
        double wheelSize = Preferences.getWheelSize(getApplicationContext());
        double distance = cumulativeWheelRevolutions * wheelSize;


        long numberOfWheelRevolutions = cumulativeWheelRevolutions - lastWheelCount;

        // update database
        database.updateCumulativeWheelRevolutionForToday(cumulativeWheelRevolutions);

        if (lastWheelTime != lastWheelTimeEvent && numberOfWheelRevolutions > 0) {
            int diff = lastWheelTimeEvent - lastWheelTime;
            double timeDiff = diff / 1024.0; // convert to seconds
            double metersPerSeconds = (wheelSize * numberOfWheelRevolutions) / timeDiff;

            lastWheelCount = cumulativeWheelRevolutions;
            lastWheelTime = lastWheelTimeEvent;
            cscValue.setMetersPerSeconds(metersPerSeconds);
            EventBus.getDefault().post(new CSCValueEvent(cscValue));
            updateNotificationState();
        }


    }


    @Override
    @DebugLog
    public void onCreate() {
        super.onCreate();
        database = BikeTrackerDatabase.getInstance(this);
        startScan();
        updateNotificationState();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    @DebugLog
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    @DebugLog
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    @DebugLog
    public boolean startScan() {
        if (!initBlueToothAdapter()) return false;

        BluetoothLeScanner scanner = bluetoothAdapter.getBluetoothLeScanner();
        if (scanner == null) {
            log("Unable to obtain a BluetoothLeScanner.");
            retryScan();
            return false;
        }
        log("BluetoothLeScanner ready");

        ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER).build();
        List<ScanFilter> filters = new ArrayList<>();
        ParcelUuid serviceUuid = ParcelUuid.fromString(BLEConstants.CSC_SERVICE);
        ScanFilter serviceFilter = new ScanFilter.Builder().setServiceUuid(serviceUuid).build();
        filters.add(serviceFilter);
        log("Start scan for CSC Service (UUID 0x1816)");
        scanner.startScan(filters, settings, new ScanCallback() {
            @Override
            @DebugLog
            public void onScanResult(int callbackType, ScanResult result) {
                super.onScanResult(callbackType, result);
                BluetoothDevice device = result.getDevice();
                if (connectionState == STATE.DISCONNECTED) { // ok, we aren't already connected
                    String address = device.getAddress();
                    String trackedDeviceAddress = Preferences.getTrackedDeviceAddress(BluetoothLeService.this);
                    if (TextUtils.isEmpty(trackedDeviceAddress)) {
                        log("new device found: " + device);
                        Preferences.saveTrackedDeviceAddress(BluetoothLeService.this, address);
                        Preferences.saveTrackedDeviceName(BluetoothLeService.this, device.getName());
                        log("saved as default");
                        trackedDeviceAddress = address;
                    }
                    if (trackedDeviceAddress.equals(address)) {
                        log("tracked device found: " + device);
                        connect(address);
                    }
                }
            }

            @Override
            @DebugLog
            public void onScanFailed(int errorCode) {
                super.onScanFailed(errorCode);
                log("scan failed: " + errorCode);
            }
        });
        return true;
    }

    public boolean initBlueToothAdapter() {
        if (bluetoothManager == null) {
            bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager == null) {
                log("Unable to initialize BluetoothManager.");
                retryScan();
                return false;
            }
        }
        log("BluetoothManager ready");
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            log("Unable to obtain a BluetoothAdapter.");
            retryScan();
            return false;
        }
        log("BluetoothAdapter ready");
        return true;
    }

    private void retryScan() {
        final Timer timer = new Timer();
        final RetryScanTask retryScanTask = new RetryScanTask(this);
        timer.schedule(retryScanTask, FIVE_MINUTES_IN_MILLISECONDS);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     * @return Return true if the connection is initiated successfully. The connection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public boolean connect(final String address) {
        if (bluetoothAdapter == null || address == null) {
            log("BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        BluetoothDevice device;
        try {
            device = bluetoothAdapter.getRemoteDevice(address);
        } catch (IllegalArgumentException ex) {
            log("Device not found.  Unable to connect.");
            return false;
        }


        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        log("connecting to GATT server");
        bluetoothGatt = device.connectGatt(this, false, gattCallback);
        connectionState = STATE.CONNECTING;
        return true;
    }


    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    @DebugLog
    public void disconnect() {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            log("BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.disconnect();
    }

    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    @DebugLog
    public void close() {
        if (bluetoothGatt == null) {
            return;
        }
        bluetoothGatt.close();
        bluetoothGatt = null;
    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (bluetoothAdapter == null || bluetoothGatt == null) {
            log("BluetoothAdapter not initialized");
            return;
        }
        bluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (bluetoothGatt == null) return null;

        return bluetoothGatt.getServices();
    }

    private class BluetoothGattCallback extends android.bluetooth.BluetoothGattCallback {
        @Override
        @DebugLog
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String action;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                action = ACTION_GATT_CONNECTED;
                connectionState = STATE.CONNECTED;
                broadcastUpdate(action);
                updateNotificationState();
                log("Connected to GATT server.");
                currentDevice = bluetoothGatt.getDevice();
                // Attempts to discover services after successful connection.
                log("Attempting to start service discovery:");
                bluetoothGatt.discoverServices();

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                action = ACTION_GATT_DISCONNECTED;
                connectionState = STATE.DISCONNECTED;
                currentDevice = null;
                updateNotificationState();
                log("Disconnected from GATT server.");
                broadcastUpdate(action);
            }
        }

        @Override
        @DebugLog
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                log("successfully service discovered");
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
                BluetoothGattService gattService = gatt.getService(BLEConstants.CSC_SERVICE_UUID);
                BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(BLEConstants.CSC_CHARACTERISTIC_UUID);
                boolean notificationResult = gatt.setCharacteristicNotification(characteristic, true);
                log("enable notification " + (notificationResult ? "successfully" : "unsuccessfully"));

                BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BLEConstants.CLIENT_CHARACTERISTIC_CONFIG_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                boolean writeDescriptorResult = gatt.writeDescriptor(descriptor);
                log("wrote Descriptor for updates " + (writeDescriptorResult ? "successfully" : "unsuccessfully"));
            } else {
                log("error: service discover status not successful: " + status);
            }
        }

        @Override
        @DebugLog
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                handleCharacteristicChanged(ACTION_DATA_AVAILABLE, characteristic);
            }
        }

        @Override
        @DebugLog
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            handleCharacteristicChanged(ACTION_DATA_AVAILABLE, characteristic);
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SettingsChangedEvent ignored) {
        updateNotificationState();
    }
    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(DataImportedEvent ignored) {
        updateNotificationState();
    }

    private void updateNotificationState() {
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Notification.Builder notificationBuilder = new Notification.Builder(this);
        long revolutionsForToday = database.getWheelRevolutionsForToday();
        String distanceStr = Util.getDistanceString(revolutionsForToday, getApplicationContext());
        String title;

        title = Util.getConnectionInfo(isConnected(), this);
        PendingIntent activity = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);
        notificationBuilder.setPriority(Notification.PRIORITY_MIN)
                .setShowWhen(false)
                .setContentTitle(title)
                .setContentIntent(activity)
                .setSmallIcon(R.drawable.ic_notification)
                .setOngoing(true)
                .setContentText("Distance today: " + distanceStr);

        nm.notify(NOTIFICATION_ID, notificationBuilder.build());
    }

    public ArrayList<LogEntry> getLogArchive() {
        return logArchive;
    }

    /**
     *
     * @return true if connected, if disconnected or connecting than false
     */
    public boolean isConnected() {
        return connectionState == STATE.CONNECTED;
    }

    private void log(String text) {
        Log.d(TAG, text);
        DateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
        Date date = new Date();
        LogEntry logEntry = new LogEntry(dateFormat.format(date), text);
        logArchive.add(logEntry);
        EventBus.getDefault().post(new LogEvent(logEntry));
    }

}
