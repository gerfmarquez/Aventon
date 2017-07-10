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
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.text.Html;
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
import com.smidur.aventon.managers.RideManager;
import com.smidur.aventon.model.GoogleApiLeg;
import com.smidur.aventon.model.SyncDestination;
import com.smidur.aventon.model.SyncLocation;
import com.smidur.aventon.model.SyncPassenger;
import com.smidur.aventon.utilities.Constants;
import com.smidur.aventon.utilities.GpsUtil;
import com.smidur.aventon.utilities.MapUtil;

/**
 * Created by marqueg on 3/15/17.
 */

public class LookForRideFragment extends Fragment {

    String TAG = getClass().getSimpleName();

    /** This fragment's view. */
    private View mFragmentView;

    Activity activity;


    Button mPickedUpPassengerButton;
    Button mPickupDirectionsButton;
    Switch driverSwitch;

    ProgressBar driverProgress;

    MapFragment mapFragment;
    GoogleMap mDriverGoogleMap;
    FragmentManager fragmentManager;

    Marker driverMarker;
    Marker destinationMarker;

    Polyline destinationPolyline;


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


        return mFragmentView;
    }


    @Override
    public void onResume() {
        super.onResume();
        RideManager.i(activity).register(driverEventsListener);

        mPickedUpPassengerButton = (Button)activity.findViewById(R.id.picked_up_passenger);
        mPickupDirectionsButton = (Button)activity.findViewById(R.id.pickup_directions_button);
        mPickedUpPassengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //todo enable taximeter
                //todo change directions button for destination
            }
        });

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

        getActivity().getFragmentManager()
                .beginTransaction()
                .remove(mapFragment)
                .commit();
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
                                    //todo add callback in case confirm fails

                                    Location passengerLocation = new Location("");
                                    Location destinationLocation = new Location("");

                                    SyncLocation passSyncLocation = passenger.getSyncOrigin().getOriginLocation();
                                    passengerLocation.setLatitude(passSyncLocation.getSyncLocationLatitude());
                                    passengerLocation.setLongitude(passSyncLocation.getSyncLocationLongitude());

                                    final SyncLocation destSyncLocation = passenger.getSyncDestination().getDestinationLocation();

                                    destinationLocation.setLatitude(destSyncLocation.getSyncLocationLatitude());
                                    destinationLocation.setLongitude(destSyncLocation.getSyncLocationLongitude());

                                    MapUtil.selectDestinationPlaceOnMap(
                                            destinationSelectedCallback,"",passengerLocation,GpsUtil.getLatLng(destinationLocation),activity);



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


                    mPickedUpPassengerButton.setVisibility(View.VISIBLE);
                    mPickupDirectionsButton.setVisibility(View.VISIBLE);
                    driverProgress.setVisibility(View.VISIBLE);//show progress until we show map on map



                    driverSwitch.setEnabled(false);

                    TextView rideInfo = (TextView) activity.findViewById(R.id.ride_info);
                    rideInfo.setVisibility(View.VISIBLE);

                    String destAddress = passenger.getSyncDestination().getDestinationAddress();
                    String originAddress = passenger.getSyncOrigin().getOriginAddress();

                    final SyncLocation pickupLocation = passenger
                            .getSyncOrigin().getOriginLocation();

                    mPickupDirectionsButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openDirections(pickupLocation);
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
                                    openDirections(pickupLocation);

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

                    builder.setTitle("=").setMessage(R.string.ride_accept_failed)
                            .setPositiveButton(R.string.ok,null)
                            .create().show();
                }
            });
        }
    };

    private void openDirections(SyncLocation destLocation) {


        String format = String.format(
                "google.navigation:q=%f,%f&mode=d"
                ,destLocation.getSyncLocationLatitude()
                ,destLocation.getSyncLocationLongitude());

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

        LatLngBounds bounds = LatLngBounds.builder().include(destLatLng).include(userLatLng).build();
        mDriverGoogleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, 100));

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
