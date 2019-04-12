package com.example.raza.getaccuratelocation;

import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;

import com.google.android.gms.common.ConnectionResult;

import meetmehdi.interfaces.LocationManagerInterface;
import meetmehdi.interfaces.PermissionManagerInterface;
import meetmehdi.location.SmartLocationManager;


public class LocationFetcherService extends Service implements LocationManagerInterface {
    private SmartLocationManager smartLocationManager;

    private double lat;
    private double lon;

    private static final long SERVICE_PERIOD = (5) * 1000;

    @Override
    public IBinder onBind(Intent arg0) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        initLocationFetching();
        return START_STICKY;
    }

    public class LocationBinder extends Binder {
        public LocationFetcherService getService() {
            return LocationFetcherService.this;
        }
    }

    public void initLocationFetching() {
        try {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!SmartLocationManager.checkPermission(this)) {
                        SmartLocationManager.requestPermission(this, new PermissionManagerInterface() {
                            @Override
                            public void onPermissionDenied(String message) {
                                SmartLocationManager.startInstalledAppDetailsActivity(getApplicationContext());
                            }
                        });
                    } else {
                        smartLocationManager = new SmartLocationManager(this, SmartLocationManager.FETCH_LOCATION_ONCE, this, SmartLocationManager.ALL_PROVIDERS, 2 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_ALL_RESTRICTION, SmartLocationManager.ANY_API); // init location manager
                    }
                } else {
                    smartLocationManager = new SmartLocationManager(this, SmartLocationManager.FETCH_LOCATION_ONCE, this, SmartLocationManager.ALL_PROVIDERS, 2 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_ALL_RESTRICTION, SmartLocationManager.ANY_API); // init location manager
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider) {
        lat = mLocation.getLatitude();
        lon = mLocation.getLongitude();
    }

    @Override
    public void onLocationNotEnabled(String message) {

    }

    @Override
    public void onPermissionDenied(String message) {

    }

    @Override
    public void onLocationFetchingFailed(int failureType, ConnectionResult connectionResult) {

    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        smartLocationManager.abortLocationFetching();
    }
}
