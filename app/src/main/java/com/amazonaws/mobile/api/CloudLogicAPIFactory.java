package com.amazonaws.mobile.api;

//
//  CloudLogicAPIFactory.java
//
//
// Copyright 2017 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to 
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.18
//

/**
 * Produces instances of Cloud Logic API configuration.
 */
public class CloudLogicAPIFactory {

    private CloudLogicAPIFactory() {}

    /**
     * Gets the configured micro-service instances.
     * @return
     */
    public static CloudLogicAPIConfiguration[] getAPIs() {
        final CloudLogicAPIConfiguration[] apis = new CloudLogicAPIConfiguration[] {
                new CloudLogicAPIConfiguration("LambdaMicroservice",
                        "Created by AWS Lambda",
                        "https://6ymccp9xqc.execute-api.us-east-1.amazonaws.com/production",
                        new String[] {
                                "/checkRegisteredDrivers",
                                "/config",
                                "/completeride",
                        },
                        com.amazonaws.mobile.api.id6ymccp9xqc.LambdaMicroserviceClient.class),
        };

        return apis;
    }
}
