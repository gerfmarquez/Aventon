//
// Copyright 2017 Amazon.com, Inc. or its affiliates (Amazon). All Rights Reserved.
//
// Code generated by AWS Mobile Hub. Amazon gives unlimited permission to
// copy, distribute and modify it.
//
// Source code generated from template: aws-my-sample-app-android v0.15
//
package com.smidur.aventon;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;
import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.signin.SignInManager;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.smidur.aventon.cloud.ApiGatewayController;
import com.smidur.aventon.managers.RideManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Splash Activity is the start-up activity that appears until a delay is expired
 * or the user taps the screen.  When the splash activity starts, various app
 * initialization operations are performed.
 */
public class SplashActivity extends Activity {
    private static final String LOG_TAG = SplashActivity.class.getSimpleName();
    private final CountDownLatch timeoutLatch = new CountDownLatch(1);
    private SignInManager signInManager;

    private CountDownLatch latchPermission = new CountDownLatch(1);

    /**
     * SignInResultsHandler handles the results from sign-in for a previously signed in user.
     */
    private class SignInResultsHandler implements IdentityManager.SignInResultsHandler {
        /**
         * Receives the successful sign-in result for an alraedy signed in user and starts the main
         * activity.
         * @param provider the identity provider used for sign-in.
         */
        @Override
        public void onSuccess(final IdentityProvider provider) {
            Log.d(LOG_TAG, String.format("User sign-in with previous %s provider succeeded",
                    provider.getDisplayName()));

            // The sign-in manager is no longer needed once signed in.
            SignInManager.dispose();

            Toast.makeText(SplashActivity.this, String.format("Sign-in with %s succeeded.",
                    provider.getDisplayName()), Toast.LENGTH_LONG).show();

            AWSMobileClient.defaultMobileClient()
                    .getIdentityManager()
                    .loadUserInfoAndImage(provider, new Runnable() {
                        @Override
                        public void run() {
                           new Thread() {
                               public void run() {


                                   requestPermissions(new String[]{"android.permission.GET_ACCOUNTS"},5);

                                   try {
                                       //if user denies the permission countdown latch is still released but security exception
                                       //will be thrown below.
                                       //next time however app will ask for permission again.
                                       latchPermission.await();
                                   }
                                   catch(InterruptedException ie){
                                       //todo log analytics}
                                   }

                                   AccountManager accountManager = (AccountManager) getSystemService(Context.ACCOUNT_SERVICE);

                                   String email = null;
                                   try {
                                       Account[] accounts = accountManager.getAccounts();
                                       for(Account account: accounts) {
                                           if(account.name.contains("gmail")) {
                                               Log.d("","Account: "+account.name);
                                               email = account.name;
                                               RideManager.i(SplashActivity.this).setDriverEmail(email);
                                               break;
                                           }

                                       }
                                   }catch(SecurityException se) {

                                       return;
                                   }

                                   if(email == null) {
                                       finish();
                                       return;
                                   }
                                   ApiGatewayController apiGatewayController = new ApiGatewayController();

                                   apiGatewayController.checkDriverRegistered(email,new ApiGatewayController.DriverRegisteredCallback() {
                                       @Override
                                       public void onDriverRegistered() {

                                           goMain("driver");
                                       }

                                       @Override
                                       public void onDriverNotRegistered() {

                                           goMain("passenger");

                                       }

                                       @Override
                                       public void onError() {
                                           Toast.makeText(SplashActivity.this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                                           runOnUiThread(new Runnable() {
                                               @Override
                                               public void run() {
                                                   finish();
                                               }
                                           });
                                       }
                                   });
                               }
                           }.start();
                        }
                    });




        }

        /**
         * For the case where the user previously was signed in, and an attempt is made to sign the
         * user back in again, there is not an option for the user to cancel, so this is overriden
         * as a stub.
         * @param provider the identity provider with which the user attempted sign-in.
         */
        @Override
        public void onCancel(final IdentityProvider provider) {
            Log.wtf(LOG_TAG, "Cancel can't happen when handling a previously sign-in user.");
        }

        /**
         * Receives the sign-in result that an error occurred signing in with the previously signed
         * in provider and re-directs the user to the sign-in activity to sign in again.
         * @param provider the identity provider with which the user attempted sign-in.
         * @param ex the exception that occurred.
         */
        @Override
        public void onError(final IdentityProvider provider, Exception ex) {
            Log.e(LOG_TAG,
                    String.format("Cognito credentials refresh with %s provider failed. Error: %s",
                            provider.getDisplayName(), ex.getMessage()), ex);

            Toast.makeText(SplashActivity.this, String.format("Sign-in with %s failed.",
                    provider.getDisplayName()), Toast.LENGTH_LONG).show();
            goSignIn();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);


        final Thread thread = new Thread(new Runnable() {
            public void run() {
                signInManager = SignInManager.getInstance(SplashActivity.this);

                final SignInProvider provider = signInManager.getPreviouslySignedInProvider();

                // if the user was already previously in to a provider.
                if (provider != null) {
                    // asynchronously handle refreshing credentials and call our handler.
                    signInManager.refreshCredentialsWithProvider(SplashActivity.this,
                            provider, new SignInResultsHandler());
                } else {
                    // Asyncronously go to the sign-in page (after the splash delay has expired).
                    goSignIn();
                }

                // Wait for the splash timeout.
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) { }

                // Expire the splash page delay.
                timeoutLatch.countDown();
            }
        });
        thread.start();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Touch event bypasses waiting for the splash timeout to expire.
        timeoutLatch.countDown();
        return true;
    }

    /**
     * Starts an activity after the splash timeout.
     * @param intent the intent to start the activity.
     */
    private void goAfterSplashTimeout(final Intent intent) {
        final Thread thread = new Thread(new Runnable() {
            public void run() {
                // wait for the splash timeout expiry or for the user to tap.
                try {
                    timeoutLatch.await();
                } catch (InterruptedException e) {
                }

                SplashActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        startActivity(intent);
                        // finish should always be called on the main thread.
                        finish();
                    }
                });
            }
        });
        thread.start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        latchPermission.countDown();
        if (requestCode==5 && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        } else {
            Toast.makeText(SplashActivity.this, R.string.accept_permission, Toast.LENGTH_LONG).show();
            finish();
        }
    }

    /**
     * Go to the main activity after the splash timeout has expired.
     */
    protected void goMain(String mode) {

        Log.d(LOG_TAG, "Launching Main Activity...");
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("mode",mode);
        goAfterSplashTimeout(intent);
    }

    /**
     * Go to the sign in activity after the splash timeout has expired.
     */
    protected void goSignIn() {
        Log.d(LOG_TAG, "Launching Sign-in Activity...");
        goAfterSplashTimeout(new Intent(this, SignInActivity.class));
    }
}
