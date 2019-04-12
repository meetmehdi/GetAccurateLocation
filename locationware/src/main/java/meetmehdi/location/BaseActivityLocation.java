package meetmehdi.location;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;

import meetmehdi.interfaces.LocationManagerInterface;
import meetmehdi.interfaces.PermissionManagerInterface;

/**
 * Created by Syed Raza Mehdi Naqvi on 8/16/2016.
 */

public abstract class BaseActivityLocation extends AppCompatActivity implements LocationManagerInterface, PermissionManagerInterface {
    private final String TAG = BaseActivityLocation.class.getSimpleName();

    private static final int REQUEST_FINE_LOCATION = 1;

    private SmartLocationManager mLocationManager;
    private Location location;


    @Override
    public void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider) {
        // storing it on application level
        location = mLocation;
        Log.v(TAG, mLocation.toString());
    }

    @Override
    public void onLocationNotEnabled(String message) {
        SmartLocationManager.buildAlertMessageTurnOnLocationProviders(this, message);
    }

    @Override
    public void onPermissionDenied(String message) {
        SmartLocationManager.showLocationDeniedDialog(this, message, new AlertDialog.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
                SmartLocationManager.startInstalledAppDetailsActivity(BaseActivityLocation.this);
            }
        }, null);
    }

    @Override
    public void onLocationFetchingFailed(int failureType, ConnectionResult connectionResult) {

    }

    public void initLocationFetching(Activity mActivity) {
        /*3rd Parametr*/
        //For One time Gps Usage: SmartLocationManager.FETCH_LOCATION_ONCE;
        //For every 10 seconds Gps Usage: SmartLocationManager.FETCH_LOCATION_BETWEEN_INTERVAL;

        /*4th parameter*/
        //For All netwrok Provider: SmartLocationManager.ALL_PROVIDERS;
        //For Network Provider only: SmartLocationManager.NETWORK_PROVIDER;
        //For GPS Provider only: SmartLocationManager.GPS_PROVIDER;

        /*Last Parameter*/
        //For using Any Api location: SmartLocationManager.ANY_API; //The priority will be google api.
        //For using google Api location: SmartLocationManager.ONLY_GOOGLE_API;
        //For using android Api Location: SmartLocationManager.ONLY_ANDROID_API;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!SmartLocationManager.checkPermission(BaseActivityLocation.this)) {
                    SmartLocationManager.requestPermission(BaseActivityLocation.this, this);
                } else {
                    mLocationManager = new SmartLocationManager(BaseActivityLocation.this, SmartLocationManager.FETCH_LOCATION_ONCE, this, SmartLocationManager.ALL_PROVIDERS, 2 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_ALL_RESTRICTION, SmartLocationManager.ANY_API); // init location manager
                }
            } else {
                mLocationManager = new SmartLocationManager(BaseActivityLocation.this, SmartLocationManager.FETCH_LOCATION_ONCE, this, SmartLocationManager.ALL_PROVIDERS, 2 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_ALL_RESTRICTION, SmartLocationManager.ANY_API); // init location manager
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void onStart() {
        super.onStart();
        /*if you want fetch in a service make a background thread*/

//        Intent serviceIntent = new Intent(this, LocationFetcherService.class);
//        startService(serviceIntent);
    }

    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        /*if you want fetch in a service make a background thread*/

//        Intent serviceIntent = new Intent(this, LocationFetcherService.class);
//        stopService(serviceIntent);
    }

    protected void onStop() {
        super.onStop();
        if (mLocationManager != null)
            mLocationManager.abortLocationFetching();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    private void requestPermission(String permissionName, int permissionRequestCode) {
        ActivityCompat.requestPermissions(BaseActivityLocation.this, new String[]{permissionName}, permissionRequestCode);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SmartLocationManager.LOCATION_PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    initLocationFetching(this);
                } else {
                    SmartLocationManager.showLocationDeniedDialog(this, SmartLocationManager.LOCATION_DENIED_MESSAGE, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            SmartLocationManager.startInstalledAppDetailsActivity(BaseActivityLocation.this);
                        }
                    }, null);
                }
                break;
        }
    }

    public void startInstalledAppDetailsActivity(final Activity context) {
        if (context == null) {
            return;
        }
        final Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
        context.startActivity(intent);
    }

    public void showLocationPermission() {
        int permissionCheck = ContextCompat.checkSelfPermission(BaseActivityLocation.this, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            requestPermission(Manifest.permission.ACCESS_FINE_LOCATION, REQUEST_FINE_LOCATION);
        } else {
            initLocationFetching(BaseActivityLocation.this);
        }
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }
}