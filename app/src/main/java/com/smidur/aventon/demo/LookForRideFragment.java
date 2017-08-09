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
import android.widget.Toast;

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
import com.smidur.aventon.managers.TaxiMeterManager;
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

        mPickedUpPassengerButton = (Button)mFragmentView.findViewById(R.id.picked_up_passenger);
        mPickupDirectionsButton = (Button)mFragmentView.findViewById(R.id.pickup_directions_button);
        mPickedUpPassengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //enable taximeter
                RideManager.i(getContext()).pauseDriverShiftAndStartRide();
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
                        float totalCost = TaxiMeterManager.i(getContext()).getTotalPrice();
                        //todo dont kill activity and handle end of ride well
//                        RideManager.i(getContext()).resumeDriverShiftAndEndRide();
                        RideManager.i(getContext()).endDriverShift();
                        //todo call server? or just show results from taxi meter to driver?
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(
                                activity);

                        String formatTotalCost = String.format(" %.2f",totalCost);

                        builder.setTitle(R.string.total_cost)
                                .setMessage(getString(R.string.total_cost_message)+formatTotalCost)
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        activity.finish();
                                    }
                                })
                                .create().show();
                    }
                });


            }
        });


        return mFragmentView;
    }


    @Override
    public void onResume() {
        super.onResume();
        RideManager.i(activity).register(driverEventsListener);


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
                                Toast.makeText(getContext(), R.string.enter_plates_error,Toast.LENGTH_LONG).show();
                                getActivity().runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        activity.finish();
                                    }
                                });

                                return;
                            }
                            if(makeModel.trim().isEmpty() || makeModel.trim().length() < 4) {
                                Toast.makeText(getContext(), R.string.enter_model,Toast.LENGTH_LONG).show();
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
        if(!getActivity().isDestroyed()) {
            getActivity().getFragmentManager()
                    .beginTransaction()
                    .remove(mapFragment)
                    .commit();
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
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {


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
