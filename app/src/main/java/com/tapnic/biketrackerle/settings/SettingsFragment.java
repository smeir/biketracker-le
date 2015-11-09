package com.tapnic.biketrackerle.settings;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Toast;

import com.tapnic.biketrackerle.R;
import com.tapnic.biketrackerle.db.BikeTrackerDatabase;
import com.tapnic.biketrackerle.db.WheelRevolution;
import com.tapnic.biketrackerle.events.DataImportedEvent;
import com.tapnic.biketrackerle.events.SettingsChangedEvent;
import com.tapnic.biketrackerle.util.Util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * A placeholder fragment containing a simple view.
 */
public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceClickListener {

    public static final String WHEEL_SIZE_KEY = "wheel_size";
    public static final String IMPORT_KEY = "import";
    public static final String EXPORT_KEY = "export";
    public static final String BIKE_TRACKER_LE_CSV = "BikeTrackerLE.csv";
    public static final String CSV_SEPARATOR = ",";

    private Preference wheelSizePreference;

    public SettingsFragment() {
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);
        wheelSizePreference = findPreference(WHEEL_SIZE_KEY);
        wheelSizePreference.setOnPreferenceClickListener(this);
        updateWheelSizeSummary();
        Preference preference = findPreference(IMPORT_KEY);
        preference.setOnPreferenceClickListener(this);
        preference = findPreference(EXPORT_KEY);
        preference.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        switch (preference.getKey()) {
            case WHEEL_SIZE_KEY:
                wheelSizePreferenceClicked();
                break;
            case IMPORT_KEY:
                importPreferenceClicked();
                break;
            case EXPORT_KEY:
                exportPreferenceClicked();
                break;
        }
        return true;
    }

    private void exportPreferenceClicked() {
        if (Util.isExternalStorageWritable()) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            final File file = new File(Environment.getExternalStorageDirectory(), BIKE_TRACKER_LE_CSV);
            if (file.exists()) {
                builder.setMessage("An export file already exists, you want to overwrite?")
                        .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                                exportToFile(file);
                            }
                        }).setNegativeButton(android.R.string.cancel, new DismissDialogClickListener()).create();
                builder.show();
            } else {
                exportToFile(file);
            }
        } else {
            showMessage(getString(R.string.error_extern_storage_not_available));
        }

    }


    private void exportToFile(File file) {
        BufferedWriter out = null;
        boolean success = false;
        try {
            //noinspection ResultOfMethodCallIgnored
            file.createNewFile();
            out = new BufferedWriter(new FileWriter(file));
            final BikeTrackerDatabase database = BikeTrackerDatabase.getInstance(getActivity());
            final List<WheelRevolution> revolutions = database.getAllCumulativeWheelRevolutions();
            for (WheelRevolution revolution : revolutions) {
                out.append(String.valueOf(revolution.date)).append(CSV_SEPARATOR).
                        append(String.valueOf(revolution.value)).append("\n");
            }
            success = true;
        } catch (IOException e) {
            showMessage(getString(R.string.error_export_data, e.getMessage()));
            e.printStackTrace();
        } finally {
            if (out != null) {
                try {
                    out.close();
                    if (success) {
                        final Toast toast = Toast.makeText(getActivity(),
                                getString(R.string.export_successful, file.getAbsolutePath()),
                                Toast.LENGTH_LONG);
                        toast.show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void importPreferenceClicked() {
        if (Util.isExternalStorageReadable()) {
            File file = new File(Environment.getExternalStorageDirectory(), BIKE_TRACKER_LE_CSV);
            if (!file.exists() || !file.canRead()) {
                final String message = getString(R.string.error_cannot_access_data_file, file.getAbsolutePath());
                showMessage(message);
                return;
            }
            importFromFile(file);
        } else {
            showMessage(getString(R.string.error_extern_storage_not_available));
        }

    }

    private void importFromFile(File file) {
        final BikeTrackerDatabase database = BikeTrackerDatabase.getInstance(getActivity());
        String line;
        String[] data;
        int insertNumber = 0;
        int skippedNumber = 0;
        BufferedReader bufferedReader;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            while ((line = bufferedReader.readLine()) != null) {
                data = line.split(CSV_SEPARATOR);
                try {
                    final Long date = Long.valueOf(data[0]);
                    final Long value = Long.valueOf(data[1]);
                    if (database.insertFromExport(date, value)) {
                        insertNumber++;
                    } else {
                        skippedNumber++;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
            bufferedReader.close();
        } catch (IOException e) {
            showMessage(getString(R.string.error_import_data, e.getMessage()));
            return;
        }

        String message = getString(R.string.entries_imported, insertNumber);
        if (skippedNumber > 0) {
            message = message + "\n" + getString(R.string.entries_skipped, skippedNumber);
        }
        showMessage(message);
        EventBus.getDefault().post(new DataImportedEvent());
    }

    private void showMessage(String message) {
        final AlertDialog alertDialog = new AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, new DismissDialogClickListener()).create();
        alertDialog.show();
    }


    public void wheelSizePreferenceClicked() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        View view = getActivity().getLayoutInflater().inflate(R.layout.wheel_size, null);

        final EditText valueEditText = (EditText) view.findViewById(R.id.wheel_value);
        valueEditText.setText(String.valueOf(Preferences.getWheelSize(getActivity())));


        final RadioGroup unitRadioGroup = (RadioGroup) view.findViewById(R.id.wheel_unit);
        if (Preferences.isUnitImperial(getActivity())) {
            unitRadioGroup.check(R.id.ft);
        } else {
            unitRadioGroup.check(R.id.m);
        }
        unitRadioGroup.setOnCheckedChangeListener(new UnitRadioGroupChangeListener(valueEditText));

        builder.setView(view);
        builder.setTitle("Set wheel circumference");
        builder.setPositiveButton(android.R.string.ok,
                new SetWheelCircumferenceClickListener(valueEditText, unitRadioGroup, getActivity()));
        builder.setNegativeButton(android.R.string.cancel,
                new DismissDialogClickListener());
        builder.create().show();
    }

    @SuppressWarnings("UnusedDeclaration")
    public void onEventMainThread(SettingsChangedEvent ignored) {
        updateWheelSizeSummary();
    }

    public void updateWheelSizeSummary() {
        wheelSizePreference.setSummary(getString(R.string.wheel_size_summary, Preferences.getWheelSize(getActivity()), Preferences.getWheelUnit(getActivity())));
    }


}
