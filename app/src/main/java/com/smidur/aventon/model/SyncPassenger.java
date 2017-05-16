package com.smidur.aventon.model;

/**
 * Created by marqueg on 5/16/17.
 */

public class SyncPassenger {

    SyncLocation syncPassengerLocation;
    SyncDestination syncDestination;

    public SyncDestination getSyncDestination() {
        return syncDestination;
    }

    public void setSyncDestination(SyncDestination syncDestination) {
        this.syncDestination = syncDestination;
    }

    public SyncLocation getSyncPassengerLocation() {
        return syncPassengerLocation;
    }

    public void setSyncPassengerLocation(SyncLocation syncPassengerLocation) {
        this.syncPassengerLocation = syncPassengerLocation;
    }
}
