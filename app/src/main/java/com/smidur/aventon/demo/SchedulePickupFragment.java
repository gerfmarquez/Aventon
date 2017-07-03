package com.smidur.aventon.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.smidur.aventon.R;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.utilities.Constants;
import com.smidur.aventon.utilities.GpsUtil;

/**
 * Created by marqueg on 3/15/17.
 */

public class SchedulePickupFragment extends Fragment implements PlaceSelectionListener {

    private int MAXIMUM_RIDE_DISTANCE = 100 * 1000;//km

    private static final int MY_PERMISSIONS_REQUEST_LOCATION = 89103571;//km


    /** This fragment's view. */
    private View mFragmentView;
    private MapFragment mapFragment;
    PlaceAutocompleteFragment autocompleteFragment;


    FragmentManager fragmentManager;
    LocationManager locationManager;


    Button mSchedulePickupButton;
    Marker driverMarker;
    Marker passengerMarker;
    Marker destinationMarker;

    GoogleMap mGoogleMap;

    Place mSelectedDestination;
    ProgressBar scheduleRideProgressBar;

    Activity activity;


    @Override
    public View onCreateView(final LayoutInflater inflater, final ViewGroup container,
                             final Bundle savedInstanceState) {


        activity = this.getActivity();
        fragmentManager = activity.getFragmentManager();
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);



        // Inflate the layout for this fragment
        mFragmentView = inflater.inflate(R.layout.schedule_pickup, container, false);

        mSchedulePickupButton = (Button)mFragmentView.findViewById(R.id.schedule_pickup);
        scheduleRideProgressBar = (ProgressBar) mFragmentView.findViewById(R.id.schedule_progress);
        scheduleRideProgressBar.setVisibility(View.VISIBLE);

        mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);
//        searchAddress = (AutoCompleteTextView) activity.findViewById(R.id.search_address);

        autocompleteFragment = (PlaceAutocompleteFragment)
                fragmentManager.findFragmentById(R.id.autocomplete_fragment);



        autocompleteFragment.getView().setBackground(new ColorDrawable(getResources().getColor(android.R.color.white)));

        autocompleteFragment.setOnPlaceSelectedListener(this);

        autocompleteFragment.setHint(getString(R.string.select_destination));


        try {
            autocompleteFragment.setBoundsBias(
                    LatLngBounds.builder()
                            .include(GpsUtil.getLatLng(GpsUtil.getLastKnownLocation(getContext())))
                            .build());

        } catch(SecurityException se) {
            //todo analytics
            //todo retry or check before attempting so that we know permission is there.
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }


        scheduleRideProgressBar.setVisibility(View.VISIBLE);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                scheduleRideProgressBar.setVisibility(View.GONE);

                mGoogleMap = googleMap;
                mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                GpsUtil.getLatLng(GpsUtil.getLastKnownLocation(getContext())), Constants.PICKUP_MAP_ZOOM));
                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mGoogleMap.getUiSettings().setCompassEnabled(true);
                mGoogleMap.getUiSettings().setMapToolbarEnabled(true);
//                mGoogleMap.setOnCameraMoveListener(new GoogleMap.OnCameraMoveListener() {
//                    @Override
//                    public void onCameraMove() {
//                        updatemarker();
//                    }
//                });

                new Thread() {
                    public void run() {
                        final LatLng passengerLatLng = GpsUtil.getLatLng(
                                GpsUtil.getUserLocation(getActivity()));
                        //retrieving location might take a little while and by that time fragment might go away
                        if(activity!=null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showPassengerLocationOnMap(passengerLatLng);
                                }
                            });
                        }

                    }
                }.start();


            }

        });

        mSchedulePickupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //don't process
                if(mSelectedDestination == null) {
                    return;
                }

                //show progress loader
                scheduleRideProgressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            RideManager.i(activity).startSchedulePassengerPickup(mSelectedDestination);
                        } catch(SecurityException se) {
                            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}
                                    ,MY_PERMISSIONS_REQUEST_LOCATION);
                            //todo retry or check before attempting so that we know permission is there.
                            //todo analytics
                        }
                    }
                }).start();

            }
        });

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

    @Override
    public void onDetach() {
        super.onDetach();

        getActivity().getFragmentManager()
                .beginTransaction()
                .remove(mapFragment)
                .remove(autocompleteFragment)
                .commit();

    }

    RideManager.PassengerEventsListener passengerEventsListener = new RideManager.PassengerEventsListener() {
        @Override
        public void onPickupScheduled(final String driver) {
            //dont end updates from scheduling pickup until driver arrives
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //hide progress loader
                    scheduleRideProgressBar.setVisibility(View.INVISIBLE);

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

        @Override
        public void onSchedulePickupConnectionError() {

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    //hide progress loader
                    scheduleRideProgressBar.setVisibility(View.INVISIBLE);

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("Connection Error").setMessage("Sorry, Connection Error")
                            .setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                }
                            })
                            .create().show();
                }
            });

        }

        @Override
        public void onDriverApproaching(final SyncLocation driverNewLocation) {

            final LatLng driverLatLng = new LatLng(
                    driverNewLocation.getSyncLocationLatitude()
                    ,driverNewLocation.getSyncLocationLongitude());
            final LatLng userLatLng = GpsUtil.getLatLng(GpsUtil.getUserLocation(getContext()));

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateDriverLocationOnMap(driverLatLng,userLatLng);
                }
            });


        }
        @Override
        public void onDriverArrived() {
            //stop receving updates from driver once they arrive
            RideManager.i(activity).endSchedulePassengerPickup();
        }

        @Override
        public void onNoDriverFoundNearby() {
            RideManager.i(activity).endSchedulePassengerPickup();


            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //hide progress loader
                    scheduleRideProgressBar.setVisibility(View.INVISIBLE);

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    builder.setTitle("Sorry, Drivers Not Found Nearby").setMessage("Sorry, Drivers Not Found Nearby")
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

    @Override
    public void onPlaceSelected(Place place) {

        scheduleRideProgressBar.setVisibility(View.VISIBLE);

        LatLng latLng = place.getLatLng();

        Location placeLocation  = new Location("");

        placeLocation.setLatitude(latLng.latitude);
        placeLocation.setLongitude(latLng.longitude);

        try {

            float distanceToAddress = placeLocation.distanceTo(GpsUtil.getLastKnownLocation(getContext()));

            if(distanceToAddress <  MAXIMUM_RIDE_DISTANCE) {
                mSchedulePickupButton.setEnabled(true);
                mSelectedDestination = place;

                new Thread() {
                    public void run() {
                        //zoom in selected place
                        final LatLng userLatLng = GpsUtil.getLatLng(GpsUtil.getUserLocation(getContext()));

                        activity.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                scheduleRideProgressBar.setVisibility(View.GONE);
                                updateDestinationLocationOnMap(userLatLng,mSelectedDestination.getLatLng());
                            }
                        });
                    }
                }.start();


            } else {
                scheduleRideProgressBar.setVisibility(View.GONE);
                new AlertDialog.Builder(activity).setTitle(R.string.destination_too_far)
                        .setMessage(R.string.destination_too_far)
                        .setPositiveButton(R.string.ok,null).create().show();

            }

        } catch(SecurityException se) {
            //todo analytics
            //todo retry or check before attempting so that we know permission is there.
            //todo this shouldn't happen only for few users that disable on purpose,
            // restarting app will request permission again
        }




    }

    @Override
    public void onError(Status status) {
        new AlertDialog.Builder(activity).setTitle(R.string.choose_valid_address)
                .setMessage(R.string.choose_valid_address)
                .setPositiveButton(R.string.ok,null).create().show();
    }


    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {


                } else {
                    activity.finish();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    private void updateDriverLocationOnMap(LatLng driverLatLng, LatLng userLatLng) {


        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot))
                .position(driverLatLng);

        LatLngBounds bounds = LatLngBounds.builder().include(driverLatLng).include(userLatLng).build();
        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 5));

//        mGoogleMap.addGroundOverlay(options);
        if(driverMarker==null) {
            driverMarker = mGoogleMap.addMarker(options);
        }
        driverMarker.setPosition(driverLatLng);
    }

    private void showPassengerLocationOnMap(LatLng latLng) {

        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot))
                .position(latLng);


        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                        latLng, Constants.PICKUP_MAP_ZOOM));


//        mGoogleMap.addGroundOverlay(options);
        if(passengerMarker==null) {
            passengerMarker = mGoogleMap.addMarker(options);
        }
        passengerMarker.setPosition(latLng);
    }

    private void updateDestinationLocationOnMap(LatLng userLatLng, LatLng destLatLng) {


        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot))
                .position(destLatLng);

        LatLngBounds bounds = LatLngBounds.builder().include(destLatLng).include(userLatLng).build();
        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 100));

//        mGoogleMap.addGroundOverlay(options);
        if(destinationMarker==null) {
            destinationMarker = mGoogleMap.addMarker(options);
        }
        destinationMarker.setPosition(destLatLng);
    }

}
