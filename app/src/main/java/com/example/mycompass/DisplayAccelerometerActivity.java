package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;

public class DisplayAccelerometerActivity extends AppCompatActivity implements SensorEventListener {
    private enum Tilt {
        NONE,
        LEFT,
        RIGHT,
        UPRIGHT,
        UPSIDEDOWN
    }


    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float xValue;
    private float yValue;
    private float zValue;
    private TextView accelX;
    private TextView accelY;
    private TextView accelZ;
    private TextView tiltText;
    private ConstraintLayout constraintLayout;
    private Utils utils;
    private boolean loaded = false;
    private SoundPool soundPool;
    private int sound1;
    private int sound2;
    private int sound3;
    private int sound4;
    private Tilt tilt;
    private Tilt previousTilt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_accelerometer);
        utils = new Utils();
        initialiseViews();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);


    }

    //Initialise the TextViews and give them starter values
    private void initialiseViews() {
        accelX = findViewById(R.id.xValue);
        accelY = findViewById(R.id.yValue);
        accelZ = findViewById(R.id.zValue);
        tiltText = findViewById(R.id.tiltText);
        accelX.setText(getString(R.string.xValue, "0.0"));
        accelY.setText(getString(R.string.yValue, "0.0"));
        accelZ.setText(getString(R.string.zValue, "0.0"));

        constraintLayout = findViewById(R.id.accelerometerLayout);
        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.
                USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build();
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> loaded = true);
//        soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
//            @Override
//            public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
//                loaded = true;
//            }
//        });
        sound1 = soundPool.load(this, R.raw.assets_note1, 1);
        sound2 = soundPool.load(this, R.raw.assets_note2, 1);
        sound3 = soundPool.load(this, R.raw.assets_note3, 1);
        sound4 = soundPool.load(this, R.raw.assets_note4, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        soundPool.release();
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        displayCurrentValues();
//        xValue = utils.lowPassFilter(sensorEvent.values[0]);
//        yValue = utils.lowPassFilter(sensorEvent.values[1]);
//        zValue = utils.lowPassFilter(sensorEvent.values[2]);
        xValue = sensorEvent.values[0];
        yValue = sensorEvent.values[1];
        zValue = sensorEvent.values[2];
        setTilt();
        onTilt();


    }

    private void setTilt() {
        if (tilt != null)
            previousTilt = tilt;
        if (zValue > 8 || zValue < -8) {
            tilt = Tilt.NONE;
        } else if (xValue > 8) {
            tilt = Tilt.LEFT;
        } else if (xValue < -8) {
            tilt = Tilt.RIGHT;
        } else if (yValue > 8) {
            tilt = Tilt.UPRIGHT;
        } else if (yValue < -8) {
            tilt = Tilt.UPSIDEDOWN;
        }

    }

    //Display the current accelerometer values in the respective TextFields
    private void displayCurrentValues() {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        accelX.setText(getString(R.string.xValue, decimalFormat.format(xValue)));
        accelY.setText(getString(R.string.yValue, decimalFormat.format(yValue)));
        accelZ.setText(getString(R.string.zValue, decimalFormat.format(zValue)));
    }

    private void onTilt() {
        if ((tilt != previousTilt && loaded) || previousTilt == null) {
            //Not sure how this works
//            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//            float volume = actualVolume / maxVolume;
            tiltText.setText(getString(R.string.tiltText, tilt));
            switch (tilt) {
                case NONE:
                    constraintLayout.setBackgroundColor(getColor(R.color.vanillaIce));
                    break;
                case LEFT:
                    constraintLayout.setBackgroundColor(getColor(R.color.paleLeaf));
                    soundPool.play(sound1, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case RIGHT:
                    constraintLayout.setBackgroundColor(getColor(R.color.burlyWood));
                    soundPool.play(sound2, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case UPRIGHT:
                    constraintLayout.setBackgroundColor(getColor(R.color.zinnwaldite));
                    soundPool.play(sound3, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case UPSIDEDOWN:
                    constraintLayout.setBackgroundColor(getColor(R.color.ecruWhite));
                    soundPool.play(sound4, 0.05f, 0.05f, 1, 0, 1f);
                    break;

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}