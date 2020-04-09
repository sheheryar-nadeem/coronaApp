package com.ix.coronavirusapp;

public class BluetoothDevices {

    String macNumber;

    public String getMacNumber() {
        return macNumber;
    }

    public void setMacNumber(String macNumber) {
        this.macNumber = macNumber;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    String deviceName;
    String phoneNumber;
    String date;
}
