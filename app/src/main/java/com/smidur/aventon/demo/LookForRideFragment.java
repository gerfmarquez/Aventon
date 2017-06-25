package com.smidur.aventon.demo;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.smidur.aventon.R;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;

/**
 * Created by marqueg on 3/15/17.
 */

public class LookForRideFragment extends Fragment {

    String TAG = getClass().getSimpleName();

    /** This fragment's view. */
    private View mFragmentView;

    Activity activity;

//    boolean keepLookingForRides = false;
    Button startShiftButton;
    Button endShiftButton;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.lookforride_fragment, container, false);

        startShiftButton = (Button) mFragmentView.findViewById(R.id.start_shift);
        startShiftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startShiftButton.setEnabled(false);
                endShiftButton.setEnabled(true);
                RideManager.i(activity).startDriverShift();

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
        startShiftButton.setEnabled(true);
        RideManager.i(activity).endDriverShift();


    }

    @Override
    public void onResume() {
        super.onResume();
        RideManager.i(activity).register(driverEventsListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        RideManager.i(activity).unregister(driverEventsListener);
    }

    RideManager.DriverEventsListener driverEventsListener = new RideManager.DriverEventsListener() {
        @Override
        public void onRideAvailable(final SyncPassenger passenger) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("=").setMessage("Confirm ride for passenger: ?")
                            .setPositiveButton("Confirm", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                   RideManager.i(activity).confirmPassengerPickup(passenger);

                                }
                            }).setNegativeButton("Reject",null)
                            .create().show();
                    startShiftButton.setEnabled(true);
                }
            });


        }

        @Override
        public void onRideStarted(final SyncPassenger passenger) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("Ride Confirmed").setMessage("You can now go pickup")
                            .setPositiveButton("Great", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    SyncLocation destinationLocation = passenger
                                            .getSyncDestination()
                                            .getDestinationLocation();

                                    String format = String.format(
                                            "google.navigation:q=%f,%f&mode=d"
                                            ,destinationLocation.getSyncLocationLatitude()
                                            ,destinationLocation.getSyncLocationLongitude());

                                    Uri directionsUri = Uri.parse(format);

                                    Intent directionsIntent = new Intent(Intent.ACTION_VIEW, directionsUri);
                                    directionsIntent.setPackage("com.google.android.apps.maps");
                                    startActivity(directionsIntent);
                                }
                            })
                            .create().show();
                }
            });
        }

        @Override
        public void onRideAcceptFailed() {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("=").setMessage("Error:")
                            .setPositiveButton("Ok",null)
                            .create().show();
                }
            });
        }
    };

}
