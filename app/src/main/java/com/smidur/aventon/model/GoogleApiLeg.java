package com.smidur.aventon.model;

/**
 * Created by marqueg on 7/3/17.
 */

public class GoogleApiLeg {
    GoogleEstimate distance;
    GoogleEstimate duration;

    public GoogleEstimate getDistance() {
        return distance;
    }

    public void setDistance(GoogleEstimate distance) {
        this.distance = distance;
    }

    public GoogleEstimate getDuration() {
        return duration;
    }

    public void setDuration(GoogleEstimate duration) {
        this.duration = duration;
    }
}
