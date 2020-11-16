package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
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
