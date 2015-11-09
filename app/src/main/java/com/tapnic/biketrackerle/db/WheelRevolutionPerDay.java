package com.tapnic.biketrackerle.db;

import java.util.Date;

public class WheelRevolutionPerDay {
    long wheelRevolution;
    Date date;

    public WheelRevolutionPerDay(long wheelRevolution, Date date) {
        this.wheelRevolution = wheelRevolution;
        this.date = date;
    }

    public long getWheelRevolution() {
        return wheelRevolution;
    }

    public void setWheelRevolution(long wheelRevolution) {
        this.wheelRevolution = wheelRevolution;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
