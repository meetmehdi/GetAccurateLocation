package com.example.raza.locationaware.activity;

import android.content.Context;
import android.location.Location;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.example.raza.locationaware.BaseActivityLocation;
import com.example.raza.locationaware.R;

public class LocationActivity extends BaseActivityLocation {

    public static final String TAG = LocationActivity.class.getSimpleName();
    TextView mLocalTV, mLocationProviderTV, mlocationTimeTV;
    public Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location);
        context = getApplicationContext();
        //initLocationFetching(LocationActivity.this, context);
        initLocationFetching(LocationActivity.this);
        initViews();
    }

    private void initViews() {
        mLocalTV = (TextView) findViewById(R.id.locationDisplayTV);
        mLocationProviderTV = (TextView) findViewById(R.id.locationProviderTV);
        mlocationTimeTV = (TextView) findViewById(R.id.locationTimeFetchedTV);
    }

    @Override
    public void locationFetched(Location mLocal, Location oldLocation, String time, String locationProvider) {
        super.locationFetched(mLocal, oldLocation, time, locationProvider);
        Toast.makeText(getApplication(), "Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude(), Toast.LENGTH_SHORT).show();
        if(mLocal.getAltitude() == 0.0 && mLocal.getLongitude() == 0.0){
            Toast.makeText(context, R.string.not_found, Toast.LENGTH_SHORT).show();
        }else{
            mLocalTV.setText("Lat : " + mLocal.getLatitude() + " Lng : " + mLocal.getLongitude());
        }
        mLocationProviderTV.setText(locationProvider);
        mlocationTimeTV.setText(time);
    }

}
