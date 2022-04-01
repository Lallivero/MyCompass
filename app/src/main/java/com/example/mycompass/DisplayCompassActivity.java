package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.TestLooperManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

public class DisplayCompassActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private ImageView compassImage;
    private ConstraintLayout constraintLayout;
    private float angle;
    private Sensor accelerometer;
    private Sensor magneticField;
    private boolean hasVibrated = false;
    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_compass);

        compassImage = (ImageView) findViewById(R.id.compassImage);
        constraintLayout = (ConstraintLayout) findViewById(R.id.compassScreen);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);

    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent == null) {
            return;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            //With filter
            accelerometerValues = Utils.lowPassFilter(sensorEvent.values.clone(), accelerometerValues);
            //Without filter
//            accelerometerValues = sensorEvent.values.clone();
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            //filter
            magneticValues = Utils.lowPassFilter(sensorEvent.values.clone(), magneticValues);
//            magneticValues = sensorEvent.values.clone();
        }
        updateOrientationAngles();
        onNorth();
        gradualNorth();
    }

    private void updateOrientationAngles() {

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues);

        float[] orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float degrees = (float) (Math.toDegrees(orientation[0]) + 360.0f) % 360.0f;
        angle = Math.round(degrees * 100.0f) / 100.0f;
        compassImage.setRotation(angle * -1);
    }

    private void onNorth() {
        if (angle > 345 || angle < 15) {
            if(hasVibrated){
                return;
            }
            vibrate();
            hasVibrated = true;
        }else{
            hasVibrated = false;
        }
    }

    private void gradualNorth() {
        int redSouth = 238;
        int greenSouth = 186;
        int blueSouth = 178;
        int redNorth = 204;
        int greenNorth = 212;
        int blueNorth = 191;
        //normalise angle difference
        float normalisedAngle = Math.abs(angle - 180f) / 180f;
        if (normalisedAngle > 1) {
            normalisedAngle = 1f;
        }

        int red = redSouth - (int) (Math.abs(redSouth - redNorth) * normalisedAngle);
        int green = greenSouth + (int) (Math.abs(greenSouth - greenNorth) * normalisedAngle);
        int blue = blueSouth + (int) (Math.abs(blueSouth - blueNorth) * normalisedAngle);

        String hexRed = Utils.getHex(red);
        String hexGreen = Utils.getHex(green);
        String hexBlue = Utils.getHex(blue);
        String opacity = "#FF";
        constraintLayout.setBackgroundColor(Color.parseColor(opacity+hexRed+hexGreen+hexBlue));
    }

    private void vibrate(){
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(VibrationEffect.createOneShot(300, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(300);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}