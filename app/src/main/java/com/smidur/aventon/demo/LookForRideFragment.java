package com.smidur.aventon.demo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.smidur.aventon.R;
import com.smidur.aventon.cloud.ApiGatewayController;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.managers.TaxiMeterManager;
import com.smidur.aventon.model.GoogleApiLeg;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.model.SyncRideSummary;
import com.smidur.aventon.navigation.NavigationDrawer;
import com.smidur.aventon.utilities.Constants;
import com.smidur.aventon.utilities.GpsUtil;
import com.smidur.aventon.utilities.MapUtil;
import com.smidur.aventon.utilities.NotificationUtil;

/**
 * Created by marqueg on 3/15/17.
 */

public class LookForRideFragment extends Fragment {

    String TAG = getClass().getSimpleName();

    /** This fragment's view. */
    private View mFragmentView;

    Activity activity;

    SyncPassenger temporaryPassengerVariable;

    Button mPickedUpPassengerButton;
    Button mPickupDirectionsButton;
    Button mCancelRideButton;
    Switch driverSwitch;

    ProgressBar driverProgress;

    MapFragment mapFragment;
    GoogleMap mDriverGoogleMap;
    FragmentManager fragmentManager;

    Marker driverMarker;
    Marker destinationMarker;

    Polyline destinationPolyline;

    boolean isActivityShowing;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.lookforride_fragment, container, false);
        activity = this.getActivity();
        fragmentManager = activity.getFragmentManager();

        mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.driver_map);

        driverProgress = (ProgressBar)mFragmentView.findViewById(R.id.driver_progress);

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {

                mDriverGoogleMap = googleMap;

                mDriverGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mDriverGoogleMap.getUiSettings().setCompassEnabled(true);
                mDriverGoogleMap.getUiSettings().setMapToolbarEnabled(true);
                mDriverGoogleMap.getUiSettings().setRotateGesturesEnabled(false);


                new Thread() {
                    public void run() {
                        Location userLocation = GpsUtil.getUserLocation(getContext());
                        final LatLng passengerLatLng = GpsUtil.getLatLng(userLocation);
                        //retrieving location might take a little while and by that time fragment might go away
                        if(activity!=null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {

                                    showUserLocationOnMap(passengerLatLng);

                                    mDriverGoogleMap.animateCamera(
                                            CameraUpdateFactory.newLatLngZoom(
                                                    passengerLatLng,
                                                    Constants.PICKUP_MAP_ZOOM));
                                }
                            });
                        }

                    }
                }.start();
            }
        });

        mPickedUpPassengerButton = (Button)mFragmentView.findViewById(R.id.picked_up_passenger);
        mPickupDirectionsButton = (Button)mFragmentView.findViewById(R.id.pickup_directions_button);
        mCancelRideButton = (Button)mFragmentView.findViewById(R.id.cancel_ride_button);

        mPickedUpPassengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                new Thread() {
                    public void run() {
                        final Location driverCurrentLocation = GpsUtil.getUserLocation(getContext());

                        new Handler(getContext().getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                //enable taximeter
                                RideManager.i(getContext()).pauseDriverShiftAndStartRide();

                                driverProgress.setVisibility(View.VISIBLE);

                                final SyncLocation destSyncLocation = temporaryPassengerVariable.getSyncDestination().getDestinationLocation();

                                final Location destinationLocation = new Location("");
                                destinationLocation.setLatitude(destSyncLocation.getSyncLocationLatitude());
                                destinationLocation.setLongitude(destSyncLocation.getSyncLocationLongitude());

                                MapUtil.selectDestinationPlaceOnMap(
                                        destinationSelectedCallback,"",driverCurrentLocation,GpsUtil.getLatLng(destinationLocation),activity);

                                //change directions button for destination
                                mPickupDirectionsButton.setText(R.string.destination_directions);
                                mPickupDirectionsButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        SyncLocation destination = RideManager.i(getContext())
                                                .getSyncPassenger().getSyncDestination().getDestinationLocation();

                                        openDirections(destination.getSyncLocationLatitude(),
                                                destination.getSyncLocationLongitude());
                                    }
                                });
                                //show reached destination button
                                mPickedUpPassengerButton.setText(R.string.reached_destination);
                                mPickedUpPassengerButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        final SyncRideSummary rideSummary = TaxiMeterManager.i(getContext()).getRideSummary();

                                        String passengerId = temporaryPassengerVariable.getPassengerId();
                                        rideSummary.setPassengerId(passengerId);

                                        RideManager.i(getContext()).completeRide(rideSummary);



                                    }
                                });
                                mPickupDirectionsButton.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {

                                        openDirections(destinationLocation.getLatitude(),destinationLocation.getLongitude());
                                    }
                                });

                            }
                        });
                    }
                }.start();


            }
        });

        temporaryPassengerVariable = null;

        return mFragmentView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        RideManager.i(activity).register(driverEventsListener);
    }


    @Override
    public void onResume() {
        super.onResume();

        Bundle extras = getActivity().getIntent().getExtras();
        if(extras!= null && extras.containsKey("confirm_ride")) {
            getActivity().getIntent().removeExtra("confirm_ride");
            NotificationUtil.i(getContext()).cancelIncomingRideRequestNotification();
            RideManager.i(activity).confirmPassengerPickup(temporaryPassengerVariable);
            return;
        }

        if(RideManager.i(activity).getDriverInfo() == null) {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                    activity);

            final AlertDialog platesMakeDialog = builder.setTitle(R.string.required_info)
                    .setMessage(R.string.enter_make_plates)
                    .setPositiveButton(R.string.submit, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                            AlertDialog thisDialog = (AlertDialog)dialog;
                            EditText platesView = (EditText)thisDialog.findViewById(R.id.plates);
                            EditText makeModelView = (EditText)thisDialog.findViewById(R.id.makeModel);
                            String plates =  platesView.getText().toString();
                            String makeModel =  makeModelView.getText().toString();

                            if(plates.trim().isEmpty() || plates.trim().length() < 4) {
//                                Toast.makeText(getContext(), R.string.enter_plates_error,Toast.LENGTH_LONG).show();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.finish();
                                    }
                                });

                                return;
                            }
                            if(makeModel.trim().isEmpty() || makeModel.trim().length() < 4) {
//                                Toast.makeText(getContext(), R.string.enter_model,Toast.LENGTH_LONG).show();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.finish();
                                    }
                                });

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
                    .create();
            platesMakeDialog.setView(getLayoutInflater(null)
                    .inflate(R.layout.view_input_plates_model,null));
            platesMakeDialog.show();
//            platesMakeDialog.setContentView(,
//                    new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        } else {
            driverSwitch.setEnabled(true);
        }

        isActivityShowing = true;

    }

    @Override
    public void onPause() {
        super.onPause();
        isActivityShowing  = false;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
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
            if(RideManager.i(context).isDriverAvailable()) {
                driverSwitch.setChecked(true);
            }
            toolbar.addView(driverSwitch,new Toolbar.LayoutParams(Gravity.RIGHT));
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(activity != null) {
            ((Toolbar)activity.findViewById(R.id.toolbar)).removeView(driverSwitch);
        }
        if(!getActivity().isDestroyed()) {
            getActivity().getFragmentManager()
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit();
        }

    }

    RideManager.DriverEventsListener driverEventsListener = new RideManager.DriverEventsListener() {
        @Override
        public void onRideAvailable(SyncPassenger passenger) {

            LookForRideFragment.this.temporaryPassengerVariable = passenger;

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if(isActivityShowing) {
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                                activity);


                        final AlertDialog alertDialog = builder.setTitle(R.string.confirm_ride).setMessage(getString(R.string.pickup_address_at)
                                +temporaryPassengerVariable.getSyncOrigin().getOriginAddress())
                                .setPositiveButton(R.string.confirm_button, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {

                                        RideManager.i(activity).confirmPassengerPickup(temporaryPassengerVariable);


                                    }
                                }).setNegativeButton(R.string.reject,null)
                                .create();

                        alertDialog.show();
                        NotificationUtil.i(getContext()).chime();

                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if(alertDialog.isShowing()) {
                                    alertDialog.cancel();
                                }
                            }
                        },30 * 1000);


                    } else {
                        NotificationUtil.i(getContext()).createNewRideAvailableNotification();
                    }


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
        public void onRideConfirmAccepted(final SyncPassenger passenger) {

            final Location driverCurrentLocation = GpsUtil.getUserLocation(getContext());

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    temporaryPassengerVariable = passenger;

                    Location passengerLocation = new Location("");


                    SyncLocation passSyncLocation = passenger.getSyncOrigin().getOriginLocation();
                    passengerLocation.setLatitude(passSyncLocation.getSyncLocationLatitude());
                    passengerLocation.setLongitude(passSyncLocation.getSyncLocationLongitude());

                    MapUtil.selectDestinationPlaceOnMap(
                            destinationSelectedCallback,"",driverCurrentLocation,GpsUtil.getLatLng(passengerLocation),activity);


                    mPickedUpPassengerButton.setVisibility(View.VISIBLE);
                    mPickupDirectionsButton.setVisibility(View.VISIBLE);
                    driverProgress.setVisibility(View.VISIBLE);//show progress until we show map on map



                    driverSwitch.setEnabled(false);

                    TextView rideInfo = (TextView) activity.findViewById(R.id.ride_info);
                    rideInfo.setVisibility(View.VISIBLE);

                    String destAddress = temporaryPassengerVariable.getSyncDestination().getDestinationAddress();
                    String originAddress = temporaryPassengerVariable.getSyncOrigin().getOriginAddress();

                    final SyncLocation pickupLocation = temporaryPassengerVariable
                            .getSyncOrigin().getOriginLocation();

                    mPickupDirectionsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openDirections(
                                    pickupLocation.getSyncLocationLatitude(),
                                    pickupLocation.getSyncLocationLongitude());
                        }
                    });

                    rideInfo.setText(Html.fromHtml(String.format(
                            rideInfo.getText().toString().replace("ss","%s"),
                            originAddress,
                            destAddress
                    )));

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle(R.string.title_ride_confirmed).setMessage(R.string.message_ride_confirmed)
                            .setPositiveButton(R.string.great, new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    openDirections(pickupLocation.getSyncLocationLatitude(),
                                            pickupLocation.getSyncLocationLongitude());

                                }
                            })
                            .create().show();
                }
            });
        }

        @Override
        public void onRideConfirmedFailed() {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle(R.string.passenger_taken).setMessage(R.string.passenger_taken)
                            .setPositiveButton(R.string.ok,null)
                            .create().show();
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

                    RideManager.i(getContext()).endDriverShift();
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

                    builder.setTitle("Ride Confirm Failed").setMessage(R.string.ride_accept_failed)
                            .setPositiveButton(R.string.ok,null)
                            .create().show();
                }
            });
        }

        @Override
        public void onCompleteRideSuccess(final SyncRideSummary rideSummary) {

            //this shouldn't be null at this point
            String driverEmail = RideManager.i(getContext()).getDriverEmail();

            ApiGatewayController apiGatewayController = new ApiGatewayController();

            apiGatewayController.completeRide(driverEmail,rideSummary, new ApiGatewayController.RideCompletedCallback() {
                        @Override
                        public void onRideCompletedSuccessful() {
                            new Handler(getContext().getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    NotificationUtil.i(getContext()).endOngoingRideNotification();
                                    //todo dont kill activity and handle end of ride well
                                    RideManager.i(getContext()).resumeDriverShiftAndEndRide();

                                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                                            activity);

                                    String formatTotalCost = String.format(" %.2f",rideSummary.getTotalCost());

                                    builder.setTitle(R.string.total_cost)
                                            .setMessage(getString(R.string.total_cost_message)+formatTotalCost)
                                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    Fragment oldLookRideFragment = getFragmentManager().findFragmentByTag(
                                                            NavigationDrawer.Screen.DRIVER_LOOK_FOR_RIDE.name());

                                                    final android.support.v4.app.FragmentManager fragMan = getFragmentManager();

                                                    fragMan.beginTransaction()
                                                            .remove(oldLookRideFragment)
                                                        .commit();

                                                    new Handler().post(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            Fragment newLookRideFragment = new LookForRideFragment();
                                                            fragMan
                                                                .beginTransaction()
                                                                .replace(R.id.main_fragment_container, newLookRideFragment,
                                                                        NavigationDrawer.Screen.DRIVER_LOOK_FOR_RIDE.name())
                                                                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                                                                .commit();

                                                            //todo call clear ride on Ride Manager
                                                        }
                                                    });






                                                }
                                            })
                                            .setCancelable(false)
                                            .create().show();

                                }
                            });
                        }

                        @Override
                        public void onRideCompletedFailed() {
                            Crashlytics.getInstance().logException(new RuntimeException("Lambda Complete Ride Call Failed."));

                        }
                    });
        }

        @Override
        public void onCompleteRideFailure() {

        }
    };

    private void openDirections(double latitude, double longitude) {


        String format = String.format(
                "google.navigation:q=%f,%f&mode=d"
                ,latitude
                ,longitude);

        Uri directionsUri = Uri.parse(format);

        Intent directionsIntent = new Intent(Intent.ACTION_VIEW, directionsUri);
        directionsIntent.setPackage("com.google.android.apps.maps");
        startActivity(directionsIntent);
    }
    private void showUserLocationOnMap(LatLng latLng) {

        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot))
                .position(latLng);


        mDriverGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng, Constants.PICKUP_MAP_ZOOM));


        if(driverMarker ==null) {
            driverMarker = mDriverGoogleMap.addMarker(options);
        }
        driverMarker.setPosition(latLng);
    }

    private void updateDestinationLocationOnMap(LatLng userLatLng, LatLng destLatLng) {


        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot))
                .position(destLatLng);

        Display display = getActivity().getWindowManager().getDefaultDisplay();
        int width = display.getWidth();
        int height = display.getHeight();

        LatLngBounds bounds = LatLngBounds.builder().include(destLatLng).include(userLatLng).build();
        mDriverGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, width,((int)(height*.70f)),500));

//        mDriverGoogleMap.addGroundOverlay(options);
        if(destinationMarker==null) {
            destinationMarker = mDriverGoogleMap.addMarker(options);
        }
        destinationMarker.setPosition(destLatLng);
    }

    MapUtil.DestinationSelectedCallback destinationSelectedCallback = new MapUtil.DestinationSelectedCallback() {
        @Override
        public void onDestinationSelected(LatLng userLatLng, LatLng destLatLng, PolylineOptions options, GoogleApiLeg leg,
                                          SyncDestination syncDestination, String formattedPrice) {


            //optionally keep estimates so that driver can see them

            if(destinationPolyline!=null)
                destinationPolyline.remove();

            destinationPolyline = mDriverGoogleMap.addPolyline(options);

            updateDestinationLocationOnMap(userLatLng,destLatLng);

            driverProgress.setVisibility(View.GONE);
        }

        @Override
        public void onDestinationTooFar() {
            //shouldn't happen
            driverProgress.setVisibility(View.GONE);
        }
    };

}
