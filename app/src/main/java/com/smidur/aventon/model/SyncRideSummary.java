package com.smidur.aventon.model;

/**
 * Created by marqueg on 8/14/17.
 */

public class SyncRideSummary {

    private String passengerId;
    private float totalCost;
    private float distance;
    private float duration;
    private float timeCompleted;


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

    public float getTimeCompleted() {
        return timeCompleted;
    }

    public void setTimeCompleted(float timeCompleted) {
        this.timeCompleted = timeCompleted;
    }
}
