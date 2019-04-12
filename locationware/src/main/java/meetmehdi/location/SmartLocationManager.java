package meetmehdi.location;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.WindowManager;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;

import meetmehdi.interfaces.LocationManagerInterface;
import meetmehdi.interfaces.PermissionManagerInterface;
import tantech.five.locationware.R;


/**
 * Created by Syed Raza Mehdi Naqvi on 8/9/2016.
 */
public class SmartLocationManager implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final String TAG = SmartLocationManager.class.getSimpleName();

    private static final int TWO_MINUTES = 1000 * 60 * 2;
    private static final String DASHBOARD_PERMISSION_DENIED = "One of the Permission is still denied";
    private static final String ERROR = "Error";

    public static final int FAILURE_GOOGLE_PLAY = 0;
    public static final int FAILURE_GOOGLE_CONNECTION = 1;
    public static final int LOCATION_PERMISSION_REQUEST_CODE = 2000;
    public static final String LOCATION_DENIED_MESSAGE = "Location permission is compulsory for Accessing location.";

    // default value is false but user can change it
    private String mLastLocationUpdateTime;                              // fetched location time
    private String locationProvider;                                     // source of fetched location

    private Location mLastLocationFetched;                               // location fetched
    private Location mLocationFetched;                                   // location fetched
    private Location networkLocation;
    private Location gpsLocation;

    private long mFirstLocationFetchInterval;
    private long mFastestLocationFetchInterval;

    private Context mContext;                                             // application context
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private LocationManagerInterface mLocationManagerInterface;

    private LocationManager locationManager;
    private android.location.LocationListener locationListener;

    boolean isGPSEnabled;
    boolean isNetworkEnabled;

    private int mProviderType;
    public static final int NETWORK_PROVIDER = 1;
    public static final int ALL_PROVIDERS = 0;
    public static final int GPS_PROVIDER = 2;

    public static final int ONLY_GOOGLE_API = 0;
    public static final int ONLY_ANDROID_API = 1;
    public static final int ANY_API = 2;

    public int mServiceProvider;

    public static final int FETCH_LOCATION_ONCE = 0;
    public static final int FETCH_LOCATION_BETWEEN_INTERVAL = 1;

    public int mGpsOption;

    public static final int LOCATION_PROVIDER_ALL_RESTRICTION = 1; // internet and gps must
    public static final int LOCATION_PROVIDER_RESTRICTION_NONE = 0; // internet or gps must
    public static final int LOCATION_PROVIDER_GPS_ONLY_RESTRICTION = 2;
    public static final int LOCATION_PROVIDER_NETWORK_ONLY_RESTRICTION = 3;
    private int mForceNetworkProviders = 0;

    public static final String FORMAT_DATE_TIME = "yyyy-MM-dd HH:mm:ss";

    public SmartLocationManager(Context context, int locationFetchingType, LocationManagerInterface locationInterface, int providerType, long fastestLocationFetchInterval, long locationFetchInterval, int forceNetworkProviders, int serviceProvider) {
        mContext = context;
        mProviderType = providerType;

        mForceNetworkProviders = forceNetworkProviders;
        mFirstLocationFetchInterval = locationFetchInterval;
        mFastestLocationFetchInterval = fastestLocationFetchInterval;

        mLocationManagerInterface = locationInterface;
        mServiceProvider = serviceProvider;
        mGpsOption = locationFetchingType;

        initSmartLocationManager(mServiceProvider);
    }

    private boolean isPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            mLocationManagerInterface.onPermissionDenied(LOCATION_DENIED_MESSAGE);
            return false;
        }
        return true;
    }


    public void initSmartLocationManager(int serviceProvider) {
        if (!isPermissionGranted())
            return;
        checkNetworkProviderEnable();

        if (serviceProvider == ONLY_GOOGLE_API) {
            if (isGooglePlayServicesAvailable()) {
                // if google play services available
                createGoogleApiForLocation();
            }
        } else if (serviceProvider == ONLY_ANDROID_API) {
            getLocationUsingAndroidAPI();
        } else if (serviceProvider == ANY_API) {
            if (isGooglePlayServicesAvailable()) {
                createGoogleApiForLocation();
            }
            getLocationUsingAndroidAPI();
        }
    }

    private void createGoogleApiForLocation() {
        // Create the LocationRequest object
        if (mGpsOption == FETCH_LOCATION_ONCE) {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        } else {
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(mFirstLocationFetchInterval)                    // 10 seconds, in milliseconds
                    .setFastestInterval(mFastestLocationFetchInterval);     // 1 second, in milliseconds
        }

        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(mContext)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }

        startGoogleLocationFetching();                                        // connect google play services toast fetch location
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        startLocationUpdates();
    }

    @Override
    public void onLocationChanged(Location location) {
        if (location == null) {
            getLocationUsingAndroidAPI();
        } else {
            //Log.v("new location", "connected");
            setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Connection suspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        mLocationManagerInterface.onLocationFetchingFailed(FAILURE_GOOGLE_CONNECTION, connectionResult);
        getLocationUsingAndroidAPI();                                                                // try to get location using Android API locationManager
    }

    private void setNewLocation(Location location, Location oldLocation) {
        if (location != null) {
            if (oldLocation == null || oldLocation.getLatitude() != location.getLatitude() || oldLocation.getLongitude() != location.getLongitude()) {
                setLocationFetched(location, oldLocation);
            }
        }
    }

    private void setLocationFetched(Location location, Location oldLocation) {
        mLastLocationFetched = oldLocation;
        mLocationFetched = location;
        String dateString = new SimpleDateFormat(FORMAT_DATE_TIME).format(new Date(mLocationFetched.getTime()));
        mLastLocationUpdateTime = dateString;
        locationProvider = location.getProvider();
        mLocationManagerInterface.locationFetched(mLocationFetched, mLastLocationFetched, mLastLocationUpdateTime, location.getProvider());
    }

    private void getLocationUsingAndroidAPI() {
        // Acquire a reference to the system Location Manager
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        setLocationListener();
        captureLocation();
    }

    public void captureLocation() {
        if (!isPermissionGranted())
            return;
        try {
            if (mProviderType == SmartLocationManager.GPS_PROVIDER) {
                if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
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

    private void setLocationListener() {
        // Define a listener that responds to location updates
        locationListener = new android.location.LocationListener() {
            public void onLocationChanged(Location location) {
                // Called when a new location is found by the network location provider.
                if (location == null) {
                    Location lastKnown = getLastKnownLocation();
                    if (lastKnown != null) {
                        //Log.v("new location", "android connected null");
                        setNewLocation(lastKnown, mLocationFetched);
                    }
                } else {
                    //Log.v("new location", "android connected");
                    setNewLocation(getBetterLocation(location, mLocationFetched), mLocationFetched);
                }
                if (mGpsOption == FETCH_LOCATION_ONCE && locationManager != null) {
                    locationManager.removeUpdates(locationListener);
                    locationManager = null;
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
        if (!isPermissionGranted())
            return null;
        try {
            if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return null;
            }
            gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            networkLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            Location newLocalGPS, newLocalNetwork;
            if (gpsLocation != null || networkLocation != null) {
                newLocalGPS = getBetterLocation(mLocationFetched, gpsLocation);
                newLocalNetwork = getBetterLocation(mLocationFetched, networkLocation);
                //Log.v("new location", "accurate location");
                setNewLocation(getBetterLocation(newLocalGPS, newLocalNetwork), mLocationFetched);
            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
        return mLocationFetched;
    }

    protected void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    public void startGoogleLocationFetching() {
        mGoogleApiClient.connect();
        if (mGoogleApiClient.isConnected()) {
            startLocationUpdates();
        }
    }

    public void pauseGoogleLocationFetching() {
        if (mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }

    public void abortLocationFetching() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                mGoogleApiClient.disconnect();
            }
        }
        // Remove the listener you previously added
        if (locationManager != null && locationListener != null) {
            if (!isPermissionGranted())
                return;
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

    public void checkNetworkProviderEnable() {
        locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);

        isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        // getting network status
        if (!isGPSEnabled && !isNetworkEnabled && mForceNetworkProviders == LOCATION_PROVIDER_ALL_RESTRICTION) {
            mLocationManagerInterface.onLocationNotEnabled(mContext.getResources().getString(R.string.location_not_fetched_enable));
        } else if (!isGPSEnabled && !isNetworkEnabled) {
            mLocationManagerInterface.onLocationNotEnabled(mContext.getResources().getString(R.string.location_provider_disable));
        } else if (!isGPSEnabled && mForceNetworkProviders == LOCATION_PROVIDER_GPS_ONLY_RESTRICTION) {
            mLocationManagerInterface.onLocationNotEnabled(mContext.getResources().getString(R.string.location_provider_disable));
        } else if (!isNetworkEnabled && mForceNetworkProviders == LOCATION_PROVIDER_NETWORK_ONLY_RESTRICTION) {
            mLocationManagerInterface.onLocationNotEnabled(mContext.getResources().getString(R.string.location_provider_disable));
        }

    }


    public Location getLastKnownLocation() {
        locationProvider = LocationManager.NETWORK_PROVIDER;
        Location lastKnownLocation = null;
        // Or use LocationManager.GPS_PROVIDER
        if (Build.VERSION.SDK_INT >= 23 &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
        int status = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext);
        if (status == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED) {
            mLocationManagerInterface.onLocationFetchingFailed(FAILURE_GOOGLE_PLAY, null);
            return false;
        } else if (status == ConnectionResult.SUCCESS) {
            return true;
        }
        return false;
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
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

    public static void buildAlertMessageTurnOnLocationProviders(final Context context, String message) {
        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(context.getResources().getString(R.string.setting), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Intent mIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            mIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            context.startActivity(mIntent);
                        }
                    })
                    .setNegativeButton(context.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ((Activity) context).finish();
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public static void startInstalledAppDetailsActivity(Context context) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    public static void showLocationDeniedDialog(Context context, String message, AlertDialog.OnClickListener onClickPositiveButton, AlertDialog.OnClickListener onClickNegativeButton) {
        try {
            AlertDialog sd = new AlertDialog.Builder(context).create();
            sd.setCancelable(false);
            sd.setTitle(ERROR);
            sd.setMessage(message);
            sd.setCanceledOnTouchOutside(false);
            sd.setButton(AlertDialog.BUTTON_POSITIVE, context.getResources().getString(R.string.setting), onClickPositiveButton);
            sd.show();
        } catch (WindowManager.BadTokenException e) {
            e.printStackTrace();
        }
    }

    public static boolean checkPermission(Context context) {
        int result = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION);
        if (result == PackageManager.PERMISSION_GRANTED)
            return true;
        return false;
    }

    @TargetApi(Build.VERSION_CODES.M)
    public static void requestPermission(Context context, PermissionManagerInterface permissionManagerInterface) {
        if (ActivityCompat.shouldShowRequestPermissionRationale((Activity) context, Manifest.permission.ACCESS_FINE_LOCATION)) {
            permissionManagerInterface.onPermissionDenied(LOCATION_DENIED_MESSAGE);
        } else {
            ((Activity) context).requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_PERMISSION_REQUEST_CODE);
        }
    }

    public void pauseLocationFetching() {
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }

    }

}
