package com.ix.coronavirusapp;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class TryBluetoothActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    private static final String TAG = "TryBluetoothActivity";
    BluetoothAdapter mBluetoothAdapter;
    public ArrayList<BluetoothDevice> mBTDevices = new ArrayList<>();
    Handler handler = new Handler();

    private static final UUID MY_UUID_INSECURE = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66");
    BluetoothConnectionService mBluetoothConnection;
    BluetoothDevice mBTDevice;

    Button btnStartConnection;
    Button btnSend;
    EditText etSend;

    public DeviceListAdapter mDeviceListAdapter;
    ListView lvNewDevices;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_try_bluetooth);

        lvNewDevices = (ListView) findViewById(R.id.lvNewDevices);
        mBTDevices = new ArrayList<>();

        btnStartConnection = (Button) findViewById(R.id.btnStartConnection);
        btnSend = (Button) findViewById(R.id.btnSend);
        etSend = (EditText) findViewById(R.id.editText);

        lvNewDevices.setOnItemClickListener(TryBluetoothActivity.this);

        btnStartConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mBluetoothConnection.startClient(mBTDevice,MY_UUID_INSECURE);
            }
        });
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                byte[] bytes = etSend.getText().toString().getBytes(Charset.defaultCharset());
                mBluetoothConnection.write(bytes);
            }
        });


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(mBroadcastReceiver4, filter);

        onOFFBlueTooth();
        updateData.run();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        mBluetoothAdapter.cancelDiscovery();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            mBTDevices.get(i).createBond();
            mBTDevice = mBTDevices.get(i);
            mBluetoothConnection = new BluetoothConnectionService(TryBluetoothActivity.this);
        }
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
            // pulsator.start();

            // mBluetoothAdapter.disable();
            // IntentFilter BTIntent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            // registerReceiver(mBroadcastReceiver1, BTIntent);
        }
    }
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
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBroadcastReceiver1: STATE TURNING ON");
                        break;
                }
            }
        }
    };


    public void enableDisableBlueTooth (){
        Log.d(TAG, "btnEnableDisable_Discoverable: Making device discoverable for 300 seconds.");

        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
        startActivity(discoverableIntent);

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
                    //Device is in Discoverable Mode
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBroadcastReceiver2: Discoverability Enabled.");
                        break;
                    //Device not in discoverable mode
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
            Log.d(TAG, "onReceive: ACTION FOUND.");

            if (action.equals(BluetoothDevice.ACTION_FOUND)){
                BluetoothDevice device = intent.getParcelableExtra (BluetoothDevice.EXTRA_DEVICE);
                mBTDevices.add(device);

                onItemClick(device);

                mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, mBTDevices);
                lvNewDevices.setAdapter(mDeviceListAdapter);
            }
        }
    };
    public void onItemClick(BluetoothDevice device) {
        mBluetoothAdapter.cancelDiscovery();
        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN_MR2){
            device.createBond();
            mBTDevice = device;
            mBluetoothConnection = new BluetoothConnectionService(TryBluetoothActivity.this);


            handler.postDelayed(dataPush,9000);
        }
    }
    private Runnable dataPush = new Runnable(){
        public void run(){
            // Data Send
            Toast.makeText(TryBluetoothActivity.this, mBluetoothConnection.deviceUUID.toString(), Toast.LENGTH_SHORT).show();
            String data = "Hello";
            byte[] bytes = data.getBytes(Charset.defaultCharset());
            mBluetoothConnection.write(bytes);

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
            // pulsator.start();
        }
    }
    @Override
    protected void onDestroy() {
        Log.d(TAG, "onDestroy: called.");
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver1);
        unregisterReceiver(mBroadcastReceiver2);
        unregisterReceiver(mBroadcastReceiver3);
        unregisterReceiver(mBroadcastReceiver4);
    }

    public class BluetoothConnectionService {
        private static final String TAG = "BluetoothConnectionServ";

        private static final String appName = "MYAPP";

        private final BluetoothAdapter mBluetoothAdapter;
        Context mContext;

        private BluetoothConnectionService.AcceptThread mInsecureAcceptThread;

        private BluetoothConnectionService.ConnectThread mConnectThread;
        private BluetoothDevice mmDevice;
        private UUID deviceUUID;
        ProgressDialog mProgressDialog;

        private BluetoothConnectionService.ConnectedThread mConnectedThread;

        public BluetoothConnectionService(Context context) {
            mContext = context;
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            start();
        }


        /**
         * This thread runs while listening for incoming connections. It behaves
         * like a server-side client. It runs until a connection is accepted
         * (or until cancelled).
         */
        private class AcceptThread extends Thread {

            // The local server socket
            private final BluetoothServerSocket mmServerSocket;

            public AcceptThread(){
                BluetoothServerSocket tmp = null;

                // Create a new listening server socket
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
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    Log.d(TAG, "run: RFCOM server socket start.....");

                    socket = mmServerSocket.accept();

                    Log.d(TAG, "run: RFCOM server socket accepted connection.");

                }catch (IOException e){
                    Log.e(TAG, "AcceptThread: IOException: " + e.getMessage() );
                }

                //talk about this is in the 3rd
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

        /**
         * This thread runs while attempting to make an outgoing connection
         * with a device. It runs straight through; the connection either
         * succeeds or fails.
         */
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

                // Get a BluetoothSocket for a connection with the
                // given BluetoothDevice
                try {
                    Log.d(TAG, "ConnectThread: Trying to create InsecureRfcommSocket using UUID: "
                            +MY_UUID_INSECURE );
                    tmp = mmDevice.createRfcommSocketToServiceRecord(deviceUUID);
                } catch (IOException e) {
                    Log.e(TAG, "ConnectThread: Could not create InsecureRfcommSocket " + e.getMessage());
                }

                mmSocket = tmp;

                // Always cancel discovery because it will slow down a connection
                mBluetoothAdapter.cancelDiscovery();

                // Make a connection to the BluetoothSocket

                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    mmSocket.connect();

                    Log.d(TAG, "run: ConnectThread connected.");
                } catch (IOException e) {
                    // Close the socket
                    try {
                        mmSocket.close();
                        Log.d(TAG, "run: Closed Socket.");
                    } catch (IOException e1) {
                        Log.e(TAG, "mConnectThread: run: Unable to close connection in socket " + e1.getMessage());
                    }
                    Log.d(TAG, "run: ConnectThread: Could not connect to UUID: " + MY_UUID_INSECURE );
                }

                //will talk about this in the 3rd video
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



        /**
         * Start the chat service. Specifically start AcceptThread to begin a
         * session in listening (server) mode. Called by the Activity onResume()
         */
        public synchronized void start() {
            Log.d(TAG, "start");

            // Cancel any thread attempting to make a connection
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
            if (mInsecureAcceptThread == null) {
                mInsecureAcceptThread = new BluetoothConnectionService.AcceptThread();
                mInsecureAcceptThread.start();
            }
        }

        /**

         AcceptThread starts and sits waiting for a connection.
         Then ConnectThread starts and attempts to make a connection with the other devices AcceptThread.
         **/

        public void startClient(BluetoothDevice device, UUID uuid){
            Log.d(TAG, "startClient: Started.");

            //initprogress dialog
            mProgressDialog = ProgressDialog.show(mContext,"Connecting Bluetooth"
                    ,"Please Wait...",true);

            mConnectThread = new BluetoothConnectionService.ConnectThread(device, uuid);
            mConnectThread.start();
        }

        /**
         Finally the ConnectedThread which is responsible for maintaining the BTConnection, Sending the data, and
         receiving incoming data through input/output streams respectively.
         **/
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

                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    // Read from the InputStream
                    try {
                        bytes = mmInStream.read(buffer);
                        String incomingMessage = new String(buffer, 0, bytes);
                        Log.d(TAG, "InputStream: " + incomingMessage);
                    } catch (IOException e) {
                        Log.e(TAG, "write: Error reading Input Stream. " + e.getMessage() );
                        break;
                    }
                }
            }

            //Call this from the main activity to send data to the remote device
            public void write(byte[] bytes) {
                String text = new String(bytes, Charset.defaultCharset());
                Log.d(TAG, "write: Writing to outputstream: " + text);
                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {
                    Log.e(TAG, "write: Error writing to output stream. " + e.getMessage() );
                }
            }

            /* Call this from the main activity to shutdown the connection */
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
        }

        private void connected(BluetoothSocket mmSocket, BluetoothDevice mmDevice) {
            Log.d(TAG, "connected: Starting.");

            // Start the thread to manage the connection and perform transmissions
            mConnectedThread = new BluetoothConnectionService.ConnectedThread(mmSocket);
            mConnectedThread.start();
        }

        /**
         * Write to the ConnectedThread in an unsynchronized manner
         *
         * @param out The bytes to write
         * @see BluetoothConnectionService.ConnectedThread#write(byte[])
         */
        public void write(byte[] out) {
            // Create temporary object
            BluetoothConnectionService.ConnectedThread r;

            // Synchronize a copy of the ConnectedThread
            Log.d(TAG, "write: Write Called.");
            //perform the write
            mConnectedThread.write(out);
        }
    }
}