package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.TextView;

import java.text.DecimalFormat;

public class DisplayAccelerometerActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private float xValue;
    private float yValue;
    private float zValue;
    private TextView accelX;
    private TextView accelY;
    private TextView accelZ;
    private Utils utils;

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
        accelX.setText(getString(R.string.xValue, "0.0"));
        accelY.setText(getString(R.string.yValue, "0.0"));
        accelZ.setText(getString(R.string.zValue, "0.0"));

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

    }
    //Display the current accelerometer values in the respective TextFields
    private void displayCurrentValues() {
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        accelX.setText(getString(R.string.xValue, decimalFormat.format(xValue)));
        accelY.setText(getString(R.string.yValue, decimalFormat.format(yValue)));
        accelZ.setText(getString(R.string.zValue, decimalFormat.format(zValue)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}