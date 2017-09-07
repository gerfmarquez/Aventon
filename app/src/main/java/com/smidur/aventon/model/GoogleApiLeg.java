package com.smidur.aventon.model;

/**
 * Created by marqueg on 7/3/17.
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
