package com.cardr.cardrlib;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.util.Log;

import java.util.ArrayList;
import java.util.Timer;

import voyomotive.voyolibrary.BasicIdentifier;
import voyomotive.voyolibrary.DiagnosticController;
import voyomotive.voyolibrary.DiagnosticInfo;
import voyomotive.voyolibrary.Enumerations.ConnectionState;
import voyomotive.voyolibrary.Enumerations.LoginState;
import voyomotive.voyolibrary.Enumerations.ScanningState;
import voyomotive.voyolibrary.Enumerations.VoyoError;
import voyomotive.voyolibrary.UserAccountsManager;
import voyomotive.voyolibrary.VoyoAPI;
import voyomotive.voyolibrary.VoyoCallback;
import voyomotive.voyolibrary.VoyoDevice;
import voyomotive.voyolibrary.VoyoDeviceManager;
import voyomotive.voyolibrary.VoyoErrorCallback;
import voyomotive.voyolibrary.VoyoScanPro;
import voyomotive.voyolibrary.VoyoUserAccount;

public class OBDManager {
    private VoyoAPI cdAPI;
    Context context;
    private VoyoDevice carDrDevice = null;

    private  final String TAG = "OBDManager";
    CarDrCallbacks carDrCallbacks;
    boolean isScanning = false;
    Timer timer;
    Timer timer1;
    ArrayList<String> dtcCode = new ArrayList<>();
    Activity activity;
    CountDownTimer countDownTimer;




    public OBDManager(Context context,CarDrCallbacks carDrCallbacks,Activity activity){
        try{
            this.carDrCallbacks = carDrCallbacks;
            this.context = context;
            this.activity = activity;
            initialize();

        } catch( Exception e ){
        }
    }

    private void initialize(){
        cdAPI= VoyoAPI.getInstance(context, Constant.PROD_API);
        onStart();

    }


    public  void createLoginRequest(String username,String password){

        isScanning = false;
        cdAPI.getUserAccountsManager().loginToAccount(username, password, new VoyoErrorCallback() {
            @Override
            public void onNotify(VoyoError result) {
                Log.v(TAG, "Login returned " + result);
                if(result.equals(VoyoError.SUCCESS)){
                    if(carDrCallbacks != null){
                        CDUser cdUser = new CDUser();
                        cdUser.loggedIn = true;
                        cdUser.message = result.getDescription();
                        carDrCallbacks.getCDUser(cdUser);
                    }
                }else{
                    if(carDrCallbacks != null){
                        CDUser cdError = new CDUser();
                        cdError.status = false;
                        cdError.message = "Unable to connect adapter";
                        carDrCallbacks.getCDUser(cdError);
                    }
                }



            }
        });

    }

    private void onStart() {
        // Subscribe to login or account creation success/failure updates:
        cdAPI.getUserAccountsManager().getVoyoUserAccount().loginState.addCallback(mLoginStateCallback);

        // Subscribe to Bluetooth scanning status updates:

        // Subscribe to login server status updates:
        cdAPI.getUserAccountsManager().serverConnectionState.addCallback(mServerConnectionCallback);

        // Subscribe to all existing and new VoyoDevice connection states:
        cdAPI.getVoyoDeviceManager().scanningState.addCallback(mScanStatusCallback);

        cdAPI.getVoyoDeviceManager().allBluetoothConnectionStates.addCallback(mDeviceConnectionCallback);

        SharedPreferences sharedPreferences = activity.getSharedPreferences("Permission", 0);
        if(sharedPreferences.getString("state","").isEmpty()){
            SharedPreferences.Editor edit = sharedPreferences.edit();

            edit.putString("state","yes");
            edit.commit();
            cdAPI.enablePermissions(activity);

        }
        cdAPI.onStart();
        cdAPI.onStartFromService(null);
    }

    public void stopVoyo(){
        if (countDownTimer != null) {
            countDownTimer.cancel();
        }
        cdAPI.getUserAccountsManager().getVoyoUserAccount().loginState.removeCallbacks();
        cdAPI.getVoyoDeviceManager().scanningState.removeCallbacks();
        cdAPI.getVoyoDeviceManager().scanningState.removeParentNotifiers();
        cdAPI.getUserAccountsManager().serverConnectionState.removeCallbacks();
        cdAPI.getVoyoDeviceManager().allBluetoothConnectionStates.removeCallback(mDeviceConnectionCallback);
        cdAPI.getUserAccountsManager().logout();
        cdAPI.onStop();
    }


    // Callback for login status
    private VoyoCallback<LoginState, VoyoUserAccount> mLoginStateCallback = new VoyoCallback<LoginState, VoyoUserAccount>() {
        @Override
        public void onNotify(LoginState state, VoyoUserAccount account) {
            switch( state ){
                case LOGGED_IN:
                    Log.i( TAG, "Login Account logged in (or created) successfully! Username: " + account.getUsername() );
                    break;
                case LOGGED_OUT:
                    Log.i( TAG, "Account logged out!" );
                    break;
            }
        }
    };

    // Callback for server connection state (login thread):
    private VoyoCallback<ConnectionState, UserAccountsManager> mServerConnectionCallback = new VoyoCallback<ConnectionState,UserAccountsManager>() {
        @Override
        public void onNotify(ConnectionState new_state, UserAccountsManager metaResult) {
            // Just update the text box for now.
            switch( new_state ){
                case CONNECTING:
                    if(carDrCallbacks != null){
                        carDrCallbacks.checkConnectionStatus("Connecting to Server");
                    }
                    break;
                case CONNECTED:
                    if(carDrCallbacks != null){
                        carDrCallbacks.checkConnectionStatus("Connected");
                    }
                    break;
                case DISCONNECTED:
                    if(carDrCallbacks != null){
                        carDrCallbacks.checkConnectionStatus("Disconnected");
                    }
                    break;
                case FAILED:
                    if(carDrCallbacks != null){
                        carDrCallbacks.checkConnectionStatus("Failed");
                    }
                default:

                    break;
            }
        }
    };

    // Callback to update Bluetooth scanning status text box:
    private VoyoCallback<ScanningState, VoyoDeviceManager> mScanStatusCallback = new VoyoCallback<ScanningState,VoyoDeviceManager>() {
        @Override
        public void onNotify(ScanningState newState, VoyoDeviceManager metaResult) {
            // If we're already connected to a device, don't care about scanning status:
            if( carDrDevice != null ){
                return;
            }
            // Update the text box:
            update_bluetooth_status(newState.toString());

        }
    };

    private VoyoCallback<ConnectionState, BasicIdentifier> mDeviceConnectionCallback = new VoyoCallback<ConnectionState,BasicIdentifier>() {
        @Override
        public void onNotify(ConnectionState connectionState, BasicIdentifier basicIdentifier) {
            VoyoDevice device = (VoyoDevice)basicIdentifier;
            // If we're already connected to a different device, just ignore it:
            if( carDrDevice != null && !carDrDevice.equals(device) ){
                return;
            }
            // Device's connection state changed. Save the first one to connect as our display device.
            switch( connectionState ) {
                case IN_RANGE:
                    Log.d(TAG,"Detected new (or somebody else's) device: " + device.getDeviceSerialNumber());
                    // Toast.makeText(getApplicationContext(),"Detected new (or somebody else's) device: "+device,Toast.LENGTH_LONG).show();
                    update_bluetooth_status("In Range");

                    break;
                case CONNECTING:
                    update_bluetooth_status("Connecting");
                    break;
                case CONNECTED:
                    // If we are already connected, print a warning but still use it, in case it is a reconnect:

                    if( carDrDevice != null ){
                        Log.w(TAG,"Got CONNECTED for device already connected, oh well.");
                    }else {
                        // Save it and stop scanning:
                        carDrDevice = device;
                        if(carDrCallbacks != null){
                            CDUser cdUser = new CDUser();
                            cdUser.setLoggedIn(true);
                            cdUser.setStatus(true);
                            carDrCallbacks.getCDUser(cdUser);
                        }
                        update_bluetooth_status("Connected");
                        carDrDevice.getDoorControls().setAutoKeyEnabled(false);
                        if(activity != null){
                            activity.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    getDTCs(carDrDevice);
                                }
                            });
                        }

                        cdAPI.getVoyoDeviceManager().stopScanning();
                        // Subscribe to parameter group updates:
                    }
                    break;
                case DISCONNECTED:
                    // If this was a failed connection, change text box back to 'scanning' or whatever
                    if (countDownTimer != null) {
                        countDownTimer.cancel();
                    }
                    if( carDrDevice == null ){
                        update_bluetooth_status( cdAPI.getVoyoDeviceManager().scanningState.get().toString() );
                        return;
                    }

                    cdAPI.getVoyoDeviceManager().startScanning();
                    break;
                case FAILED:
                    update_bluetooth_status( "Error " + device );
                    break;
                default:
                    // Update the text box:
                    update_bluetooth_status( "Error " + device );

                    break;
            }
        }
    };

    private void update_bluetooth_status(final String status){
        if(carDrCallbacks != null){
            carDrCallbacks.bluetoothStatus(status);
        }
        Log.i(TAG, "update_bluetooth_status('"+status+"')");
    }
    private void getDTCs(VoyoDevice device) {
        if(isScanning){
            return;
        }

        isScanning = true;
        device.getDiagnosticControls().deleteScanPro(new VoyoErrorCallback() {
            @Override
            public void onNotify(VoyoError voyoError) {
            }
        }, device.getDiagnosticControls().getLatestScanTime());


//        timer.schedule(
//                new TimerTask(){
//                    @Override
//                    public void run() {
//                                if(device != null){
//                                    scanOBD(device);
//                                }
//
//                    }
//                }, 7000);


        countDownTimer =   new CountDownTimer(100000, 1000) {

            public void onTick(long millisUntilFinished) {
                Log.d(TAG, "seconds remaining: " + millisUntilFinished / 1000);
                //here you can have your logic to set text to edittext
            }

            public void onFinish() {
                if(device != null){

                    scanOBD(device);
                }
            }

        }.start();




    }

    public void scanOBD(VoyoDevice voyoDevices){


        voyoDevices.getDiagnosticControls().activateScanPro(new VoyoCallback<VoyoScanPro, VoyoError>() {
            @Override
            public void onNotify(VoyoScanPro voyoScanPro, VoyoError voyoError) {

                if(voyoError == VoyoError.SUCCESS) {
                    CDDeviceResponse cdDeviceResponse = new CDDeviceResponse();
                    cdDeviceResponse.setVin(voyoScanPro.getVin());
                    ArrayList<CDScanResponse> arrayList = new ArrayList<>();
                    for (DiagnosticController controller : voyoScanPro.getControllerList()) {
                        for (DiagnosticInfo dtc : controller.getDtcList()) {
                            if (!dtcCode.contains(dtc.getDtc_name())) {
                                CDScanResponse cdScanResponse = new CDScanResponse();
                                cdScanResponse.setDtcCode(dtc.getDtc_name());
                                dtcCode.add(dtc.getDtc_name());
                                String finalDesc = "";
                                if(dtc.getShort_description() != null){
                                    finalDesc = dtc.getShort_description().replace("\"","");
                                }
                                cdScanResponse.setDtcDesc(finalDesc);
                                if (dtc.isActive()) {
                                    cdScanResponse.setDtcStatus("Active");
                                } else if (dtc.isPending()) {
                                    cdScanResponse.setDtcStatus("Pending");
                                } else if (dtc.isHistory()) {
                                    cdScanResponse.setDtcStatus("History");
                                } else if (dtc.isLight()) {
                                    cdScanResponse.setDtcStatus("Light");
                                }
                                arrayList.add(cdScanResponse);
                                cdDeviceResponse.setCdScanResponses(arrayList);
                            }
                        }

                    }

                    if(carDrCallbacks != null){
                        carDrCallbacks.scanResponse(cdDeviceResponse);
                    }
                }else{
                    if(carDrCallbacks != null){
                        CDError cdError = new CDError();

                        cdError.status = false;
                        cdError.message = voyoError.getDescription();
                        carDrCallbacks.scanError(cdError);
                    }
                    // isScanning = false;
                }
            }
        });

//        timer1.schedule(
//                new TimerTask(){
//
//                    @Override
//                    public void run(){
//
//                    }
//                }, 3000);
    }

}
