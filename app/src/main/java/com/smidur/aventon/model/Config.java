package com.smidur.aventon.model;

/**
 * Created by marqueg on 8/30/17.
 */

public class Config {
    boolean killswitch;
    int minimumRequiredVersion;

    public boolean isKillswitch() {
        return killswitch;
    }

    public int getMinimumRequiredVersion() {
        return minimumRequiredVersion;
    }
}
