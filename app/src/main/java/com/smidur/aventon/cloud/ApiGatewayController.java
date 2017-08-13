package com.smidur.aventon.cloud;

import android.util.Log;

import com.amazonaws.AmazonClientException;
import com.amazonaws.http.HttpMethodName;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.api.CloudLogicAPI;
import com.amazonaws.mobile.api.CloudLogicAPIFactory;
import com.amazonaws.mobile.api.id6ymccp9xqc.LambdaMicroserviceClient;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobileconnectors.apigateway.ApiRequest;
import com.amazonaws.mobileconnectors.apigateway.ApiResponse;
import com.amazonaws.util.StringUtils;
import com.google.gson.Gson;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by marqueg on 8/8/17.
 */

public class ApiGatewayController {

    String TAG = getClass().getSimpleName();



    public void checkDriverRegistered(String email,final DriverRegisteredCallback driverRegisteredCallback) {

        String path = "/checkRegisteredDrivers";

        final ApiGatewayResult apiGatewayResult = new ApiGatewayResult() {
            @Override
            public void onSuccess(int code, String message) {
                if(code == 200) {
                    driverRegisteredCallback.onDriverRegistered();
                } else {
                    driverRegisteredCallback.onDriverNotRegistered();
                }
            }

            @Override
            public void onError() {
                driverRegisteredCallback.onError();
            }
        };

        // Set your request method, path, query string parameters, and request body
        final String method = "POST";

        final Map<String, String> headers = new HashMap<String, String>();

//        final byte[] content = jsonBody.getBytes(StringUtils.UTF8);

        // Create an instance of your custom SDK client
        final AWSMobileClient mobileClient = AWSMobileClient.defaultMobileClient();
        final CloudLogicAPI client = mobileClient.createAPIClient(LambdaMicroserviceClient.class);

        // Construct the request
        final ApiRequest request =
                new ApiRequest(client.getClass().getSimpleName())
                        .withPath(path)
                        .withHttpMethod(HttpMethodName.valueOf(method))
                        .withHeaders(headers)
                        .withParameter("email",email)
                        .addHeader("Content-Type", "application/json")
                        .addHeader("Content-Length", String.valueOf(1))//content.length))
                        .withBody(".")
                ;


        // Make network call on background thread
        new Thread(new Runnable() {

            @Override
            public void run() {
                try {

                    // Invoke the API
                    final ApiResponse response = client.execute(request);

                    final int statusCode = response.getStatusCode();
                    final String statusText = response.getStatusText();

                    Log.d(TAG, "Response Status: " + statusCode + " " + statusText);

                    apiGatewayResult.onSuccess(statusCode,statusText);


                } catch (final AmazonClientException exception) {
                    Log.e(TAG, exception.getMessage(), exception);

                    apiGatewayResult.onError();
                }
            }
        }).start();

    }

//    public void completeRide(long timeCompleted,String email, float totalCost,final DriverRegisteredCallback driverRegisteredCallback) {
//
//        String path = "/completeride";
//
//        HashMap<String,String> toJson = new HashMap<>();
//        toJson.put("timeCompleted",""+timeCompleted);
//        toJson.put("email",""+email);
//        toJson.put("totalCost",""+totalCost);
//
//        String body = new Gson().toJson(toJson);
//
//        ApiGatewayResult apiGatewayResult = new ApiGatewayResult() {
//            @Override
//            public void onSuccess(int code, String message) {
//                if(code == 200) {
//                    driverRegisteredCallback.onDriverRegistered();
//                } else {
//                    driverRegisteredCallback.onDriverNotRegistered();
//                }
//            }
//
//            @Override
//            public void onError() {
//                driverRegisteredCallback.onError();
//            }
//        };
//        invokeAwsGatewayApi(path,apiGatewayResult);
//    }

    private void invokeAwsGatewayApi(final String path,final ApiGatewayResult apiGatewayResult) {


    }


    public interface DriverRegisteredCallback {
        public void onDriverRegistered();
        public void onDriverNotRegistered();
        public void onError();
    }

    interface ApiGatewayResult {
        void onSuccess(int code, String message);
        void onError();
    }
}
