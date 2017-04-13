package com.smidur.aventon.demo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.smidur.aventon.HttpWrapper;
import com.smidur.aventon.R;

import java.io.IOException;

/**
 * Created by marqueg on 3/15/17.
 */

public class LookForRideFragment extends DemoFragmentBase {

    String TAG = getClass().getSimpleName();

    /** This fragment's view. */
    private View mFragmentView;

    Activity activity;

//    boolean keepLookingForRides = false;
    Button endShiftButton;
    Button startShiftButton;
    Thread lookForRideThread;

    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.lookforride_fragment, container, false);

        startShiftButton = (Button) mFragmentView.findViewById(R.id.start_shift);
        startShiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lookForARideNetworkCall();
            }
        });

        endShiftButton = (Button) mFragmentView.findViewById(R.id.end_shift);
        endShiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endShift();
            }
        });

        activity = this.getActivity();

        return mFragmentView;
    }
    public void endShift() {
        endShiftButton.setEnabled(false);
        lookForRideThread.interrupt();
        if(wrapper!=null && wrapper.getStreamReader()!=null) {
            try {
                wrapper.getStreamReader().close();

            } catch(IOException ioe) {
                ioe.printStackTrace();
            }
        }

    }
    HttpWrapper wrapper;


    public void lookForARideNetworkCall() {
        startShiftButton.setEnabled(false);
        endShiftButton.setEnabled(true);
        lookForRideThread = new Thread() {
            public void run() {
                while(endShiftButton.isEnabled()) {
                    try {

                        wrapper = new HttpWrapper();

                        wrapper.httpGET("available_rides/driver1", new HttpWrapper.UpdateCallback() {
                            @Override
                            public void onUpdate(String message) {

                                activity.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {

                                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                                                activity);

                                        builder.setTitle("Confirm").setMessage("Confirm?")
                                                .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                                    @Override
                                                    public void onClick(DialogInterface dialog, int which) {
                                                        new Thread() {
                                                            public void run() {
                                                                try {
                                                                    HttpWrapper wrapper = new HttpWrapper();

                                                                    wrapper.httpGET("accept_ride/driver1/passenger1"
                                                                            ,activity);
                                                                } catch(IOException ioe) {
                                                                    ioe.printStackTrace();
                                                                }

                                                            }
                                                        }.start();
                                                    }
                                                }).setNegativeButton("Reject",null)
                                                .create().show();
                                    }
                                });


                            }
                        },activity);

                    } catch(IOException ioe) {
                        ioe.printStackTrace();
                        try { Thread.sleep(15000); }catch(InterruptedException ie){}
                        continue;
                    }
                }
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        startShiftButton.setEnabled(true);
                    }
                });

            }
        };
        lookForRideThread.start();
    }

}
