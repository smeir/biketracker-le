package com.tapnic.biketrackerle.util;


import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.text.TextUtils;

import com.tapnic.biketrackerle.settings.Preferences;

import java.util.Calendar;

public class Util {
    public static final float FOOT_IN_METER = 0.3048f;
    public static final float METER_IN_FOOT = 3.28084f;
    public static final float KM_IN_MILES = 0.621371f;

    public static String formatDistance(double distance) {
        return String.format("%.2f", distance);
    }

    public static float meterToFoot(float wheelSize) {
        return METER_IN_FOOT * wheelSize;
    }

    public static double kilometerToMiles(double distance) {
        return KM_IN_MILES * distance;
    }

    public static float footToMeter(Float wheelSize) {
        return FOOT_IN_METER * wheelSize;
    }

    /**
     * calculate distance for number of revolutions and returns formated text, based on current unit system in preferences
     * @param revolutions number of wheel revolutions
     * @param context application context
     * @return x km or x mi
     */
    static public String getDistanceString(double revolutions, Context context) {
        boolean unitImperial = Preferences.isUnitImperial(context);
        double distance = getDistance(revolutions, context);
        String distanceStr = formatDistance(distance);
        if (unitImperial) {
            distanceStr = distanceStr + " mi";
        } else {
            distanceStr = distanceStr + " km";
        }
        return distanceStr;
    }

    public static double getDistance(double revolutions, Context context) {
        boolean unitImperial = Preferences.isUnitImperial(context);
        double wheelSize = Preferences.getWheelSizeInMeter(context);
        double distance = (revolutions * wheelSize) / 1000;
        if (unitImperial) {
            distance = kilometerToMiles(distance);
        }
        return distance;
    }

    /**
     * returns today in milliseconds since 1.1.1970 at 0:00:00
     */
    public static long getTodayInMillis() {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(System.currentTimeMillis());
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    public static String getConnectionInfo(boolean isConnected, Context context) {
        String title;
        String trackedDeviceName = Preferences.getTrackedDeviceName(context);
        if (isConnected) {
            title = "Connected to " + trackedDeviceName;
        } else {
            if (TextUtils.isEmpty(trackedDeviceName)) {
                title = "Looking for a CSC device...";
            } else {
                title = "Looking for " + trackedDeviceName;
            }
        }
        return title;
    }

    static public String getVersionName(Context context) {
        try {
            return context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return "";
    }
    /* Checks if external storage is available for read and write */
    static public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    /* Checks if external storage is available to at least read */
    static public boolean isExternalStorageReadable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            return true;
        }
        return false;
    }
}
