package com.smidur.aventon;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.api.CloudLogicAPI;
import com.amazonaws.mobile.api.idov38qr7b97.AventonMobileHubClient;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.DynamoDBMapper;
import com.amazonaws.models.nosql.TripsDO;
import com.amazonaws.util.StringUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by marqueg on 1/16/17.
 */

public class CallLambdaService {
    String LOG_TAG = "CallLambdaService";
    public void callLambdaService() {
        final DynamoDBMapper dynamoDBMapper = AWSMobileClient.defaultMobileClient().getDynamoDBMapper();
        final TripsDO tripsDO = new TripsDO();

        tripsDO.setUserId(AWSMobileClient.defaultMobileClient().getIdentityManager().getCachedUserID());
        tripsDO.setDropoffAddress("Drop Off Address");
        tripsDO.setPickupAddress("Pick Up Address");
        tripsDO.setDropoffCoordinates(new Double(-1.0));
        tripsDO.setPickupCoordinates(new Double(-1.0));

        AmazonClientException lastException = null;

        try {
            dynamoDBMapper.save(tripsDO);
        } catch (final AmazonClientException ex) {
            Log.e(LOG_TAG, "Failed saving item : " + ex.getMessage(), ex);
            lastException = ex;
        }
    }
    public void invokeAPI() {

        // Set your request method, path, query string parameters, and request body
        final String method = "POST";
        final String path = "/items";
        final String body = "{\"someParameter\":\"someValue\"}";
        final Map<String, String> queryStringParameters = new HashMap<String, String>();
        final Map<String, String> headers = new HashMap<String, String>();

        final byte[] content = body.getBytes(StringUtils.UTF8);

        // Construct the request
        final ApiRequest request =
                new ApiRequest(this.getClass().getSimpleName())
                        .withPath(path)
                        .withHttpMethod(HttpMethodName.valueOf(method))
                        .withHeaders(headers)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Content-Length", String.valueOf(content.length))
                        .withParameters(queryStringParameters)
                        .withBody(content);

        // Create an instance of your custom SDK client
        final AWSMobileClient mobileClient = AWSMobileClient.defaultMobileClient();
        final CloudLogicAPI client = mobileClient.createAPIClient(AventonMobileHubClient.class);

        // Make network call on background thread
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    // Invoke the API
                    final ApiResponse response = client.execute(request);

                    final int statusCode = response.getStatusCode();
                    final String statusText = response.getStatusText();

                    Log.d(LOG_TAG, "Response Status: " + statusCode + " " + statusText);

                    // TODO: Add your custom handling for server response status codes (e.g., 403 Forbidden)

                } catch (final AmazonClientException exception) {
                    Log.e(LOG_TAG, exception.getMessage(), exception);

                    // TODO: Put your exception handling code here (e.g., network error)
                }
            }
        }).start();
    }
}
