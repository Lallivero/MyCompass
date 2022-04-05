package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;

import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

public class DisplayCompassActivity extends AppCompatActivity implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magneticField;
    private SoundPool soundPool;

    private ImageView compassImage;
    private ConstraintLayout constraintLayout;
    private TextView headingText;
    private TextView headingDegree;

    private float angle;
    private long pingDelay = 1000;
    private long previousPingTime = System.currentTimeMillis();
    private boolean hasVibrated = true;
    private float[] accelerometerValues = new float[3];
    private float[] magneticValues = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];
    private boolean soundLoaded;

    private int sound1;

    HashMap<String, Integer> headingValues = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_compass);

        compassImage = findViewById(R.id.compassImage);
        constraintLayout = findViewById(R.id.compassScreen);
        headingText = findViewById(R.id.headingWrittenTextView);
        headingDegree = findViewById(R.id.headingAngleTextView);

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.
                USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build();

        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundLoaded = true);

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_GAME);
        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);
        sound1 = soundPool.load(this, R.raw.assets_note1, 1);
        setupHeadingMap();

    }
    //North2 needed for proximity calculations to work
    private void setupHeadingMap(){
        headingValues.put("North", 0);
        headingValues.put("North-East", 45);
        headingValues.put("East", 90);
        headingValues.put("South-East", 135);
        headingValues.put("South", 180);
        headingValues.put("South-West", 225);
        headingValues.put("West", 270);
        headingValues.put("North-West", 315);
        headingValues.put("North2", 360);
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
        displayAngle();
        provideHeadingText();
        gradualNorthColour();
        if(soundLoaded)
            playSound();


    }

    //Provides the angle of the phone from the northern line
    private void updateOrientationAngles() {

        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerValues, magneticValues);

        float[] orientation = SensorManager.getOrientation(rotationMatrix, orientationAngles);

        float degrees = (float) (Math.toDegrees(orientation[0]) + 360.0f) % 360.0f;
        angle = Math.round(degrees * 100.0f) / 100.0f;
        compassImage.setRotation(angle * -1);
    }

    private void displayAngle() {
        String decimalPattern = "0.00";
        headingDegree.setText(getString(R.string.headingAngleText, Utils.decimalFormat(angle, decimalPattern)));
    }

    private void provideHeadingText() {

        float minHeadingDiff = 360;
        String closestHeading = "";
        for (Map.Entry<String, Integer> entry : headingValues.entrySet()) {
            float diff =Math.abs(angle - entry.getValue()) ;
            if ( diff < minHeadingDiff) {
                minHeadingDiff = diff;
                closestHeading = entry.getKey().replaceAll("\\d","");
            }
        }
        headingText.setText(closestHeading);
        vibrateOnNorth(closestHeading);
    }

    //Vibrates if pointing north
    private void vibrateOnNorth(String heading) {
        if(heading.equals("North") && !hasVibrated){
            int vibrationDuration = 300;
            vibrate(vibrationDuration);
            hasVibrated = true;
        }else if(!heading.equals("North")){
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
        float normalisedAngle = normaliseAngle(angle);

        int red = redSouth - (int) ((redSouth - redNorth) * normalisedAngle);
        int green = greenSouth - (int) ((greenSouth - greenNorth) * normalisedAngle);
        int blue = blueSouth - (int) ((blueSouth - blueNorth) * normalisedAngle);

        String hexRed = Utils.getHex(red);
        String hexGreen = Utils.getHex(green);
        String hexBlue = Utils.getHex(blue);
        String opacity = "#FF";
        constraintLayout.setBackgroundColor(Color.parseColor(opacity + hexRed + hexGreen + hexBlue));
    }

    private float normaliseAngle(float mAngle){
        float normalisedAngle = Math.abs(mAngle - 180f) / 180f;
        return (normalisedAngle > 1 ? 1f : normalisedAngle);
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

    private void changePingDelay(){
        float normalisedAngle = normaliseAngle(angle);
        long maxPingDelay = 3000;
        long newPingDelay = (long) ((1-normalisedAngle) * maxPingDelay);
        long minPingDelay = 200;
        pingDelay = (Math.max(newPingDelay, minPingDelay));
    }

    private void playSound(){
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        float currentVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //Normalise since soundPool.play() requires a value between 0.0 and 1.0 for volume
        float normalisedVolume = currentVolume / maxVolume;
        long currentTime = System.currentTimeMillis();
        if(currentTime- previousPingTime > pingDelay)
        {
            soundPool.play(sound1, normalisedVolume, normalisedVolume, 1, 0, 1f);
            changePingDelay();
            previousPingTime = currentTime;
        }

    }
}