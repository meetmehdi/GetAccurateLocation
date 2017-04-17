package com.example.raza.locationaware.permission;

import java.io.Serializable;

/**
 * Created by Raza on 12/23/16.
 */

public interface PermissionManagerInterface extends Serializable
{
    String TAG = PermissionManagerInterface.class.getSimpleName();

    void onPermissionGranted(String message, int requestCode);

    void onPermissionDenied(String message, int requestCode);

    void isAllGranted(boolean flag);
}
