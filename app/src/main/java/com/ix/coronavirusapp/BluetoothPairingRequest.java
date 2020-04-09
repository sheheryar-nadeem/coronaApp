package com.ix.coronavirusapp;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;

public class BluetoothPairingRequest extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
            // convert broadcast intent into activity intent (same action string)

            BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, BluetoothDevice.ERROR);
            Intent pairingIntent = new Intent();
            pairingIntent.setClass(context, MainActivity.class);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
            pairingIntent.putExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT, type);
            pairingIntent.setAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
            pairingIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);


            if (device != null) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                        device.setPin("1111".getBytes("UTF-8"));
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            context.startActivity(pairingIntent);
        }
    }

}
