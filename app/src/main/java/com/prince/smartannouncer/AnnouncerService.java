package com.prince.smartannouncer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import androidx.core.app.NotificationCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AnnouncerService extends Service implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isTtsReady = false;
    private SharedPreferences prefs;
    private Vibrator vibrator;

    private final BroadcastReceiver batteryReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            Intent batteryStatus = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            int level = -1;
            if (batteryStatus != null) {
                level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            }

            if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                if (prefs.getBoolean("charger_alert", true)) {
                    announce("चार्जर कनेक्ट हो गया है। बैटरी " + level + " प्रतिशत है।");
                }
            } else if (Intent.ACTION_POWER_DISCONNECTED.equals(action)) {
                if (prefs.getBoolean("charger_alert", true)) {
                    announce("चार्जर निकाल दिया गया है। बैटरी " + level + " प्रतिशत है।");
                }
            }
        }
    };

    private final BroadcastReceiver timeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_TIME_TICK.equals(intent.getAction())) {
                checkTimeAnnouncement();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("SmartAnnouncerPrefs", MODE_PRIVATE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        tts = new TextToSpeech(this, this);

        startForegroundServiceNotification();

        IntentFilter batteryFilter = new IntentFilter();
        batteryFilter.addAction(Intent.ACTION_POWER_CONNECTED);
        batteryFilter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        registerReceiver(batteryReceiver, batteryFilter);

        registerReceiver(timeReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
    }

    private void checkTimeAnnouncement() {
        if (!prefs.getBoolean("time_alert", true)) return;

        SimpleDateFormat sdfMin = new SimpleDateFormat("mm", Locale.getDefault());
        int minute = Integer.parseInt(sdfMin.format(new Date()));

        int interval = prefs.getInt("time_interval", 60);
        if (minute % interval == 0) {
            SimpleDateFormat sdfHour = new SimpleDateFormat("hh बज कर mm मिनट", new Locale("hi", "IN"));
            String timeText = sdfHour.format(new Date());
            if (minute == 0) {
                SimpleDateFormat sdfExact = new SimpleDateFormat("hh बज गए हैं", new Locale("hi", "IN"));
                timeText = sdfExact.format(new Date());
            }
            announce("समय है " + timeText);
        }
    }

    private void announce(String text) {
        if (!isTtsReady) return;

        // वाइब्रेशन
        if (prefs.getBoolean("vibration_enabled", true) &&  vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(200);
            }
        }

        // अलर्ट टोन
        if (prefs.getBoolean("tone_enabled", true)) {
            try {
                Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), alert);
                r.play();
                Thread.sleep(300);
            } catch (Exception ignored) {}
        }

        // आवाज़ की गति और पिच
        float speed = prefs.getFloat("tts_speed", 1.0f);
        float pitch = prefs.getFloat("tts_pitch", 1.0f);
        tts.setSpeechRate(speed);
        tts.setPitch(pitch);

        // ऑडियो चैनल
        int streamType = prefs.getInt("audio_channel", AudioManager.STREAM_MUSIC);
        Bundle params = new Bundle();
        params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, streamType);

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, "SmartAnnouncerID");
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            tts.setLanguage(new Locale("hi", "IN"));
            isTtsReady = true;
        }
    }

    private void startForegroundServiceNotification() {
        String channelId = "announcer_service";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    channelId, "Announcer Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }

        Notification notification = new NotificationCompat.Builder(this, channelId)
                .setContentTitle("Smart Announcer")
                .setContentText("सेवा चालू है")
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .build();

        startForeground(1, notification);
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(batteryReceiver);
        unregisterReceiver(timeReceiver);
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
     }
}
