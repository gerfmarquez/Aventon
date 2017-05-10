package com.smidur.aventon.demo;

import android.app.Activity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.smidur.aventon.R;
import com.smidur.aventon.managers.RideManager;

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
                RideManager.i(activity).startPassengerSchedulePickup();
            }
        });

        activity = this.getActivity();

        return mFragmentView;
    }

    @Override
    public void onResume() {
        super.onResume();
        RideManager.i(activity).register(passengerEventsListener);
    }
    @Override
    public void onPause() {
        super.onPause();
        RideManager.i(activity).unregister(passengerEventsListener);
    }

    RideManager.PassengerEventsListener passengerEventsListener = new RideManager.PassengerEventsListener() {
        @Override
        public void onPickupScheduled(final String driver) {

            RideManager.i(activity).endSchedulePickup();

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
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {

                                }
                            })
                            .create().show();
                }
            });


        }

    };

}
