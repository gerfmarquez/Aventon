package com.smidur.aventon;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import android.util.Log;
import android.view.MotionEvent;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.amazonaws.mobile.AWSMobileClient;
import com.amazonaws.mobile.user.signin.SignInManager;
import com.amazonaws.mobile.user.signin.SignInProvider;
import com.amazonaws.mobile.user.IdentityManager;
import com.amazonaws.mobile.user.IdentityProvider;
import com.crashlytics.android.Crashlytics;
import com.google.gson.Gson;
import com.smidur.aventon.cloud.ApiGatewayController;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.Config;
import com.smidur.aventon.utilities.GpsUtil;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 * Copyright 2020, Gerardo Marquez.
 */

/**
 * Splash Activity is the start-up activity that appears until a delay is expired
 * or the user taps the screen.  When the splash activity starts, various app
 * initialization operations are performed.
 */
public class SplashActivity extends AppCompatActivity {
    private static final String LOG_TAG = SplashActivity.class.getSimpleName();
    private final CountDownLatch timeoutLatch = new CountDownLatch(1);
    private SignInManager signInManager;

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 8910;
    private CountDownLatch latchPermission = new CountDownLatch(1);
    private boolean permissionGrantedResult = false;

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

//            Toast.makeText(SplashActivity.this, String.format("Sign-in with %s succeeded.",
//                    provider.getDisplayName()), Toast.LENGTH_LONG).show();

            AWSMobileClient.defaultMobileClient()
                    .getIdentityManager()
                    .loadUserInfoAndImage(provider, new Runnable() {
                        @Override
                        public void run() {
                           new Thread() {
                               public void run() {


                                   ActivityCompat.requestPermissions(SplashActivity.this,
                                           new String[]{"android.permission.GET_ACCOUNTS",android.Manifest.permission.ACCESS_FINE_LOCATION},MY_PERMISSIONS_REQUEST_LOCATION);

                                   try {
                                       //if user denies the permission countdown latch is still released but security exception
                                       //will be thrown below.
                                       //next time however app will ask for permission again.
                                       latchPermission.await(15, TimeUnit.SECONDS);
                                   }
                                   catch(InterruptedException ie){
                                       //todo log analytics}
                                   }
                                   if(GpsUtil.getLastKnownLocation(SplashActivity.this)==null) {
                                       Crashlytics.logException(new IllegalStateException("latch expired and still no location."));
                                       runOnUiThread(new Runnable() {
                                           @Override
                                           public void run() {
                                               Toast.makeText(SplashActivity.this, getString(R.string.something_wrong_title), Toast.LENGTH_LONG).show();

                                                finish();
                                           }
                                       });
                                       return;
                                   }
                                   if(!permissionGrantedResult)return;

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
                                           runOnUiThread(new Runnable() {
                                               @Override
                                               public void run() {
                                                   Toast.makeText(SplashActivity.this, getString(R.string.connection_error), Toast.LENGTH_LONG).show();
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

//            Toast.makeText(SplashActivity.this, String.format("Sign-in with %s failed.",
//                    provider.getDisplayName()), Toast.LENGTH_LONG).show();
            goSignIn();

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(LOG_TAG, "onCreate");



        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        final ApiGatewayController.ApiGatewayResult pullConfigResultCallback = new ApiGatewayController.ApiGatewayResult() {
            @Override
            public void onSuccess(int code, String message) {
                if(code == 200) {
                    Config config = new Gson().fromJson(message,Config.class);
                    if(config.isKillswitch()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alertKillswitchOn();
                            }
                        });
                        return;
                    }
                    if(BuildConfig.VERSION_CODE >= config.getMinimumRequiredVersion()) {
                        //good
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                redirectAccordingly();

                            }
                        });


                    } else {
                        //bad
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                alertNotRequiredVersion();
                            }
                        });

                    }
                } else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            alertSomethingWrong();
                        }
                    });

                }
            }

            @Override
            public void onError() {
                alertSomethingWrong();
            }
        };

        ApiGatewayController apiGatewayController = new ApiGatewayController();

        apiGatewayController.pullConfig(pullConfigResultCallback);

    }

    private void alertKillswitchOn() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setTitle(R.string.killswitch_title)
                .setMessage(R.string.killswitch_message)
                .setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })
                .setCancelable(false)
                .create().show();
    }
    private void alertNotRequiredVersion() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setTitle(R.string.update_required_title)
                .setMessage(R.string.update_required_message)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //todo open market to download latest
                        finish();
                    }
                })
                .setCancelable(false)
                .create().show();
    }
    private void alertSomethingWrong() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);

        builder.setTitle(R.string.something_wrong_title)
                .setMessage(R.string.something_wrong_title)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //todo open market to download latest
                        finish();
                    }
                })
                .setCancelable(false)
                .create().show();
    }

    private void redirectAccordingly() {
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


        if (requestCode==MY_PERMISSIONS_REQUEST_LOCATION && grantResults.length > 0) {
            boolean grantedResults = true;

            for(int grantResult: grantResults ) {
                grantedResults &= (grantResult == PackageManager.PERMISSION_GRANTED);
            }
            if(grantedResults) {
                permissionGrantedResult = true;

                latchPermission.countDown();

            } else {
                Toast.makeText(SplashActivity.this, getString(R.string.accept_permission), Toast.LENGTH_LONG).show();
                finish();
            }

        } else {
            Toast.makeText(SplashActivity.this, getString(R.string.accept_permission), Toast.LENGTH_LONG).show();
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
