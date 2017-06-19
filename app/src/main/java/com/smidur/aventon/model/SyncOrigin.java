package com.smidur.aventon.model;

/**
 * Created by marqueg on 6/18/17.
 */

public class SyncOrigin {

    public SyncOrigin(SyncLocation originLocation,String address) {
        this.originAddress = address;
        this.originLocation = originLocation;
    }

    private String originAddress;
    private SyncLocation originLocation;

    public String getOriginAddress() {
        return originAddress;
    }

    public void setOriginAddress(String originAddress) {
        this.originAddress = originAddress;
    }

    public SyncLocation getOriginLocation() {
        return originLocation;
    }

    public void setOriginLocation(SyncLocation originLocation) {
        this.originLocation = originLocation;
    }
}
