package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class GoogleApiLeg {
    GoogleEstimate distance;
    GoogleEstimate duration_in_traffic;

    public GoogleEstimate getDistance() {
        return distance;
    }

    public void setDistance(GoogleEstimate distance) {
        this.distance = distance;
    }

    public GoogleEstimate getDurationInTraffic() {
        return duration_in_traffic;
    }

    public void setDurationInTraffic(GoogleEstimate duration_in_traffic) {
        this.duration_in_traffic = duration_in_traffic;
    }
}
