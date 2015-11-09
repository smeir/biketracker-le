package com.tapnic.biketrackerle;

import java.util.Date;

public class LogEntry {

    String time;
    String text;

    public LogEntry(String time, String text) {
        this.time = time;
        this.text = text;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
