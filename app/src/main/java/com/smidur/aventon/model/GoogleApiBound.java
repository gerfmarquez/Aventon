package com.smidur.aventon.model;

/**
 * Created by marqueg on 11/17/15.
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
