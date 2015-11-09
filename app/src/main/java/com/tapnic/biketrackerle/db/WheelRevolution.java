package com.tapnic.biketrackerle.db;


@SuppressWarnings("UnusedDeclaration")
public class WheelRevolution {

    public Long _id;
    public long date;

    public long value;

    public WheelRevolution() {
    }

    public WheelRevolution(long date, long wheelRevolution) {
        this.date = date;
        this.value = wheelRevolution;
    }
}
