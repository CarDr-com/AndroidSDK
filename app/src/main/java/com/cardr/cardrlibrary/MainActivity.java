package com.cardr.cardrlibrary;

import androidx.appcompat.app.AppCompatActivity;

import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;

import com.cardr.cardrlib.CDDeviceResponse;
import com.cardr.cardrlib.CDError;
import com.cardr.cardrlib.CDUser;
import com.cardr.cardrlib.CarDrCallbacks;
import com.cardr.cardrlib.OBDManager;

import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements CarDrCallbacks{


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OBDManager obdManager = new OBDManager(this, this,this);
        //u0413;b00a0;p2610;u0100;u3000;u0140;p0157;u0415;b11d8;p164d;b1d03;u0121
        ArrayList<String> strings  =  new ArrayList<>();
        strings.add("u0413");
        strings.add("b00a0");
        strings.add("p2610");
        strings.add("u0100");
        strings.add("u3000");
        strings.add("u0140");
        strings.add("u0415");
        strings.add("b11d8");
        strings.add("p164d");
        strings.add("b1d03");
        //vin=&dtcCode=u0413;b00a0;p2610;u0100;u3000;u0140;p0157;u0415;b11d8;p164d;b1d03;u0121
        obdManager.getPCF("A4VSNMhXJN","9A8E222B15B3FAE9C365E48629613","1FM5K8GTXHGA58694",strings,this);


    }

    @Override
    public void checkConnectionStatus(String status) {

    }

    @Override
    public void bluetoothStatus(String status) {

    }

    @Override
    public void getCDUser(CDUser cdUser) {

    }

    @Override
    public void scanError(CDError cdError) {

    }

    @Override
    public void scanResponse(CDDeviceResponse cdDeviceResponse) {

    }

    @Override
    public void timeRemaning(String timeInSec) {

    }

    @Override
    public void getPCFResponse(JSONObject jsonObject, String status,String error) {
        System.out.println(jsonObject);

    }
}