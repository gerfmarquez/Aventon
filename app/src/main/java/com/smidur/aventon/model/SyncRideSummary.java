package com.smidur.aventon.model;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

public class SyncRideSummary {

    private String passengerId;
    private float totalCost;
    private float distance;
    private float duration;
    private long timeCompleted;
    private String dateTimeCompleted;

    //todo add plates and make/model?

    public String getPassengerId() {
        return passengerId;
    }

    public void setPassengerId(String passengerId) {
        this.passengerId = passengerId;
    }

    public float getTotalCost() {
        return totalCost;
    }

    public void setTotalCost(float totalCost) {
        this.totalCost = totalCost;
    }

    public float getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = distance;
    }

    public float getDuration() {
        return duration;
    }

    public void setDuration(float duration) {
        this.duration = duration;
    }

    public long getTimeCompleted() {
        return timeCompleted;
    }

    public void setTimeCompleted(long timeCompleted) {
        this.timeCompleted = timeCompleted;
    }

    public String getDateTimeCompleted() {
        return dateTimeCompleted;
    }

    public void setDateTimeCompleted(String dateTimeCompleted) {
        this.dateTimeCompleted = dateTimeCompleted;
    }
}
