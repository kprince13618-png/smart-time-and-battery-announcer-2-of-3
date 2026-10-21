package com.prince.smartannouncer;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = context.getSharedPreferences("SmartAnnouncerPrefs", Context.MODE_PRIVATE);
            boolean isServiceEnabled = prefs.getBoolean("service_enabled", true);

            if (isServiceEnabled) {
                Intent serviceIntent = new Intent(context, AnnouncerService.class);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }
            }
        }
    }
} 2 of 5 In list 5 items
