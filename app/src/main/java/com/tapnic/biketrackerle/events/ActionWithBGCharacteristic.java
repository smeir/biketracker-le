package com.tapnic.biketrackerle.events;


import android.bluetooth.BluetoothGattCharacteristic;

public class ActionWithBGCharacteristic extends ActionEvent{
    public ActionWithBGCharacteristic(String action, BluetoothGattCharacteristic characteristic) {
        super(action);
        this.characteristic = characteristic;
    }

    BluetoothGattCharacteristic characteristic;

    public BluetoothGattCharacteristic getCharacteristic() {
        return characteristic;
    }

    public void setCharacteristic(BluetoothGattCharacteristic characteristic) {
        this.characteristic = characteristic;
    }
}
