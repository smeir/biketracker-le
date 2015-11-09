package com.tapnic.biketrackerle.settings;

import android.widget.EditText;
import android.widget.RadioGroup;

import com.tapnic.biketrackerle.R;
import com.tapnic.biketrackerle.util.Util;


class UnitRadioGroupChangeListener implements RadioGroup.OnCheckedChangeListener {
    private final EditText valueEditText;

    public UnitRadioGroupChangeListener(EditText valueEditText) {
        this.valueEditText = valueEditText;
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        String value = valueEditText.getText().toString();
        float convertedValue;
        if (checkedId == R.id.ft) {
            convertedValue = Util.meterToFoot(Float.valueOf(value));
        } else {
            convertedValue = Util.footToMeter(Float.valueOf(value));
        }
        valueEditText.setText(String.valueOf(convertedValue));
    }
}
