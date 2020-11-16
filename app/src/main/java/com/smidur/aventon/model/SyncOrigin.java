package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
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
