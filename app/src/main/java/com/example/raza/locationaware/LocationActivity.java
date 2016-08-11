package com.example.raza.locationaware;

import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.example.raza.locationaware.location.LocationManagerInterface;
import com.example.raza.locationaware.location.SmartLocationManager;
import com.google.android.gms.location.LocationRequest;

public class LocationActivity extends AppCompatActivity implements LocationManagerInterface {

    public static final String TAG = LocationActivity.class.getSimpleName();

    SmartLocationManager mLocationManager;
    TextView mLocalTV, mLocationProviderTV, mlocationTimeTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        mLocationManager = new SmartLocationManager(getApplicationContext(), this, this, SmartLocationManager.ALL_PROVIDERS, LocationRequest.PRIORITY_HIGH_ACCURACY, 10 * 1000, 1 * 1000, SmartLocationManager.LOCATION_PROVIDER_RESTRICTION_NONE); // init location manager
        mLocalTV = (TextView) findViewById(R.id.locationDisplayTV);
        mLocationProviderTV = (TextView) findViewById(R.id.locationProviderTV);
        mlocationTimeTV = (TextView) findViewById(R.id.locationTimeFetchedTV);
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

    @Override
    public void locationFetched(Location mLocal, Location oldLocation, String time, String locationProvider) {
        Toast.makeText(getApplication(), "Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude(), Toast.LENGTH_LONG).show();
        mLocalTV.setText("Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude());
        mLocationProviderTV.setText(locationProvider);
        mlocationTimeTV.setText(time);
    }
}
