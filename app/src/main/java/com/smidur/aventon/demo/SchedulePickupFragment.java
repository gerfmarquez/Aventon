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
import android.support.v7.widget.AppCompatEditText;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.smidur.aventon.R;
import com.smidur.aventon.managers.RideManager;

/**
 * Created by marqueg on 3/15/17.
 */

public class SchedulePickupFragment extends DemoFragmentBase implements PlaceSelectionListener {

    private int MAXIMUM_RIDE_DISTANCE = 100 * 1000;//km

    /** This fragment's view. */
    private View mFragmentView;
    private MapFragment mapFragment;
    PlaceAutocompleteFragment autocompleteFragment;


    FragmentManager fragmentManager;
    LocationManager locationManager;

    Button mSchedulePickupButton;


    GoogleMap mGoogleMap;

    Place mSelectedPlace;

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

        mapFragment = (MapFragment) fragmentManager.findFragmentById(R.id.map);
//        searchAddress = (AutoCompleteTextView) activity.findViewById(R.id.search_address);

        autocompleteFragment = (PlaceAutocompleteFragment)
                fragmentManager.findFragmentById(R.id.autocomplete_fragment);

        autocompleteFragment.getView().setBackground(new ColorDrawable(getResources().getColor(android.R.color.white)));

        autocompleteFragment.setOnPlaceSelectedListener(this);

        try {
            autocompleteFragment.setBoundsBias(
                    LatLngBounds.builder()
                            .include(getLatLng(getLastKnownLocation()))
                            .build());

        } catch(SecurityException se) {
            //todo analytics
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }


        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                mGoogleMap = googleMap;

            }
        });
        mSchedulePickupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mSelectedPlace == null) {
                    mSchedulePickupButton.setEnabled(false);
                    return;
                }
                RideManager.i(activity).startSchedulePassengerPickup();
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

    RideManager.PassengerEventsListener passengerEventsListener = new RideManager.PassengerEventsListener() {
        @Override
        public void onPickupScheduled(final String driver) {

            RideManager.i(activity).endSchedulePassengerPickup();

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

    @Override
    public void onPlaceSelected(Place place) {

        LatLng latLng = place.getLatLng();

        Location placeLocation  = new Location("");

        placeLocation.setLatitude(latLng.latitude);
        placeLocation.setLongitude(latLng.longitude);

        try {

            float distanceToAddress = placeLocation.distanceTo(getLastKnownLocation());

            if(distanceToAddress <  MAXIMUM_RIDE_DISTANCE) {
                mSchedulePickupButton.setEnabled(true);
                mSelectedPlace = place;

            } else {
                new AlertDialog.Builder(activity).setTitle(R.string.destination_too_far)
                        .setMessage(R.string.destination_too_far)
                        .setPositiveButton(R.string.ok,null).create().show();

            }

        } catch(SecurityException se) {
            //todo analytics
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }




    }

    @Override
    public void onError(Status status) {
        new AlertDialog.Builder(activity).setTitle(R.string.choose_valid_address)
                .setMessage(R.string.choose_valid_address)
                .setPositiveButton(R.string.ok,null).create().show();
    }

    private Location getLastKnownLocation() throws SecurityException {
        Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        return lastKnownLocation;
    }
    private LatLng getLatLng(Location location) {
        return new LatLng(location.getLatitude(),location.getLongitude());
    }
}
