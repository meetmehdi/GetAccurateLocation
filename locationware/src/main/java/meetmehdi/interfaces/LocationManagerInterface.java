package meetmehdi.interfaces;

import android.location.Location;

import com.google.android.gms.common.ConnectionResult;

/**
 * Created by Syed Raza Mehdi Naqvi on 8/10/2016.
 */
public interface LocationManagerInterface {

    void locationFetched(Location mLocation, Location oldLocation, String time, String locationProvider);

    void onLocationFetchingFailed(int failureType, ConnectionResult connectionResult);

    void onLocationNotEnabled(String message);

    void onPermissionDenied(String message);
}
