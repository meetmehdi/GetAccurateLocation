package com.example.raza.locationaware;

import android.location.Location;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.example.raza.locationaware.location.LocationManagerInterface;
import com.example.raza.locationaware.location.SmartLocationManager;
import com.google.android.gms.location.LocationRequest;

/**
 * Created by Syed Raza Mehdi Naqvi on 8/16/2016.
 */

public class BaseActivityLocation extends AppCompatActivity implements LocationManagerInterface {


    private SmartLocationManager mLocationManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationManager = new SmartLocationManager(getApplicationContext(), this, this, SmartLocationManager.ALL_PROVIDERS, LocationRequest.PRIORITY_HIGH_ACCURACY, 10 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
    }

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
        mLocationManager = new SmartLocationManager(getApplicationContext(), this, this, SmartLocationManager.ALL_PROVIDERS, LocationRequest.PRIORITY_HIGH_ACCURACY, 10 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
    }

    @Override
    public void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider) {
        // storing it on application level
        GetAccurateLocationApplication.mCurrentLocation = mLocation;
        GetAccurateLocationApplication.oldLocation = oldLocation;
        GetAccurateLocationApplication.locationProvider = locationProvider;
        GetAccurateLocationApplication.locationTime = time;
    }

    protected void onStart() {
        super.onStart();
        mLocationManager.startLocationFetching();
    }

    protected void onStop() {
        super.onStop();
        mLocationManager.abortLocationFetching();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mLocationManager.pauseLocationFetching();
    }
}