package com.smidur.aventon.model;

/**
 * Created by marqueg on 5/15/17.
 */

public class SyncLocation {
    private double syncLocationLatitude;
    private double syncLocationLongitude;

    public SyncLocation(double syncLocationLatitude, double syncLocationLongitude) {
        this.syncLocationLatitude = syncLocationLatitude;
        this.syncLocationLongitude = syncLocationLongitude;
    }

    public double getSyncLocationLatitude() {
        return syncLocationLatitude;
    }

    public void setSyncLocationLatitude(double syncLocationLatitude) {
        this.syncLocationLatitude = syncLocationLatitude;
    }

    public double getSyncLocationLongitude() {
        return syncLocationLongitude;
    }

    public void setSyncLocationLongitude(double syncLocationLongitude) {
        this.syncLocationLongitude = syncLocationLongitude;
    }
}
