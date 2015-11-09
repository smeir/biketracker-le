package com.tapnic.biketrackerle.db;

import static nl.qbusict.cupboard.CupboardFactory.cupboard;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.tapnic.biketrackerle.util.Util;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class BikeTrackerDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "biketrackerle.db";
    private static final int DATABASE_VERSION = 1;
    private static BikeTrackerDatabase instance;
    private static final AtomicInteger openCounter = new AtomicInteger();

    static {
        // register our models
        cupboard().register(WheelRevolution.class);
    }

    public BikeTrackerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized BikeTrackerDatabase getInstance(final Context c) {
        if (instance == null) {
            instance = new BikeTrackerDatabase(c.getApplicationContext());
        }
        openCounter.incrementAndGet();
        return instance;
    }

    public ArrayList<WheelRevolutionPerDay> getWheelRevolutionPerDays() {

        List<WheelRevolution> revolutions = getAllCumulativeWheelRevolutions();
        ArrayList<WheelRevolutionPerDay> wheelRevolutionPerDays = new ArrayList<>();
        for (int i = 0; i < revolutions.size(); i++) {
            WheelRevolution wheelRevolution = revolutions.get(i);
            long difference;
            if (i != revolutions.size() - 1) {
                WheelRevolution wheelRevolutionNext = revolutions.get(i + 1);
                difference = wheelRevolution.value - wheelRevolutionNext.value;

            } else { // last one, there is no next
                difference = wheelRevolution.value;
            }

            WheelRevolutionPerDay wheelRevolutionPerDay = new WheelRevolutionPerDay(difference, new Date(wheelRevolution.date));
            wheelRevolutionPerDays.add(wheelRevolutionPerDay);
        }
        return wheelRevolutionPerDays;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // this will ensure that all tables are created
        cupboard().withDatabase(db).createTables();
        // add indexes and other database tweaks
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // this will upgrade tables, adding columns and new tables.
        // Note that existing columns will not be converted
        cupboard().withDatabase(db).upgradeTables();
        // do migration work
    }

    @Override
    public void close() {
        if (openCounter.decrementAndGet() == 0) {
            super.close();
        }
    }

    public void updateCumulativeWheelRevolutionForToday(long cumulativeWheelRevolutionValue) {
        long todayInMillis = Util.getTodayInMillis();
        ContentValues values = new ContentValues(1);
        values.put("value", cumulativeWheelRevolutionValue);
        SQLiteDatabase database = getWritableDatabase();
        int numberUpdated = cupboard().withDatabase(database).update(WheelRevolution.class, values, "date = ?", String.valueOf(todayInMillis));
        if (numberUpdated == 0) { // nothing updated, so create a new entry
            WheelRevolution revolution = new WheelRevolution(todayInMillis, cumulativeWheelRevolutionValue);
            cupboard().withDatabase(database).put(revolution);
        }
    }

    /**
     * newest first
     */
    public List<WheelRevolution> getAllCumulativeWheelRevolutions() {
        return cupboard().withDatabase(getReadableDatabase()).query(WheelRevolution.class).orderBy("date desc").list();
    }

    /**
     * @return number of wheel revolutions
     * todo optimize, maybe with limit
     */
    public long getWheelRevolutionsForToday() {
        final ArrayList<WheelRevolutionPerDay> wheelRevolutionPerDays = getWheelRevolutionPerDays();
        if (wheelRevolutionPerDays.size() >= 1) {
            final WheelRevolutionPerDay day = wheelRevolutionPerDays.get(0);
            if (day.getDate().getTime() == Util.getTodayInMillis()) {
                return day.getWheelRevolution();
            }
        }
        return 0;
    }

    public double getTotalWheelRevolutions() {
        final List<WheelRevolution> allCumulativeWheelRevolutions = getAllCumulativeWheelRevolutions();
        if (!allCumulativeWheelRevolutions.isEmpty()) {
            final WheelRevolution revolution = allCumulativeWheelRevolutions.get(0);
            return revolution.value;
        }
        return 0;
    }

    synchronized public boolean insertFromExport(Long date, Long value) {
        final SQLiteDatabase database = getWritableDatabase();
        WheelRevolution revolution = cupboard().withDatabase(database).
                query(WheelRevolution.class).
                withSelection("date = ?", String.valueOf(date)).get();
        if (revolution != null) { // already there, so skip
            return false;
        }
        revolution = new WheelRevolution(date, value);
        cupboard().withDatabase(database).put(revolution);
        return true;
    }
}