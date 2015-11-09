package com.tapnic.biketrackerle.settings;

import android.content.Context;
import android.content.DialogInterface;
import android.widget.EditText;
import android.widget.RadioGroup;

import com.tapnic.biketrackerle.R;
import com.tapnic.biketrackerle.events.SettingsChangedEvent;

import de.greenrobot.event.EventBus;


class SetWheelCircumferenceClickListener implements DialogInterface.OnClickListener {
    private final EditText valueEditText;
    private final RadioGroup unit;
    private Context context;

    public SetWheelCircumferenceClickListener(EditText valueEditText, RadioGroup unit, Context context) {
        this.valueEditText = valueEditText;
        this.unit = unit;
        this.context = context;
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        try {
            Float size = Float.valueOf(valueEditText.getText().toString());
            Preferences.saveWheelSize(context, size);
            if (unit.getCheckedRadioButtonId() == R.id.ft) {
                Preferences.saveWheelUnit(context, Preferences.UNIT_FOOT);
            } else {
                Preferences.saveWheelUnit(context, Preferences.UNIT_METER);
            }
            EventBus.getDefault().post(new SettingsChangedEvent());
        } catch (NumberFormatException ex) {
            ex.printStackTrace();
        }
        dialog.dismiss();
    }
}
