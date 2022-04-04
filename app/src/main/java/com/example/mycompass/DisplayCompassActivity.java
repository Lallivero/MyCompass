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
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.widget.ImageView;

public class DisplayCompassActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private ImageView compassImage;
    private ConstraintLayout constraintLayout;
    private float angle;
    private Sensor accelerometer;
    private Sensor magneticField;
    private boolean hasVibrated = true;
    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientationAngles = new float[3];
    private int vibrationDuration = 300;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_compass);

        compassImage = findViewById(R.id.compassImage);
        constraintLayout = findViewById(R.id.compassScreen);

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
    protected void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {

        if (sensorEvent == null) {
            return;
        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            accelerometerValues = Utils.lowPassFilter(sensorEvent.values.clone(), accelerometerValues);

        } else if (sensorEvent.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {

            magneticValues = Utils.lowPassFilter(sensorEvent.values.clone(), magneticValues);

        }
        updateOrientationAngles();
        onNorth();
        gradualNorthColour();
    }

    //Provides the angle of the phone from the northern line
    private void updateOrientationAngles() {

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues);

        float[] orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float degrees = (float) (Math.toDegrees(orientation[0]) + 360.0f) % 360.0f;
        angle = Math.round(degrees * 100.0f) / 100.0f;
        compassImage.setRotation(angle * -1);
    }

    //Vibrates if pointing north
    private void onNorth() {
        if (angle > 345 || angle < 15) {
            if (hasVibrated) {
                return;
            }
            vibrate(vibrationDuration);
            hasVibrated = true;
        } else {
            hasVibrated = false;
        }
    }

    //Changes from a colour while pointing north to another when pointing south gradually
    private void gradualNorthColour() {
        int redSouth = 238;
        int greenSouth = 186;
        int blueSouth = 178;
        int redNorth = 204;
        int greenNorth = 230;
        int blueNorth = 191;
        //normalise angle difference
        float normalisedAngle = Math.abs(angle - 180f) / 180f;
        if (normalisedAngle > 1) {
            normalisedAngle = 1f;
        }

        int red = redSouth - (int) ((redSouth - redNorth) * normalisedAngle);
        int green = greenSouth - (int) ((greenSouth - greenNorth) * normalisedAngle);
        int blue = blueSouth - (int) ((blueSouth - blueNorth) * normalisedAngle);

        String hexRed = Utils.getHex(red);
        String hexGreen = Utils.getHex(green);
        String hexBlue = Utils.getHex(blue);
        String opacity = "#FF";
        constraintLayout.setBackgroundColor(Color.parseColor(opacity + hexRed + hexGreen + hexBlue));
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    private void vibrate(int durationTimeMillis) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).
                    vibrate(VibrationEffect.createOneShot(durationTimeMillis, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            ((Vibrator) getSystemService(VIBRATOR_SERVICE)).vibrate(durationTimeMillis);
        }
    }
}