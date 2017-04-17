package com.example.raza.locationaware.permission;

import android.Manifest;

/**
 * Created by BrOlLy on 09/03/2017.
 */

public class Enums {

    public static final int REQ_CODE = 0;

    public static final String CAMERA = Manifest.permission.CAMERA;
    public static final String CALL_LOG = Manifest.permission.READ_CALL_LOG;
    public static final String LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    public static final String READ_SMS = Manifest.permission.READ_SMS;
    public static final String READ_CONTACTS = Manifest.permission.READ_CONTACTS;

    public static String cameraMessage = "Camera Permission is Compulsory for Camera usage";
    public static String callLog = "Call log permission is compulsory for accessing call logs";
    public static String locationMessage = "Location permission is compulsory for Accessing location";
    public static String smsMessage = "Sms Permission is compulsory for Read/Write sms";
    public static String contactMessage = "Contacts permission is compulsory for getting contacts details";
}
