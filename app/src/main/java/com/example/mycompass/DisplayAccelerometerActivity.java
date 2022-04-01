package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.widget.TextView;

public class DisplayAccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    private enum Tilt {
        FACEUP,
        LEFT,
        RIGHT,
        UPRIGHT,
        UPSIDEDOWN
    }

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float[] filteredValues;
    private float xValue;
    private float yValue;
    private float zValue;
    private TextView accelX;
    private TextView accelY;
    private TextView accelZ;
    private TextView tiltText;
    private ConstraintLayout constraintLayout;

    private boolean soundLoaded = false;
    private SoundPool soundPool;
    private int sound1;
    private int sound2;
    private int sound3;
    private int sound4;
    private Tilt tilt;
    private Tilt previousTilt;
    private float gradientValue;
    private final float GRAVITY_CONSTANT = 9.82f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_accelerometer);

        initialiseViews();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);


    }

    //Initialise the TextViews and give them starter values, also initiates sound resources and soundPool
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
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundLoaded = true);

        sound1 = soundPool.load(this, R.raw.assets_note1, 1);
        sound2 = soundPool.load(this, R.raw.assets_note2, 1);
        sound3 = soundPool.load(this, R.raw.assets_note3, 1);
        sound4 = soundPool.load(this, R.raw.assets_note4, 1);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        soundPool.release();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        displayCurrentValues();
        filteredValues = Utils.lowPassFilter(sensorEvent.values.clone(), filteredValues);
        xValue = filteredValues[0];
        yValue = filteredValues[1];
        zValue = filteredValues[2];


        setTilt();
        tiltColour();
        playSound();


    }

    //gradual colour test
    private void calculateGradientValue() {
        gradientValue = (float) Math.sqrt(Math.pow(xValue, 2) + Math.pow(yValue, 2));
        gradientValue = gradientValue / GRAVITY_CONSTANT;

        if (gradientValue > 1)
            gradientValue = 1f;

    }

    //Works, but boring. Only changes opacity of background based on xy vector
    private void setGradualColorMono() {
        int i = (int) (gradientValue * 255);
        String hexaDecimal;

        if (i < 16) {
            hexaDecimal = "0" + Integer.toHexString(i);
        } else {
            hexaDecimal = Integer.toHexString(i);
        }

        constraintLayout.setBackgroundColor(Color.parseColor("#" + hexaDecimal + "CCD4BF"));
    }

    //Changes background colour based on tilt, x controls red, y green and z blue
    private void tiltColour() {
        int lowestValue = 75;
        int maxValue = 255;
        int x = calculateRGB(xValue, maxValue, lowestValue);
        int y = calculateRGB(yValue, maxValue, lowestValue);
        int z = calculateRGB(zValue, maxValue, lowestValue);
        String xHex = Utils.getHex(x);
        String yHex = Utils.getHex(y);
        String zHex = Utils.getHex(z);
        String opacity = "#FF";
        constraintLayout.setBackgroundColor(Color.parseColor(opacity + xHex + yHex + zHex));


    }

    //calculates the RGB value between max and lowest for a leaning x,y or z
    private int calculateRGB(float value, int maxValue, int lowestValue) {

        float normalisedValue = Math.abs(value) / GRAVITY_CONSTANT;
        if (normalisedValue >= 1) {
            return maxValue;
        }

        return (Math.max((int) (normalisedValue * maxValue), lowestValue));
    }


    //Sets the tilt depending on the tilt value
    private void setTilt() {
        float tiltValue = 3f;
        if (tilt != null)
            previousTilt = tilt;
        if (xValue > tiltValue && xValue > Math.abs(yValue)) {
            tilt = Tilt.LEFT;
        } else if (xValue < -tiltValue && Math.abs(xValue) > Math.abs(yValue)) {
            tilt = Tilt.RIGHT;
        } else if (yValue > tiltValue && yValue > Math.abs(xValue)) {
            tilt = Tilt.UPRIGHT;
        } else if (yValue < -tiltValue && Math.abs(yValue) > Math.abs(xValue)) {
            tilt = Tilt.UPSIDEDOWN;
        } else {
            tilt = Tilt.FACEUP;
        }

    }

    //Display the current accelerometer values in the respective TextFields
    private void displayCurrentValues() {
        String decimalPattern = "0.00";
        accelX.setText(getString(R.string.xValue, Utils.decimalFormat(xValue, decimalPattern)));
        accelY.setText(getString(R.string.yValue, Utils.decimalFormat(yValue, decimalPattern)));
        accelZ.setText(getString(R.string.zValue, Utils.decimalFormat(zValue, decimalPattern)));
    }

    //Used to change colour, now it just plays sound
    private void playSound() {
        if ((tilt != previousTilt && soundLoaded) || previousTilt == null) {
            //This solutions doesn't seem to work yet, using fixed volume for now
//            AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
//            float actualVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
//            float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
//            float volume = actualVolume / maxVolume;
            tiltText.setText(getString(R.string.tiltText, tilt));
            switch (tilt) {
                case FACEUP:
                    //constraintLayout.setBackgroundColor(getColor(R.color.vanillaIce));
                    break;
                case LEFT:
                    //constraintLayout.setBackgroundColor(getColor(R.color.paleLeaf));
                    soundPool.play(sound1, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case RIGHT:
                    //constraintLayout.setBackgroundColor(getColor(R.color.burlyWood));
                    soundPool.play(sound3, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case UPRIGHT:
                    //constraintLayout.setBackgroundColor(getColor(R.color.zinnwaldite));
                    soundPool.play(sound2, 0.05f, 0.05f, 1, 0, 1f);
                    break;
                case UPSIDEDOWN:
                    //constraintLayout.setBackgroundColor(getColor(R.color.ecruWhite));
                    soundPool.play(sound4, 0.05f, 0.05f, 1, 0, 1f);
                    break;

            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}