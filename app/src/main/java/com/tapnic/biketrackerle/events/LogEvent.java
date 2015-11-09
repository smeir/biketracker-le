package com.tapnic.biketrackerle.events;


import com.tapnic.biketrackerle.LogEntry;

public class LogEvent {
    LogEntry entry;

    public LogEvent(LogEntry entry) {
        this.entry = entry;
    }

    public LogEntry getEntry() {
        return entry;
    }

    public void setEntry(LogEntry entry) {
        this.entry = entry;
    }
}
