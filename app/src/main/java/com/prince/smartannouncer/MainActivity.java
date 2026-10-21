package com.prince.smartannouncer;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        prefs = getSharedPreferences("SmartAnnouncerPrefs", MODE_PRIVATE);

        SwitchCompat switchService = findViewById(R.id.switchService);
        SwitchCompat switchCharger = findViewById(R.id.switchCharger);
        SwitchCompat switchTime = findViewById(R.id.switchTime);
        SwitchCompat switchVibration = findViewById(R.id.switchVibration);
        SwitchCompat switchTone = findViewById(R.id.switchTone);

        SeekBar seekSpeed = findViewById(R.id.seekSpeed);
        SeekBar seekPitch = findViewById(R.id.seekPitch);
        RadioGroup groupChannel = findViewById(R.id.groupAudioChannel);

        switchCharger.setChecked(prefs.getBoolean("charger_alert", true));
        switchTime.setChecked(prefs.getBoolean("time_alert", true));
        switchVibration.setChecked(prefs.getBoolean("vibration_enabled", true));
        switchTone.setChecked(prefs.getBoolean("tone_enabled", true));

        int currentChannel = prefs.getInt("audio_channel", AudioManager.STREAM_MUSIC);
        if (currentChannel == AudioManager.STREAM_ALARM) {
            groupChannel.check(R.id.radioAlarm);
        } else if (currentChannel == AudioManager.STREAM_NOTIFICATION) {
            groupChannel.check(R.id.radioNotification);
        } else {
            groupChannel.check(R.id.radioMedia);
        }

        seekSpeed.setProgress((int) (prefs.getFloat("tts_speed", 1.0f) * 10));
        seekPitch.setProgress((int) (prefs.getFloat("tts_pitch", 1.0f) * 10));

        switchCharger.setOnCheckedChangeListener((btn, isChecked) -> 
            prefs.edit().putBoolean("charger_alert", isChecked).apply());

        switchTime.setOnCheckedChangeListener((btn, isChecked) -> 
            prefs.edit().putBoolean("time_alert", isChecked).apply());

        switchVibration.setOnCheckedChangeListener((btn, isChecked) -> 
            prefs.edit().putBoolean("vibration_enabled", isChecked).apply());

        switchTone.setOnCheckedChangeListener((btn, isChecked) -> 
            prefs.edit().putBoolean("tone_enabled", isChecked).apply());

        seekSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float speed = Math.max(0.5f, progress / 10.0f);
                prefs.edit().putFloat("tts_speed", speed).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        seekPitch.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float pitch = Math.max(0.5f, progress / 10.0f);
                prefs.edit().putFloat("tts_pitch", pitch).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        groupChannel.setOnCheckedChangeListener((group, checkedId) -> {
            int channel = AudioManager.STREAM_MUSIC;
            if (checkedId == R.id.radioAlarm) {
                channel = AudioManager.STREAM_ALARM;
            } else if (checkedId == R.id.radioNotification) {
                channel =  AudioManager.STREAM_NOTIFICATION;
            }
            prefs.edit().putInt("audio_channel", channel).apply();
        });

        switchService.setOnCheckedChangeListener((btn, isChecked) -> {
            Intent serviceIntent = new Intent(this, AnnouncerService.class);
            if (isChecked) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    startForegroundService(serviceIntent);
                } else {
                    startService(serviceIntent);
                }
            } else {
                stopService(serviceIntent);
            }
        });
     }
}
