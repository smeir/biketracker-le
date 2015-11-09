package com.tapnic.biketrackerle.events;


import com.tapnic.biketrackerle.CSCValue;

public class CSCValueEvent {
    CSCValue value;

    public CSCValueEvent(CSCValue cscValue) {
        value = cscValue;
    }

    public CSCValue getValue() {
        return value;
    }

    public void setValue(CSCValue value) {
        this.value = value;
    }
}
