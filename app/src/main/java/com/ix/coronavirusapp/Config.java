package com.ix.coronavirusapp;

public class Config {

    public static final String URL  = "https://covid-trace-api.azurewebsites.net/";
    public static final String REGISTER_URL = URL + "register";
    public static final String VERIFY_URL = URL + "verify";
    public static final String DETECTION_URL = URL + "detection";
    public static final String verifyMacAddress_URL = URL + "users/verifyMacAddress";
    public static final String notifyUsers_URL = URL + "detection/notifyUsers";
    public static final String notification_URL = URL + "notification";


    public static final String MyPREFERENCES = "MyPrefs" ;

    public static final String _id = "_idKey";
    public static final String phoneNumber = "phoneNumberKey";
    public static final String fullName = "fullNameKey";
    public static final String verified = "verifiedKey";
    public static final String macAddress = "macAddressKey";
    public static final String deviceId = "deviceIdKey";
}
