package com.smidur.aventon.model;

/**
 * Created by marqueg on 5/16/17.
 */

public class SyncPassenger {

    //todo IMPORTANT REMOVE THIS security
    String syncPassengerId;
    SyncDestination syncDestination;
    SyncOrigin syncOrigin;

    public SyncDestination getSyncDestination() {
        return syncDestination;
    }

    public void setSyncDestination(SyncDestination syncDestination) {
        this.syncDestination = syncDestination;
    }

    public String getPassengerId() {
        return syncPassengerId;
    }

    public void setPassengerId(String syncPassengerId) {
        this.syncPassengerId = syncPassengerId;
    }

    public String getSyncPassengerId() {
        return syncPassengerId;
    }

    public void setSyncPassengerId(String syncPassengerId) {
        this.syncPassengerId = syncPassengerId;
    }

    public SyncOrigin getSyncOrigin() {
        return syncOrigin;
    }

    public void setSyncOrigin(SyncOrigin syncOrigin) {
        this.syncOrigin = syncOrigin;
    }
}