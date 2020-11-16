package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class GoogleApiBound {
    GoogleApiGpsPoint northeast;
    GoogleApiGpsPoint southwest;

    public GoogleApiGpsPoint getNortheast() {
        return northeast;
    }

    public void setNortheast(GoogleApiGpsPoint northeast) {
        this.northeast = northeast;
    }

    public GoogleApiGpsPoint getSouthwest() {
        return southwest;
    }

    public void setSouthwest(GoogleApiGpsPoint southwest) {
        this.southwest = southwest;
    }
}
