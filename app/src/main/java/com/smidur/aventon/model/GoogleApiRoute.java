package com.smidur.aventon.model;

/**
 * Created by marqueg on 11/16/15.
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
