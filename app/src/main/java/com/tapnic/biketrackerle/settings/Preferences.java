package com.tapnic.biketrackerle.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.tapnic.biketrackerle.util.Util;

import java.util.Locale;


public class Preferences {
    private static final String PREF_WHEEL_SIZE = "wheelSize";
    private static final String PREF_WHEEL_UNIT = "wheelUnit";
    private static final String PREF_TRACK_DEVICE_ADDRESS = "trackDeviceAddress";
    private static final String PREF_TRACK_DEVICE_NAME = "trackDeviceName";
    public static final String UNIT_FOOT = "ft";
    public static final String UNIT_METER = "m";
    final static float DEFAULT_WHEEL_SIZE = Locale.getDefault() == Locale.US ? 7.12f : 2.16f;
    final static String DEFAULT_WHEEL_UNIT = Locale.getDefault() == Locale.US ? UNIT_FOOT : UNIT_METER;


    public static float getWheelSizeInMeter(Context context) {
        float wheelSize = getWheelSize(context);
        if (isUnitImperial(context)) {
            wheelSize = Util.footToMeter(wheelSize);
        }
        return wheelSize;
    }

    public static float getWheelSize(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getFloat(Preferences.PREF_WHEEL_SIZE, DEFAULT_WHEEL_SIZE);
    }

    public static void saveWheelSize(Context context, float size) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putFloat(Preferences.PREF_WHEEL_SIZE, size);
        edit.apply();
    }

    public static String getWheelUnit(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(Preferences.PREF_WHEEL_UNIT, DEFAULT_WHEEL_UNIT);
    }

    public static void saveWheelUnit(Context context, String unit) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Preferences.PREF_WHEEL_UNIT, unit);
        edit.apply();
    }

    public static String getTrackedDeviceAddress(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(Preferences.PREF_TRACK_DEVICE_ADDRESS, "");
    }

    public static void saveTrackedDeviceAddress(Context context, String deviceAddress) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Preferences.PREF_TRACK_DEVICE_ADDRESS, deviceAddress);
        edit.apply();
    }

    public static String getTrackedDeviceName(Context context) {
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString(Preferences.PREF_TRACK_DEVICE_NAME, "");
    }

    public static void saveTrackedDeviceName(Context context, String deviceName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final SharedPreferences.Editor edit = prefs.edit();
        edit.putString(Preferences.PREF_TRACK_DEVICE_NAME, deviceName);
        edit.apply();
    }

    public static boolean isUnitImperial(Context context) {
        return getWheelUnit(context).equals(UNIT_FOOT);
    }
}
