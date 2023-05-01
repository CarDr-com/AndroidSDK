package com.cardr.cardrlib;

import org.json.JSONObject;

public interface CarDrCallbacks {
    public void checkConnectionStatus(String status);
    public void bluetoothStatus(String status);
    public void getCDUser(CDUser cdUser);
    public void scanError(CDError cdError);
    public void scanResponse(CDDeviceResponse cdDeviceResponse);
    public void timeRemaning(String timeInSec);

    public void getPCFResponse(JSONObject jsonObject,String status,String error);

}
