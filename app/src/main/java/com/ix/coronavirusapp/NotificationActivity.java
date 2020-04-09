package com.ix.coronavirusapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

public class NotificationActivity extends AppCompatActivity
        implements ConnectivityReceiver.ConnectivityReceiverListener {

    private static final String TAG = "NotificationActivity";

    SharedPreferences sharedpreferences;
    String phoneNumber = "";

    LinearLayout dataLayout;
    LinearLayout networkLayout;
    Button tryButton;
    ListView listView;

    private List<Notification> allNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        listView = (ListView) findViewById(R.id.listView);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channel to show notifications.
            String channelId  = getString(R.string.default_notification_channel_id);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(new NotificationChannel("COVID'19 TRACE" , "Weather", NotificationManager.IMPORTANCE_LOW));
        }

        if (getIntent().getExtras() != null) {
            for (String key : getIntent().getExtras().keySet()) {
                Object value = getIntent().getExtras().get(key);
                Log.d(TAG, "Key: " + key + " Value: " + value);
            }
        }

        sharedpreferences = getSharedPreferences(Config.MyPREFERENCES, Context.MODE_PRIVATE);
        phoneNumber = sharedpreferences.getString(Config.phoneNumber, "");
        try {
            phoneNumber = URLEncoder.encode(phoneNumber, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

//        phoneNumber = "%2B923238226000";


        dataLayout = (LinearLayout) findViewById(R.id.dataLayout);
        networkLayout = (LinearLayout) findViewById(R.id.networkLayout);
        tryButton = (Button) findViewById(R.id.tryButton);

        checkConnection();
        tryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                checkConnection();
                notificationAPI();
            }
        });

        notificationAPI();

    }


    public void notificationAPI(){
        final ProgressDialog progressDialog = new ProgressDialog(NotificationActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Loading...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.GET, Config.notification_URL+"?phoneNumber="+phoneNumber,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
                        String message = "";
                        JSONObject jsonObject = null;
                        JSONArray dataArray = null;

                        try{
                            jsonObject = new JSONObject(response);
                            success = jsonObject.getBoolean("success");

                            if (success){
                                dataArray = jsonObject.getJSONArray("data");

                                allNotifications = new ArrayList<>();
                                Notification notification;

                                for (int i = 0; i < dataArray.length(); i++) {
                                    try {

                                        String date = "";
                                        String time = "";
                                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
                                        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
                                        try {

                                            Date d = sdf.parse(dataArray.getJSONObject(i).getString("date"));
                                            SimpleDateFormat finalFormatDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                            SimpleDateFormat finalFormatTime = new SimpleDateFormat("HH:mm a", Locale.getDefault());

                                            date = finalFormatDate.format(d);
                                            time = finalFormatTime.format(d);

                                        } catch (ParseException ex) {
                                            Log.v("Exception", ex.getLocalizedMessage());
                                        }

                                        notification = new Notification();
                                        notification.setText(dataArray.getJSONObject(i).getString("text"));
                                        notification.setDate(date);
                                        notification.setTime(time);
                                        allNotifications.add(notification);

                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                }
                                listView.setAdapter(new NotificationAdapter(NotificationActivity.this, allNotifications));
                            }

                        }catch (JSONException e){

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(NotificationActivity.this);
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
                                builder = new AlertDialog.Builder(NotificationActivity.this);
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
                                builder = new AlertDialog.Builder(NotificationActivity.this);
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
                            builder = new AlertDialog.Builder(NotificationActivity.this);
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
        };
        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
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
