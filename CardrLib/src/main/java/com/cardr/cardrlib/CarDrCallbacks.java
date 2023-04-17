package com.cardr.cardrlib;

public interface CarDrCallbacks {
    public void checkConnectionStatus(String status);
    public void bluetoothStatus(String status);
    public void getCDUser(CDUser cdUser);
    public void scanError(CDError cdError);
    public void scanResponse(CDDeviceResponse cdDeviceResponse);
}
