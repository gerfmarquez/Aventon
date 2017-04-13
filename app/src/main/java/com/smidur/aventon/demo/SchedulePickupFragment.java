package com.smidur.aventon.demo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.amazonaws.mobile.AWSMobileClient;
import com.smidur.aventon.HttpWrapper;
import com.smidur.aventon.R;

import java.io.IOException;

/**
 * Created by marqueg on 3/15/17.
 */

public class SchedulePickupFragment extends DemoFragmentBase {

    /** This fragment's view. */
    private View mFragmentView;

    Activity activity;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.schedule_pickup, container, false);

        Button schedulePickupButton = (Button)mFragmentView.findViewById(R.id.schedule_pickup);
        schedulePickupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                schedulePickupNetworkCall();
            }
        });

        activity = this.getActivity();

        return mFragmentView;
    }

    public void schedulePickupNetworkCall() {
        new Thread() {
            public void run() {
//                while(true) {
                    try {

                        HttpWrapper wrapper = new HttpWrapper();

                        wrapper.httpGET("shcedule_pickup/passenger1", new HttpWrapper.UpdateCallback() {
                            @Override
                            public void onUpdate(String message) {

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                                                activity);

                                        builder.setTitle("Pickup Confirmed").setMessage("Driver will pick you up soon")
                                                .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {

                                                    }
                                                })
                                                .create().show();
                                    }
                                });


                            }
                        },activity);

                    } catch(IOException ioe) {
                        ioe.printStackTrace();

                    }
//                }


            }
        }.start();
    }

}
