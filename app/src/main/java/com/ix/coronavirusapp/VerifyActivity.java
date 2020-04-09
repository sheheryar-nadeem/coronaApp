package com.ix.coronavirusapp;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.chaos.view.PinView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class VerifyActivity extends AppCompatActivity {

    String phoneNumber, code;

    SharedPreferences sharedpreferences;

    PinView pinViewCode;
    Button bt_verify;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify);


        sharedpreferences = getSharedPreferences(Config.MyPREFERENCES, Context.MODE_PRIVATE);
        phoneNumber = sharedpreferences.getString(Config.phoneNumber, "");

        pinViewCode = (PinView) findViewById(R.id.pinViewCode);

        bt_verify = (Button) findViewById(R.id.bt_verify);
        bt_verify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                VerifyCodeAPI();
            }
        });
    }

    public void VerifyCodeAPI(){
        if (!validate()){
            return;
        }

        final ProgressDialog progressDialog = new ProgressDialog(VerifyActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Authenticating...");
        progressDialog.show();

        StringRequest stringRequest = new StringRequest(Request.Method.POST, Config.VERIFY_URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        boolean success = false;
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

                                Intent intent = new Intent(VerifyActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }else {
                                String message = jsonObject.getString("message");

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(VerifyActivity.this);
                                builder.setMessage(message)
                                        .setNegativeButton("OK", new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.setTitle("Error");
                                alert.show();
                            }
                        }catch (JSONException e){

                            AlertDialog.Builder builder;
                            builder = new AlertDialog.Builder(VerifyActivity.this);
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

                                message = jsonObject.getString("message");

                                AlertDialog.Builder builder;
                                builder = new AlertDialog.Builder(VerifyActivity.this);
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
                                builder = new AlertDialog.Builder(VerifyActivity.this);
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
                            builder = new AlertDialog.Builder(VerifyActivity.this);
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
                params.put("code", code);
                params.put("phoneNumber", phoneNumber);
                return params;
            }
        };
        //Adding the string request to the queue
        RequestQueue requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(stringRequest);
    }

    public boolean validate() {
        boolean valid = true;

        code = pinViewCode.getText().toString();

        if (code.isEmpty() || code.length() < 6) {
            pinViewCode.setError("Please Vaild PIN Code");
            valid = false;
        } else {
            pinViewCode.setError(null);
        }

        return valid;
    }
}
