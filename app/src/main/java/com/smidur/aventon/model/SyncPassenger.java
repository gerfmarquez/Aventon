package com.smidur.aventon.model;

/**
 * Created by marqueg on 5/16/17.
 */

public class SyncPassenger {

    //todo IMPORTANT REMOVE THIS
    String passengerId;
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

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }
}
