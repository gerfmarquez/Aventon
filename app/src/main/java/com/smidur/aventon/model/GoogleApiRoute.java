package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class GoogleApiRoute {
    GoogleApiPolyline overview_polyline;
    GoogleApiBound bounds;
    GoogleApiLeg[] legs;

    public GoogleApiBound getBounds() {
        return bounds;
    }

    public void setBounds(GoogleApiBound bounds) {
        this.bounds = bounds;
    }

    public GoogleApiPolyline getOverview_polyline() {
        return overview_polyline;
    }

    public void setOverview_polyline(GoogleApiPolyline overview_polyline) {
        this.overview_polyline = overview_polyline;
    }

    public GoogleApiLeg[] getLegs() {
        return legs;
    }

    public void setLegs(GoogleApiLeg[] legs) {
        this.legs = legs;
    }
}
