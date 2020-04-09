package com.ix.coronavirusapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.NetworkInterface;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener {

    private static final String TAG = "RegisterActivity";
    LinearLayout dataLayout;
    LinearLayout networkLayout;
    Button tryButton;
    Button buttonNext;

    String phoneNumber = "", macAddress = "", deviceId = "", fullName = "";
    EditText editTextPhoneNumber, editTextFullName;

    SharedPreferences sharedpreferences;

    BluetoothAdapter mBluetoothAdapter;
    public static final String SECURE_SETTINGS_BLUETOOTH_ADDRESS = "bluetooth_address";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        sharedpreferences = getSharedPreferences(Config.MyPREFERENCES, Context.MODE_PRIVATE);
        Boolean status = sharedpreferences.getBoolean(Config.verified, false);
        if (status) {
            Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        dataLayout = (LinearLayout) findViewById(R.id.dataLayout);
        networkLayout = (LinearLayout) findViewById(R.id.networkLayout);
        tryButton = (Button) findViewById(R.id.tryButton);

        checkConnection();
        tryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConnection();
            }
        });


        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        macAddress = mBluetoothAdapter.getAddress();


        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (!task.isSuccessful()) { return; }
                deviceId = task.getResult().getToken();
            }
        });

        editTextFullName = (EditText) findViewById(R.id.editTextFullName);
        editTextPhoneNumber = (EditText) findViewById(R.id.editTextPhoneNumber);
        buttonNext = (Button) findViewById(R.id.buttonSubmit);

        buttonNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register();
            }
        });
    }

    public static String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    String hex = Integer.toHexString(b & 0xFF);
                    if (hex.length() == 1)
                        hex = "0".concat(hex);
                    res1.append(hex.concat(":"));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) {
        }
        return "";
    }

    public void register(){
        if (!validate()){
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(RegisterActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.REGISTER_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
                        String message = "";
                        JSONObject jsonObject = null;
                        JSONObject dataObject = null;

                        try{
                            jsonObject = new JSONObject(response);
                            success = jsonObject.getBoolean("success");

                            if (success){
                                dataObject = jsonObject.getJSONObject("data");

                                SharedPreferences.Editor editor = sharedpreferences.edit();
                                editor.putString(Config._id, dataObject.getString("_id"));
                                editor.putString(Config.phoneNumber, dataObject.getString("phoneNumber"));
                                editor.putString(Config.macAddress, dataObject.getString("macAddress"));
                                editor.putString(Config.deviceId, dataObject.getString("deviceId"));
                                editor.putString(Config.fullName, dataObject.getString("fullName"));
                                editor.putBoolean(Config.verified, dataObject.getBoolean("verified"));
                                editor.commit();

                                Intent intent = new Intent(RegisterActivity.this, VerifyActivity.class);
                                startActivity(intent);
                                finish();
                            }

                        }catch (JSONException e){

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(RegisterActivity.this);
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

                                message = jsonObject.getJSONArray("errors").getJSONObject(0).getString("message");

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(RegisterActivity.this);
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
                                builder = new AlertDialog.Builder(RegisterActivity.this);
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
                            builder = new AlertDialog.Builder(RegisterActivity.this);
                            builder.setMessage("Sorry! Not connected to internet")
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
                params.put("fullName", fullName);
                params.put("phoneNumber", phoneNumber);
                params.put("macAddress", macAddress);
                params.put("deviceId", deviceId);
                return params;
            }
        };
        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public boolean validate() {
        boolean valid = true;

        fullName = editTextFullName.getText().toString();
        phoneNumber = editTextPhoneNumber.getText().toString();

        if (fullName.isEmpty()) {
            editTextFullName.setError("Please Enter FullName");
            valid = false;
        } else {
            editTextFullName.setError(null);
        }

        String regexStr = "^[+][9][2][0-9]{10,13}$";
        if(editTextPhoneNumber.getText().toString().length()<10 || phoneNumber.length()>13 || phoneNumber.matches(regexStr)==false  ) {
            editTextPhoneNumber.setError("Please Enter a vaild Phone Number");
            valid = false;
        } else {
            editTextPhoneNumber.setError(null);
        }

        return valid;
    }

    private void checkConnection() {
        boolean isConnected = ConnectivityReceiver.isConnected();
        showSnack(isConnected);
    }
    private void showSnack(boolean isConnected) {
        String message;
        if (isConnected) {
            dataLayout.setVisibility(View.VISIBLE);
            networkLayout.setVisibility(View.GONE);
            message = "Good! Connected to Internet";
        } else {
            dataLayout.setVisibility(View.GONE);
            networkLayout.setVisibility(View.VISIBLE);

             message = "Sorry! Not connected to internet";
            // Snackbar snackbar = Snackbar.make(findViewById(R.id.registerScreen), message, Snackbar.LENGTH_LONG);
            // snackbar.show();
        };
    }
    @Override
    protected void onResume() {
        super.onResume();
        checkConnection();
        MyApplication.getInstance().setConnectivityListener(this);
    }
    @Override
    public void onNetworkConnectionChanged(boolean isConnected) {
        showSnack(isConnected);
    }
}
