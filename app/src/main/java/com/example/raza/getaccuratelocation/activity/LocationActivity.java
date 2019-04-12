package com.example.raza.getaccuratelocation.activity;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.raza.getaccuratelocation.GetAccurateLocationApplication;
import com.example.raza.getaccuratelocation.R;

import meetmehdi.location.BaseActivityLocation;

public class LocationActivity extends BaseActivityLocation {
    private final String TAG = LocationActivity.class.getSimpleName();

    private Context context;
    private TextView mLocalTV, mLocationProviderTV, mLocationTimeTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);

        context = getApplicationContext();

        initViews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        initLocationFetching(LocationActivity.this);
    }

    private void initViews() {
        mLocalTV = (TextView) findViewById(R.id.locationDisplayTV);
        mLocationProviderTV = (TextView) findViewById(R.id.locationProviderTV);
        mLocationTimeTV = (TextView) findViewById(R.id.locationTimeFetchedTV);
    }

    @Override
    public void locationFetched(Location mLocal, Location oldLocation, String time, String locationProvider) {
        super.locationFetched(mLocal, oldLocation, time, locationProvider);

        GetAccurateLocationApplication.mCurrentLocation = getLocation();

        GetAccurateLocationApplication.locationTime = time;
        GetAccurateLocationApplication.oldLocation = oldLocation;
        GetAccurateLocationApplication.locationProvider = locationProvider;


        Toast.makeText(getApplication(), "Latitude : " + mLocal.getLatitude() + " Longitude : " + mLocal.getLongitude(), Toast.LENGTH_SHORT).show();
        if (mLocal.getAltitude() == 0.0 && mLocal.getLongitude() == 0.0) {
            Toast.makeText(context, R.string.not_found, Toast.LENGTH_SHORT).show();
        } else {
            mLocalTV.setText("Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude());
        }
        mLocationProviderTV.setText(locationProvider);
        mLocationTimeTV.setText(time);
    }

}
