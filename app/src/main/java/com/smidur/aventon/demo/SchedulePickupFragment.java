package com.smidur.aventon.demo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.drawable.ColorDrawable;

import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.smidur.aventon.R;
import com.smidur.aventon.http.HttpController;
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.GoogleApiDirections;
import com.smidur.aventon.model.GoogleApiLeg;
import com.smidur.aventon.model.GoogleApiPolyline;
import com.smidur.aventon.model.GoogleApiRoute;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncDriver;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.utilities.Constants;
import com.smidur.aventon.utilities.FareUtil;
import com.smidur.aventon.utilities.GpsUtil;
import com.smidur.aventon.utilities.MapUtil;
import com.smidur.aventon.utilities.PolyLineUtil;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by marqueg on 3/15/17.
 */

public class SchedulePickupFragment extends Fragment implements PlaceSelectionListener {



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

    TextView priceEstimate;
    TextView durationEstimate;
    TextView distanceEstimate;

    RelativeLayout routeEstimateFooter;
    RelativeLayout driverInfoFooter;

    Polyline destinationPolyline;

    GoogleMap mGoogleMap;

    SyncDestination mSelectedDestination;
    ProgressBar scheduleRideProgressBar;

    Activity activity;

    boolean routeCalcInProgress = false;


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

        routeEstimateFooter = (RelativeLayout) mFragmentView.findViewById(R.id.map_footer);
        driverInfoFooter = (RelativeLayout) mFragmentView.findViewById(R.id.driver_info_footer);


        autocompleteFragment.getView().setBackground(new ColorDrawable(getResources().getColor(android.R.color.white)));

        autocompleteFragment.setOnPlaceSelectedListener(this);

        autocompleteFragment.setHint(getString(R.string.select_destination));


        priceEstimate = (TextView) mFragmentView.findViewById(R.id.priceEstimate);
        durationEstimate = (TextView) mFragmentView.findViewById(R.id.durationEstimate);
        distanceEstimate = (TextView) mFragmentView.findViewById(R.id.distanceEstimate);


        autocompleteFragment.setBoundsBias(
                LatLngBounds.builder()
                        .include(GpsUtil.getLatLng(GpsUtil.getLastKnownLocation(getContext())))
                        .build());



        scheduleRideProgressBar.setVisibility(View.VISIBLE);
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                scheduleRideProgressBar.setVisibility(View.GONE);

                mGoogleMap = googleMap;
                mGoogleMap.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                                GpsUtil.getLatLng(GpsUtil.getLastKnownLocation(getContext())),
                                Constants.PICKUP_MAP_ZOOM));

                mGoogleMap.getUiSettings().setMyLocationButtonEnabled(true);
                mGoogleMap.getUiSettings().setCompassEnabled(true);
                mGoogleMap.getUiSettings().setMapToolbarEnabled(true);
                mGoogleMap.getUiSettings().setRotateGesturesEnabled(false);

                mGoogleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
                    @Override
                    public void onMapLongClick(final LatLng latLng) {
                        if(routeCalcInProgress) return;
                        routeCalcInProgress = true;

                        ((Vibrator)activity.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(250);

                        String destinationAddress = GpsUtil.geoCodeLocation(
                                latLng.latitude,latLng.longitude, getActivity());

                        MapUtil.selectDestinationPlaceOnMap(destinationSelectedCallback, destinationAddress, latLng, getActivity());
                        scheduleRideProgressBar.setVisibility(View.VISIBLE);
                    }
                });

                new Thread() {
                    public void run() {
                        final LatLng passengerLatLng = GpsUtil.getLatLng(
                                GpsUtil.getUserLocation(getActivity()));
                        //retrieving location might take a little while and by that time fragment might go away
                        if(activity!=null) {
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    showUserLocationOnMap(passengerLatLng);
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
                if(routeCalcInProgress)return;

                //don't process
                if(mSelectedDestination == null) {
                    return;
                }

                //show progress loader
                scheduleRideProgressBar.setVisibility(View.VISIBLE);

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        RideManager.i(activity).startSchedulePassengerPickup(mSelectedDestination);

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
        public void onPickupScheduled(final SyncDriver driver) {
            //dont end updates from scheduling pickup until driver arrives
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //hide progress loader
                    scheduleRideProgressBar.setVisibility(View.INVISIBLE);
                    mSchedulePickupButton.setEnabled(false);
                    mSchedulePickupButton.setVisibility(View.INVISIBLE);

                    routeEstimateFooter.setVisibility(View.GONE);
                    driverInfoFooter.setVisibility(View.VISIBLE);

                    TextView platesTv = (TextView)driverInfoFooter.findViewById(R.id.plates_label);
                    TextView makeModelTv = (TextView)driverInfoFooter.findViewById(R.id.make_model_label);

                    platesTv.setText(driver.getPlates());
                    makeModelTv.setText(driver.getMakeModel());

                    android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                            activity);

                    String message = getContext().getString(R.string.pickup_confirmed)
                            .toString().replace("ss","%s");
                    message = String.format(message,driver.getMakeModel(),driver.getPlates());

                    builder.setTitle(R.string.pickup_confirmed_title).setMessage(message)
                            .setPositiveButton(R.string.ok, null)
                            .setOnCancelListener(null)
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

                    builder.setTitle(R.string.connection_error).setMessage(R.string.sorry_connection_error)
                            .setPositiveButton(R.string.ok, null)
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

                    builder.setTitle(R.string.drivers_not_found_nearby).setMessage(R.string.drivers_not_found_nearby)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
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

        MapUtil.selectDestinationPlaceOnMap(destinationSelectedCallback,place.getAddress().toString(), place.getLatLng(),getActivity());
    }

    @Override
    public void onError(Status status) {
        new AlertDialog.Builder(activity).setTitle(R.string.choose_valid_address)
                .setMessage(R.string.choose_valid_address)
                .setPositiveButton(R.string.ok,null).create().show();
    }




    private void updateDriverLocationOnMap(LatLng driverLatLng, LatLng userLatLng) {


        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot))
                .position(driverLatLng);

        LatLngBounds bounds = LatLngBounds.builder().include(driverLatLng).include(userLatLng).build();
        mGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 100));

//        mGoogleMap.addGroundOverlay(options);
        if(driverMarker==null) {
            driverMarker = mGoogleMap.addMarker(options);
        }
        driverMarker.setPosition(driverLatLng);
    }

    private void showUserLocationOnMap(LatLng latLng) {

        MarkerOptions options = new MarkerOptions();
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot))
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
        options.icon(BitmapDescriptorFactory.fromResource(R.drawable.reddot))
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



    MapUtil.DestinationSelectedCallback destinationSelectedCallback = new MapUtil.DestinationSelectedCallback() {
        @Override
        public void onDestinationSelected(LatLng userLatLng, LatLng destLatLng, PolylineOptions options, GoogleApiLeg leg,
                                          SyncDestination syncDestination, String formattedPrice) {
            routeCalcInProgress = false;

            mSelectedDestination = syncDestination;
            mSchedulePickupButton.setEnabled(true);
            mSchedulePickupButton.setVisibility(View.VISIBLE);
            routeEstimateFooter.setVisibility(View.VISIBLE);

            priceEstimate.setText(formattedPrice
//                                        getString(R.string.price_label)+" "+
            );
            durationEstimate.setText(
//                                        getString(R.string.duration_label)+" "+
                    leg.getDuration().getText());
            distanceEstimate.setText(
//                                        getString(R.string.distance_label)+" "+
                    leg.getDistance().getText());

            if(destinationPolyline!=null)
                destinationPolyline.remove();

            destinationPolyline = mGoogleMap.addPolyline(options);
            scheduleRideProgressBar.setVisibility(View.GONE);
            updateDestinationLocationOnMap(userLatLng,destLatLng);

            scheduleRideProgressBar.setVisibility(View.GONE);
        }

        @Override
        public void onDestinationTooFar() {
            routeCalcInProgress = false;

            mSchedulePickupButton.setEnabled(false);
            scheduleRideProgressBar.setVisibility(View.GONE);
            new AlertDialog.Builder(getContext()).setTitle(R.string.destination_too_far)
                    .setMessage(R.string.destination_too_far)
                    .setPositiveButton(R.string.ok,null).create().show();

            scheduleRideProgressBar.setVisibility(View.GONE);

        }
    };
}
