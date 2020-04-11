package com.ix.coronavirusapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.UUID;

import pl.bclogic.pulsator4droid.library.PulsatorLayout;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    Handler handler = new Handler();

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothConnectionService mBluetoothConnection;
    BluetoothDevice mBTDevice;


    String phoneNumber = "", macAddress = "", deviceName;
    Boolean connectedStarting = false;

    PulsatorLayout pulsator;
    TextView textViewName;

    SharedPreferences sharedpreferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        TextView toolbarTextView = (TextView) findViewById(R.id.toolbarTextView);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        toolbarTextView.setText("Bluetooth Scanning");

        sharedpreferences = getSharedPreferences(Config.MyPREFERENCES, Context.MODE_PRIVATE);
        phoneNumber = sharedpreferences.getString(Config.phoneNumber, "");
        deviceName = sharedpreferences.getString(Config.deviceName, "");

        String currentDeviceId = sharedpreferences.getString(Config.currentDeviceId, "");
        String deviceId = sharedpreferences.getString(Config.deviceId, "");
        if (!currentDeviceId.equals(deviceId)){
            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putBoolean(Config.verified, false);
            editor.commit();

            Intent intent = new Intent(MainActivity.this, RegisterActivity.class);
            startActivity(intent);
            finish();
        }

        pulsator = (PulsatorLayout) findViewById(R.id.pulsator);
        textViewName = (TextView) findViewById(R.id.name);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBluetoothAdapter.setName(phoneNumber+"/CovidTrace");
        macAddress = mBluetoothAdapter.getAddress();

        textViewName.setText(deviceName);

//        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
//        registerReceiver(mBroadcastReceiver4, filter);

        mBTDevices = new ArrayList<>();

        onOFFBlueTooth();
        updateData.run();
        apiLoader.run();

        FloatingActionButton fab = findViewById(R.id.notificationFab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AlertDialog.Builder builder;
                builder = new AlertDialog.Builder(MainActivity.this);
                builder.setMessage("Are you sure you want to send notification ! ")
                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                dialog.cancel();
                                notifyUsersAPI();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.setTitle("Alert Message");
                alert.show();
            }
        });

        verifyMacAddressAPI();
    }

    public void verifyMacAddressAPI(){
        String phoneNo = "";
        try {
            phoneNo = URLEncoder.encode(phoneNumber, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.verifyMacAddress_URL+"?phoneNumber="+phoneNo+"&macAddress="+macAddress,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
                        JSONObject jsonObject = null;
                        try{
                            jsonObject = new JSONObject(response);
                            success = jsonObject.getBoolean("success");
                            if (!success){
                                Intent i = new Intent(MainActivity.this, RegisterActivity.class);
                                sharedpreferences.edit().remove(Config._id).commit();
                                sharedpreferences.edit().remove(Config.phoneNumber).commit();
                                sharedpreferences.edit().remove(Config.fullName).commit();
                                sharedpreferences.edit().remove(Config.verified).commit();
                                sharedpreferences.edit().remove(Config.macAddress).commit();
                                sharedpreferences.edit().remove(Config.deviceId).commit();
                                startActivity(i);
                                finish();
                            }
                        }catch (JSONException e){ }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { }
                }) {
        };
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public void notifyUsersAPI(){
        final ProgressDialog progressDialog = new ProgressDialog(MainActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.notifyUsers_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
                        String message = "";
                        JSONObject jsonObject = null;

                        try{
                            jsonObject = new JSONObject(response);
                            success = jsonObject.getBoolean("success");
                            message = jsonObject.getString("message");

                            if (success){
                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(message)
                                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.setTitle("Success");
                                alert.show();
                            }

                        }catch (JSONException e){

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage(e.toString())
                                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.setTitle("Error");
                            alert.show();
                        }

                        progressDialog.dismiss();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {

                        NetworkResponse networkResponse = error.networkResponse;
                        if (networkResponse != null && networkResponse.data != null) {
                            String jsonError = new String(networkResponse.data);

                            boolean success = false;
                            String message = "";
                            JSONObject jsonObject = null;

                            try{
                                jsonObject = new JSONObject(jsonError);

                                message = jsonObject.getJSONArray("errors").getJSONObject(0).getString("msg");

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(message)
                                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.setTitle("Error");
                                alert.show();

                            }catch (JSONException e){

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setMessage(e.toString())
                                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.setTitle("Error");
                                alert.show();
                            }

                        }else {

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(MainActivity.this);
                            builder.setMessage("Check your connection and try again ")
                                    .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.setTitle("Error");
                            alert.show();
                        }
                        progressDialog.dismiss();

                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("phoneNumber", phoneNumber);
                return params;
            }
        };
        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }


    public void onOFFBlueTooth(){
        Log.d(TAG, "onClick: on/off bluetooth.");

        if(mBluetoothAdapter == null){
            Log.d(TAG, "enableDisableBT: Does not have BT capabilities.");
        }
        if(!mBluetoothAdapter.isEnabled()){
            Log.d(TAG, "enableDisableBT: enabling BT.");
            Intent enableBTIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBTIntent, 1001);

            IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            registerReceiver(mBroadcastReceiver1, BTIntent);
        }else {
             enableDisableBlueTooth();

             mBluetoothAdapter.disable();
             IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
             registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }
    private Runnable bluetoothName = new Runnable(){
        public void run(){
            BluetoothAdapter myDevice = BluetoothAdapter.getDefaultAdapter();
            myDevice.setName(phoneNumber+"/CovidTrace");
            handler.postDelayed(bluetoothName,9000);
        }
    };
    private final BroadcastReceiver mBroadcastReceiver1 = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(mBluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, mBluetoothAdapter.ERROR);
                switch(state){
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "onReceive: STATE OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE ON");
                        handler.postDelayed(bluetoothName,9000);
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    public void enableDisableBlueTooth (){
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 3600 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 3600);
        startActivityForResult(discoverableIntent, 1000);

        IntentFilter intentFilter = new IntentFilter(mBluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        registerReceiver(mBroadcastReceiver2,intentFilter);
    }
    private final BroadcastReceiver mBroadcastReceiver2 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Able to receive connections.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Disabled. Not able to receive connections.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBroadcastReceiver2: Connecting....");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBroadcastReceiver2: Connected.");
                        break;
                }
            }
        }
    };


    ArrayList<String> bluetoothList = new ArrayList<String>();
    private Runnable updateData = new Runnable(){
        public void run(){
            discoverBlueTooth();
            handler.postDelayed(updateData,9000);
        }
    };
    public void discoverBlueTooth () {
        Log.d(TAG, "btnDiscover: Looking for unpaired devices.");

        if(mBluetoothAdapter.isDiscovering()){
            mBluetoothAdapter.cancelDiscovery();
            Log.d(TAG, "btnDiscover: Canceling discovery.");
            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }

        if(!mBluetoothAdapter.isDiscovering()){
            //check BT permissions in manifest
            checkBTPermissions();

            mBluetoothAdapter.startDiscovery();
            IntentFilter discoverDevicesIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mBroadcastReceiver3, discoverDevicesIntent);
        }
    }
    private BroadcastReceiver mBroadcastReceiver3 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            Log.d(TAG, "onReceive action list:. " + action);

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);

                String deviceName = device.getName();
                if (!deviceName.isEmpty() && deviceName.split("/").length == 2){
                    if (deviceName.split("/")[1].equals("CovidTrace")){
                        bluetoothList.add(deviceName.split("/")[0]);
                    }
                }

                Snackbar snackbar = Snackbar.make(findViewById(R.id.activity_main), device.getName(), Snackbar.LENGTH_LONG);
                snackbar.show();

//                if(device.getAddress().equals("C0:EE:FB:E5:BD:53")) {
//                    Log.d(TAG, "connecting to One Plus");
//
//                    Boolean checkValue = false;
//                    for (int j = 0; j < allBluetoothDevices.size(); j++) {
//                        String macAddress = allBluetoothDevices.get(j).getMacNumber();
//                        if (macAddress.equals(device.getAddress())) {
//                            checkValue = true;
//                        }
//                    }
//                    if (!checkValue) {
//                        uniqueBluetoothDevices.add(device);
//                    }
//                }
//                onItemClick(device);
            }
        }
    };

    String phoneNumberRemove = "", dateRemove = "";
    JSONArray allBluetoothDevices;
    private Runnable apiLoader = new Runnable(){
        public void run(){

            allBluetoothDevices = new JSONArray();
            JSONObject bluetoothDevices;

            String jsonArrayList = sharedpreferences.getString(Config.jsonArrayList, "[]");
            JSONArray jsonArray = null;
            try {
                jsonArray = new JSONArray(jsonArrayList);

                for (int i = 0; i < jsonArray.length(); i++) {

                    String pn = jsonArray.getJSONObject(i).getString("phoneNumber");
                    String d = jsonArray.getJSONObject(i).getString("date");

                    if (!phoneNumberRemove.equals(pn) && !dateRemove.equals(d)){

                        bluetoothDevices = new JSONObject();
                        bluetoothDevices.put("phoneNumber", pn);
                        bluetoothDevices.put("date", d);

                        allBluetoothDevices.put(bluetoothDevices);
                    }

                }

            } catch (JSONException e) {
                e.printStackTrace();
            }

            HashSet<String> hashBluetoothList = new HashSet(bluetoothList);
            for(String phoneNumber : hashBluetoothList){
                try {

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                    String date = simpleDateFormat.format(new Date());

                    bluetoothDevices = new JSONObject();
                    bluetoothDevices.put("phoneNumber", phoneNumber);
                    bluetoothDevices.put("date", date);

                    allBluetoothDevices.put(bluetoothDevices);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            SharedPreferences.Editor editor = sharedpreferences.edit();
            editor.putString(Config.jsonArrayList, allBluetoothDevices.toString());
            editor.commit();

            bluetoothList.clear();


            String jsonArrayJushString = sharedpreferences.getString(Config.jsonArrayList, "[]");
            JSONArray jsonArrayJush = null;
            try {
                jsonArrayJush = new JSONArray(jsonArrayJushString);
                if (jsonArrayJush.length() > 0) {
                    detectionAPI(jsonArrayJush.getJSONObject(0).getString("phoneNumber"), jsonArrayJush.getJSONObject(0).getString("date"));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }

            handler.postDelayed(apiLoader,90000);
        }
    };
    public void detectionAPI(final String receverNumber, final String receverDate){

        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.DETECTION_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
                        JSONObject jsonObject = null;
                        try{
                            jsonObject = new JSONObject(response);
                            success = jsonObject.getBoolean("success");
                            if (success){
                                phoneNumberRemove = receverNumber;
                                dateRemove = receverDate;
                            }
                        }catch (JSONException e){ }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) { }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("sender", phoneNumber);
                params.put("reciever", receverNumber);
                return params;
            }
        };
        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }




    public void onItemClick(final BluetoothDevice device) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mBluetoothAdapter.cancelDiscovery();
                if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){

                    device.createBond();
                    mBTDevice = device;
                    mBluetoothConnection = new BluetoothConnectionService(MainActivity.this);

                    // Start Connection
                    mBluetoothConnection.startClient(mBTDevice,MY_UUID_INSECURE);

                    handler.postDelayed(dataPush,15000);
                }
            }
        }, 30000);
    }
    private Runnable dataPush = new Runnable(){
        public void run(){
            // Data Send
            if (connectedStarting){
                String data = "+923355566778";
                byte[] bytes = data.getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        }
    };


    private final BroadcastReceiver mBroadcastReceiver4 = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if(action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)){
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDED.");
                    mBTDevice = mDevice;
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver: BOND_BONDING.");
                }
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver: BOND_NONE.");
                }
            }
        }
    };


    private void checkBTPermissions() {
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }
        }else{
            Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001){
            enableDisableBlueTooth();
        }
        if (requestCode == 1000){
            pulsator.start();
        }
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();

        mBluetoothAdapter.setName(deviceName);
        mBluetoothAdapter.disable();

        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        // unregisterReceiver(mBroadcastReceiver4);
    }



    public class BluetoothConnectionService {
        private static final String TAG = "BluetoothConnectionServ";

        private static final String appName = "MYAPP";

        private final BluetoothAdapter mBluetoothAdapter;
        Context mContext;

        private AcceptThread mInsecureAcceptThread;

        private ConnectThread mConnectThread;
        private BluetoothDevice mmDevice;
        private UUID deviceUUID;
        ProgressDialog mProgressDialog;

        private ConnectedThread mConnectedThread;

        public BluetoothConnectionService(Context context) {
            mContext = context;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            start();
        }

        private class AcceptThread extends Thread {
            private final BluetoothServerSocket mmServerSocket;
            public AcceptThread(){
                BluetoothServerSocket tmp = null;
                try{
                    tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(appName, MY_UUID_INSECURE);

                    Log.d(TAG, "AcceptThread: Setting up Server using: " + MY_UUID_INSECURE);
                }catch (IOException e){
                    Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
                }
                mmServerSocket = tmp;
            }

            public void run(){
                Log.d(TAG, "run: AcceptThread Running.");
                BluetoothSocket socket = null;
                try{
                    Log.d(TAG, "run: RFCOM server socket start.....");
                    socket = mmServerSocket.accept();
                    Log.d(TAG, "run: RFCOM server socket accepted connection.");
                }catch (IOException e){
                    Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
                }
                if(socket != null){
                    connected(socket,mmDevice);
                }
                Log.i(TAG, "END mAcceptThread ");
            }
            public void cancel() {
                Log.d(TAG, "cancel: Canceling AcceptThread.");
                try {
                    mmServerSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "cancel: Close of AcceptThread ServerSocket failed. " + e.getMessage() );
                }
            }
        }

        private class ConnectThread extends Thread {
            private BluetoothSocket mmSocket;

            public ConnectThread(BluetoothDevice device, UUID uuid) {
                Log.d(TAG, "ConnectThread: started.");
                mmDevice = device;
                deviceUUID = uuid;
            }

            public void run(){
                BluetoothSocket tmp = null;
                Log.i(TAG, "RUN mConnectThread ");
                try {
                    Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: " +MY_UUID_INSECURE );
                    tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
                } catch (IOException e) {
                    Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
                }
                mmSocket = tmp;
                mBluetoothAdapter.cancelDiscovery();
                try {
                    mmSocket.connect();
                    Log.d(TAG, "run: ConnectThread connected.");
                } catch (IOException e) {
                    try {
                        mmSocket.close();
                        Log.d(TAG, "run: Closed Socket.");
                    } catch (IOException e1) {
                        Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                    }
                    Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
                }
                connected(mmSocket,mmDevice);
            }

            public void cancel() {
                try {
                    Log.d(TAG, "cancel: Closing Client Socket.");
                    mmSocket.close();
                } catch (IOException e) {
                    Log.e(TAG, "cancel: close() of mmSocket in Connectthread failed. " + e.getMessage());
                }
            }
        }

        public synchronized void start() {
            Log.d(TAG, "start");
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
            if (mInsecureAcceptThread == null) {
                mInsecureAcceptThread = new AcceptThread();
                mInsecureAcceptThread.start();
            }
        }

        public void startClient(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "startClient: Started.");
            mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth","Please Wait...",true);
            mConnectThread = new ConnectThread(device, uuid);
            mConnectThread.start();
        }



        private class ConnectedThread extends Thread {
            private final BluetoothSocket mmSocket;
            private final InputStream mmInStream;
            private final OutputStream mmOutStream;

            public ConnectedThread(BluetoothSocket socket) {
                Log.d(TAG, "ConnectedThread: Starting.");

                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                //dismiss the progressdialog when connection is established
                try{
                    mProgressDialog.dismiss();
                }catch (NullPointerException e){
                    e.printStackTrace();
                }


                try {
                    tmpIn = mmSocket.getInputStream();
                    tmpOut = mmSocket.getOutputStream();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            }

            public void run(){
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
                while (true) {
                    try {
                        bytes = mmInStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "InputStream: " + incomingMessage);

                        connectedStarting = false;

                    } catch (IOException e) {
                        Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                        break;
                    }
                }
            }

            public void write(byte[] bytes) {
                String text = new String(bytes, Charset.defaultCharset());
                Log.d(TAG, "write: Writing to outputstream: " + text);

                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
                }
            }

            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
        }

        private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
            Log.d(TAG, "connected: Starting.");

            connectedStarting = true;

            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new ConnectedThread(mmSocket);
            mConnectedThread.start();
        }


        public void write(byte[] out) {
            // Create temporary object
            ConnectedThread r;
            // Synchronize a copy of the ConnectedThread
            Log.d(TAG, "write: Write Called.");
            //perform the write
            mConnectedThread.write(out);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_list, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_notification) {
            Intent intent = new Intent(MainActivity.this, NotificationActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
