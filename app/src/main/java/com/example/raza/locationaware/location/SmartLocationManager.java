package com.example.raza.locationaware.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.DateFormat;
import java.util.Date;

/**
 * Created by Syed Raza Mehdi Naqvi on 8/9/2016.
 */
public class SmartLocationManager implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private static final String TAG = SmartLocationManager.class.getSimpleName();

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final int PERMISSION_REQUEST_CODE = 1000;
    private static final int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    // default value is false but user can change it
    private String mLastLocationUpdateTime;                                                         // fetched location time
    private String locationProvider;                                                                // source of fetched location

    private Location mLastLocationFetched;                                                          // location fetched
    private Location mLocationFetched;                                                              // location fetched
    private Location networkLocation;
    private Location gpsLocation;

    private int mLocationPiority;
    private long mLocationFetchInterval;
    private long mFastestLocationFetchInterval;

    private Context mContext;                                                                       // application context
    private Activity mActivity;                                                                     // activity context
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationManagerInterface mLocationManagerInterface;

    private android.location.LocationManager locationManager;
    private android.location.LocationListener locationListener;

    boolean isGPSEnabled;
    boolean isNetworkEnabled;

    private int mProviderType;
    public static final int NETWORK_PROVIDER = 1;
    public static final int ALL_PROVIDERS = 0;
    public static final int GPS_PROVIDER = 2;

//    private final double STANDARD_LOCATION_ACCURACY = 100.0;
//    private final double STANDARD_LOCATION_SEED_LIMIT = 6.95;

    public static final int LOCATION_PROVIDER_ALL_RESTICTION = 1;
    public static final int LOCATION_PROVIDER_RESTRICTION_NONE = 0;
    public static final int LOCATION_PROVIDER_GPS_ONLY_RESTICTION = 2;
    public static final int LOCATION_PROVIDER_NETWORK_ONLY_RESTICTION = 3;
    private int mForceNetworkProviders = 0;

    public SmartLocationManager(Context context, Activity activity, LocationManagerInterface locationInterface, int providerType, int locationPiority, long locationFetchInterval, long fastestLocationFetchInterval, int forceNetworkProviders) {
        mContext = context;
        mActivity = activity;
        mProviderType = providerType;

        mLocationPiority = locationPiority;
        mForceNetworkProviders = forceNetworkProviders;
        mLocationFetchInterval = locationFetchInterval;
        mFastestLocationFetchInterval = fastestLocationFetchInterval;

        mLocationManagerInterface = locationInterface;

        initSmartLocationManager();
    }


    public void initSmartLocationManager() {

        // 1) ask for permission for Android 6 above to avoid crash
        // 2) check if gps is available
        // 3) get location using awesome strategy

        askLocationPermission();                            // for android version 6 above
        checkNetworkProviderEnable(mForceNetworkProviders);                       //

        if (isGooglePlayServicesAvailable())                // if googleplay services available
            initLocationObjts();                            // init obj for google play service and start fetching location
        else
            getLocationUsingAndroidAPI();                   // otherwise get location using Android API
    }

    private void initLocationObjts() {
        // Create the LocationRequest object
        mLocationRequest = LocationRequest.create()
                .setPriority(mLocationPiority)
                .setInterval(mLocationFetchInterval)                    // 10 seconds, in milliseconds
                .setFastestInterval(mFastestLocationFetchInterval);     // 1 second, in milliseconds

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mActivity)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        startLocationFetching();                                        // connect google play services to fetch location
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        startLocationUpdates();
        if (location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
            getLocationUsingAndroidAPI();
        } else {
            setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            getLastKnownLocation();
        } else {
            setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(mActivity, CONNECTION_FAILURE_RESOLUTION_REQUEST); // Start an Activity that tries to resolve the error
                getLocationUsingAndroidAPI();                                                                // try to get location using Android API locationManager
            } catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        } else {
            Log.i(TAG, "Location services connection failed with code " + connectionResult.getErrorCode());
        }
    }

    private void setNewLocation(Location location, Location oldLocation) {
        if (location != null) {
            mLastLocationFetched = oldLocation;
            mLocationFetched = location;
            mLastLocationUpdateTime = DateFormat.getTimeInstance().format(new Date());
            locationProvider = location.getProvider();
            mLocationManagerInterface.locationFetched(location, mLastLocationFetched, mLastLocationUpdateTime, location.getProvider());
        }
    }

    private void getLocationUsingAndroidAPI() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        setLocationListner();
        captureLocation();
    }

    public void captureLocation() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        try {
            if (mProviderType == SmartLocationManager.GPS_PROVIDER) {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            } else if (mProviderType == SmartLocationManager.NETWORK_PROVIDER) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
            } else {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    private void setLocationListner() {
        // Define a listener that responds to location updates
        locationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (location == null) {
                    getLastKnownLocation();
                } else {
                    setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
//                    if (isLocationAccurate(location) && location.getAccuracy() < STANDARD_LOCATION_ACCURACY && location.getSpeed() < STANDARD_LOCATION_SEED_LIMIT) {// no use of this if
//                        setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
//                    } else {
//                        setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
//                    }
                }
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            public void onProviderEnabled(String provider) {
            }

            public void onProviderDisabled(String provider) {
            }
        };
    }

    public Location getAccurateLocation() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        try {
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location newLocalGPS, newLocalNetwork;
            if (gpsLocation != null || networkLocation != null) {
                newLocalGPS = getBetterLocation(mLocationFetched, gpsLocation);
                newLocalNetwork = getBetterLocation(mLocationFetched, networkLocation);
                setNewLocation(getBetterLocation(newLocalGPS, newLocalNetwork), mLocationFetched);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return mLocationFetched;
    }

    protected void startLocationUpdates() {
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void startLocationFetching() {
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void pauseLocationFetching() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }

    public void abortLocationFetching() {
        mGoogleApiClient.disconnect();

        // Remove the listener you previously added
        if (locationManager != null && locationListener != null) {
            if (Build.VERSION.SDK_INT >= 23 &&
                    ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            try {
                locationManager.removeUpdates(locationListener);
                locationManager = null;
            } catch (Exception ex) {
                Log.e(TAG, ex.getMessage());

            }
        }
    }

    public void resetLocation() {
        mLocationFetched = null;
        mLastLocationFetched = null;
        networkLocation = null;
        gpsLocation = null;
    }

    //  Android M Permission check
    public void askLocationPermission() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {


            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    || ContextCompat.checkSelfPermission(mActivity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                    ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_COARSE_LOCATION)
                        || ActivityCompat.shouldShowRequestPermissionRationale(mActivity, Manifest.permission.ACCESS_FINE_LOCATION)) {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
                    builder.setMessage("Please allow all permissions in App Settings for additional functionality.")
                            .setCancelable(false)
                            .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                                public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                    Toast.makeText(mContext, "Welcome", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton("Deny", new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                                    mActivity.finish();
                                }
                            });
                    final AlertDialog alert = builder.create();
                    alert.show();

                } else
                    ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION
                            , Manifest.permission.ACCESS_FINE_LOCATION
                    }, PERMISSION_REQUEST_CODE);

            }
        }
    }

    public void checkNetworkProviderEnable(int enforceActive) {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!isGPSEnabled && !isNetworkEnabled) {
            buildAlertMessageTurnOnLocationProviders("Your location providers seems to be disabled, please enable it", "OK", "Cancel");
        } else if (!isGPSEnabled && mForceNetworkProviders == LOCATION_PROVIDER_GPS_ONLY_RESTICTION) {
            buildAlertMessageTurnOnLocationProviders("Your GPS seems to be disabled, please enable it", "OK", "Cancel");
        } else if (!isNetworkEnabled && mForceNetworkProviders == LOCATION_PROVIDER_NETWORK_ONLY_RESTICTION) {
            buildAlertMessageTurnOnLocationProviders("Your Network location provider seems to be disabled, please enable it", "OK", "Cancel");
        }
        // getting network status

        if (!isGPSEnabled && !isNetworkEnabled) {
            Toast.makeText(mContext, "Location can't be fetched!", Toast.LENGTH_SHORT).show(); // show alert
            mActivity.finish();
        }
    }

    private void buildAlertMessageTurnOnLocationProviders(String message, String positiveButtonText, String negativeButtonText) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        builder.setMessage(message)
                .setCancelable(false)
                .setPositiveButton(positiveButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        Intent mIntent = new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        mContext.startActivity(mIntent);
                    }
                })
                .setNegativeButton(negativeButtonText, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        mActivity.finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }


    public Location getLastKnownLocation() {
        locationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = null;
        // Or use LocationManager.GPS_PROVIDER
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return lastKnownLocation;
        }
        try {
            lastKnownLocation = locationManager.getLastKnownLocation(locationProvider);
            return lastKnownLocation;
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
        return lastKnownLocation;
    }

    public boolean isGooglePlayServicesAvailable() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);

        if (status == ConnectionResult.SUCCESS) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Determines whether one Location reading is better than the current Location fix
     *
     * @param location            The new Location that you want to evaluate
     * @param currentBestLocation The current Location fix, to which you want to compare the new one
     */
    protected Location getBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return location;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > TWO_MINUTES;
        boolean isSignificantlyOlder = timeDelta < -TWO_MINUTES;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return location;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return currentBestLocation;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return location;
        } else if (isNewer && !isLessAccurate) {
            return location;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return location;
        }
        return currentBestLocation;
    }

    /**
     * Checks whether two providers are the same
     */

    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    public boolean isLocationAccurate(Location location) {
        if (location.hasAccuracy()) {
            return true;
        } else {
            return false;
        }
    }

    public Location getStaleLocation() {
        if (mLastLocationFetched != null) {
            return mLastLocationFetched;
        }
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return null;
        }
        if (mProviderType == SmartLocationManager.GPS_PROVIDER) {
            return locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        } else if (mProviderType == SmartLocationManager.NETWORK_PROVIDER) {
            return locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
        } else {
            return getBetterLocation(locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER), locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER));
        }
    }
}
