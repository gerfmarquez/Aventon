package com.smidur.aventon.model;

/**
 * Created by marqueg on 5/15/17.
 */

public class SyncDestination {
    private String destinationAddress;
    private SyncLocation destinationLocation;

    public SyncDestination(String destinationAddress, SyncLocation destinationLocation) {
        this.destinationAddress = destinationAddress;
        this.destinationLocation = destinationLocation;
    }

    public String getDestinationAddress() {
        return destinationAddress;
    }

    public void setDestinationAddress(String destinationAddress) {
        this.destinationAddress = destinationAddress;
    }

    public SyncLocation getDestinationLocation() {
        return destinationLocation;
    }

    public void setDestinationLocation(SyncLocation destinationLocation) {
        this.destinationLocation = destinationLocation;
    }
}
