package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
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
