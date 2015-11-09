package com.tapnic.biketrackerle;


public class CSCValue {
    long cumulativeWheelRevolutions;
    double metersPerSeconds;

    public long getCumulativeWheelRevolutions() {
        return cumulativeWheelRevolutions;
    }

    public void setCumulativeWheelRevolutions(long cumulativeWheelRevolutions) {
        this.cumulativeWheelRevolutions = cumulativeWheelRevolutions;
    }

    public double getMetersPerSeconds() {
        return metersPerSeconds;
    }

    public void setMetersPerSeconds(double metersPerSeconds) {
        this.metersPerSeconds = metersPerSeconds;
    }

    public double getKilometersPerSeconds() {
        return metersPerSeconds * 3.6; // 60 * 60 / 1000
    }
}
