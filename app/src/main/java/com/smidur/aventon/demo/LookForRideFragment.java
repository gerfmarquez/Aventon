package com.smidur.aventon.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

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



    Switch driverSwitch;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.lookforride_fragment, container, false);



        return mFragmentView;
    }


    @Override
    public void onResume() {
        super.onResume();
        RideManager.i(activity).register(driverEventsListener);

        if(RideManager.i(activity).getDriverInfo() == null) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    activity);

            builder.setView(R.layout.view_input_plates_model);



            builder.setTitle(R.string.required_info).setMessage(R.string.enter_make_plates)
                    .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AlertDialog thisDialog = (AlertDialog)dialog;
                            EditText platesView = (EditText)thisDialog.findViewById(R.id.plates);
                            EditText makeModelView = (EditText)thisDialog.findViewById(R.id.makeModel);
                            String plates =  platesView.getText().toString();
                            String makeModel =  makeModelView.getText().toString();

                            if(plates.trim().isEmpty() || plates.trim().length() < 5) {
                                activity.finish();
                                return;
                            }
                            if(makeModel.trim().isEmpty() || makeModel.trim().length() < 5) {
                                activity.finish();
                                return;
                            }
                            //todo validate plates and make and model
                            RideManager.i(getContext()).setDriverInfo(makeModel,plates);
                            driverSwitch.setEnabled(true);

                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            getActivity().finish();
                        }
                    })
                    .create().show();

        } else {
            driverSwitch.setEnabled(true);
        }



    }

    @Override
    public void onPause() {
        super.onPause();
        RideManager.i(activity).unregister(driverEventsListener);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        activity = this.getActivity();

        if(activity != null) {
            Toolbar toolbar = (Toolbar) activity.findViewById(R.id.toolbar);
            driverSwitch = (Switch)getLayoutInflater(null).inflate(R.layout.driver_toolbar,null);
            driverSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if(isChecked) {
                        RideManager.i(activity).startDriverShift();
                    } else {
                        RideManager.i(activity).endDriverShift();
                    }
                }
            });
            toolbar.addView(driverSwitch,new Toolbar.LayoutParams(Gravity.RIGHT));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(activity != null) {
            ((Toolbar)activity.findViewById(R.id.toolbar)).removeView(driverSwitch);
        }
    }

    RideManager.DriverEventsListener driverEventsListener = new RideManager.DriverEventsListener() {
        @Override
        public void onRideAvailable(final SyncPassenger passenger) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);


                    builder.setTitle(R.string.confirm_ride).setMessage(getString(R.string.pickup_address_at)
                            +passenger.getSyncOrigin().getOriginAddress())
                            .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                   RideManager.i(activity).confirmPassengerPickup(passenger);

                                }
                            }).setNegativeButton(R.string.reject,null)
                            .create().show();

                }
            });


        }

        @Override
        public void onRideStarted(final SyncPassenger passenger) {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    driverSwitch.setEnabled(false);

                    activity.findViewById(R.id.no_ride).setVisibility(View.GONE);
                    TextView rideInfo = (TextView) activity.findViewById(R.id.ride_info);
                    rideInfo.setVisibility(View.VISIBLE);

                    String destAddress = passenger.getSyncDestination().getDestinationAddress();
                    String originAddress = passenger.getSyncOrigin().getOriginAddress();

                    rideInfo.setText(Html.fromHtml(String.format(
                                    rideInfo.getText().toString().replace("ss","%s"),
                                    originAddress,
                                    destAddress
                                    )));

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("Ride Confirmed").setMessage("You can now go pickup")
                            .setPositiveButton("Great", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    SyncLocation pickupLocation = passenger
                                            .getSyncOrigin().getOriginLocation();

                                    String format = String.format(
                                            "google.navigation:q=%f,%f&mode=d"
                                            ,pickupLocation.getSyncLocationLatitude()
                                            ,pickupLocation.getSyncLocationLongitude());

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
        public void onRideEnded() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    driverSwitch.setEnabled(true);
                }
            });
        }

        @Override
        public void onLookForRideConnectionError() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle(R.string.connection_error).setMessage(R.string.sorry_connection_error)
                            .setPositiveButton(R.string.ok, null)
                            .create().show();
                    driverSwitch.setChecked(false);
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

                    builder.setTitle("=").setMessage("Ride Accept Failed!")
                            .setPositiveButton("Ok",null)
                            .create().show();
                }
            });
        }
    };

}
