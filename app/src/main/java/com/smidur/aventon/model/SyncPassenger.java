package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
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
