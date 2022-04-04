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
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class DisplayAccelerometerActivity extends AppCompatActivity implements SensorEventListener {

    private enum Tilt {
        FACEUP,
        LEFT,
        RIGHT,
        UPRIGHT,
        UPSIDEDOWN
    }

    //Hardware access
    private SensorManager sensorManager;
    private Sensor accelerometer;

    private SoundPool soundPool;

    //UI and Views
    private TextView accelX;
    private TextView accelY;
    private TextView accelZ;
    private TextView tiltText;
    private ConstraintLayout constraintLayout;

    //Variables
    private float[] filteredValues;
    private int sound1;
    private int sound2;
    private int sound3;
    private int sound4;
    private Tilt tilt = Tilt.FACEUP;
    private Tilt previousTilt;
    private long shakeStartTime = 0;
    private int shakeCount = 0;
    private final float GRAVITY_CONSTANT = 9.82f;

    //Flow control
    private boolean thematicColour = false;
    private boolean soundLoaded = false;
    private boolean currentlyShaking = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_display_accelerometer);

        AudioAttributes audioAttributes = new AudioAttributes.Builder().setUsage(AudioAttributes.
                USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(audioAttributes).build();
        soundPool.setOnLoadCompleteListener((soundPool, sampleId, status) -> soundLoaded = true);
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);

        this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

        initialiseUiAndAudio();
    }

    //Initialise the TextViews and give them starter values, also initiates sound resources
    private void initialiseUiAndAudio() {
        accelX = findViewById(R.id.xValue);
        accelY = findViewById(R.id.yValue);
        accelZ = findViewById(R.id.zValue);
        tiltText = findViewById(R.id.tiltText);
        accelX.setText(getString(R.string.xValue, "0.0"));
        accelY.setText(getString(R.string.yValue, "0.0"));
        accelZ.setText(getString(R.string.zValue, "0.0"));
        constraintLayout = findViewById(R.id.accelerometerLayout);

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


        filteredValues = Utils.lowPassFilter(sensorEvent.values.clone(), filteredValues);
        onTilt();
        if (thematicColour)
            gradualThematicColours();
        else
            vibrantTiltColour();


    }

    private void onTilt() {
        displayCurrentValues();

        tiltText.setText(getString(R.string.tiltText, tilt));

        setTilt();
        if (tilt != previousTilt && soundLoaded) {
            amIShaking();
            playSound();
        }
    }

    //Changes background colour based on tilt, x controls red, y green and z blue
    private void vibrantTiltColour() {
        int lowestValue = 75;
        int maxValue = 255;
        int x = calculateRGB(filteredValues[0], maxValue, lowestValue);
        int y = calculateRGB(filteredValues[1], maxValue, lowestValue);
        int z = calculateRGB(filteredValues[2], maxValue, lowestValue);
        String xHex = Utils.getHex(x);
        String yHex = Utils.getHex(y);
        String zHex = Utils.getHex(z);
        String opacity = "#FF";
        constraintLayout.setBackgroundColor(Color.parseColor(opacity + xHex + yHex + zHex));


    }

    //calculates the RGB value between max and lowest for a leaning x,y or z
    private int calculateRGB(float value, int maxValue, int lowestValue) {
        float normalisedValue = Utils.normalise(value, GRAVITY_CONSTANT);
        return (Math.max((int) (normalisedValue * maxValue), lowestValue));
    }

    //Sets the tilt depending on the tilt value
    private void setTilt() {
        float tiltValue = 3f;
        previousTilt = tilt;
        if (filteredValues[0] > tiltValue && filteredValues[0] > Math.abs(filteredValues[1])) {
            tilt = Tilt.LEFT;
        } else if (filteredValues[0] < -tiltValue && Math.abs(filteredValues[0]) > Math.abs(filteredValues[1])) {
            tilt = Tilt.RIGHT;
        } else if (filteredValues[1] > tiltValue && filteredValues[1] > Math.abs(filteredValues[0])) {
            tilt = Tilt.UPRIGHT;
        } else if (filteredValues[1] < -tiltValue && Math.abs(filteredValues[1]) > Math.abs(filteredValues[0])) {
            tilt = Tilt.UPSIDEDOWN;
        } else {
            tilt = Tilt.FACEUP;
        }


    }

    //Display the current accelerometer values in the respective TextFields,
    //also reduces number of decimals
    private void displayCurrentValues() {
        String decimalPattern = "0.00";
        accelX.setText(getString(R.string.xValue, Utils.decimalFormat(filteredValues[0], decimalPattern)));
        accelY.setText(getString(R.string.yValue, Utils.decimalFormat(filteredValues[1], decimalPattern)));
        accelZ.setText(getString(R.string.zValue, Utils.decimalFormat(filteredValues[2], decimalPattern)));
    }

    private void amIShaking() {
        long currentTime = System.currentTimeMillis();
        if ((currentTime - shakeStartTime) > 400) {
            shakeStartTime = currentTime;
            currentlyShaking = true;
        }
        if (currentlyShaking) {
            if (shakeCount == 2) {
                shakeCount = 0;
                Toast.makeText(this, "Ooooh I am getting dizzy!", Toast.LENGTH_SHORT).show();
                currentlyShaking = false;
            } else if ((currentTime - shakeStartTime) < 100) {
                shakeCount++;
            }
        }
    }

    private void playSound() {
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        float currentVolume = (float) audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = (float) audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        //Normalise since soundPool.play() requires a value between 0.0 and 1.0 for volume
        float normalisedVolume = currentVolume / maxVolume;
        switch (tilt) {
            case FACEUP:
                break;
            case LEFT:
                soundPool.play(sound1, normalisedVolume, normalisedVolume, 1, 0, 1f);
                break;
            case RIGHT:
                soundPool.play(sound3, normalisedVolume, normalisedVolume, 1, 0, 1f);
                break;
            case UPRIGHT:
                soundPool.play(sound2, normalisedVolume, normalisedVolume, 1, 0, 1f);
                break;
            case UPSIDEDOWN:
                soundPool.play(sound4, normalisedVolume, normalisedVolume, 1, 0, 1f);
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    //EVERYTHING BELOW THIS POINT IS EXPERIMENTAL AND NOT REFACTORED FOR READABILITY,
    //NONE OF IT IS USED IN THE RUNNING APP
    //-------------------------------------------------------------------------------

    //Technically works, but due to the pastels being so close in RGB values and so close to white
    //the result is just a whiteish mess unless you manage to get almost precisely 0 on two of the axes
    private void gradualThematicColours() {
        int[] xColour = {204, 212, 191};
        int[] yColour = {238, 186, 178};
        int[] zColour = {245, 226, 228};

        float normalisedX = Utils.normalise(filteredValues[0], GRAVITY_CONSTANT);
        float normalisedY = Utils.normalise(filteredValues[1], GRAVITY_CONSTANT);
        float normalisedZ = Utils.normalise(filteredValues[2], GRAVITY_CONSTANT);
        Log.e("Test", String.valueOf(normalisedX));
        xColour = Utils.intArrayMultiplyByN(xColour, normalisedX);
        yColour = Utils.intArrayMultiplyByN(yColour, normalisedY);
        zColour = Utils.intArrayMultiplyByN(zColour, normalisedZ);
        Log.e("Test", String.valueOf(xColour[0]));
        Log.e("Test", String.valueOf(yColour[0]));
        Log.e("Test", String.valueOf(zColour[0]));
        int[] finalColour = {xColour[0] + yColour[0] + zColour[0], xColour[1] + yColour[1] + zColour[1], xColour[2] + yColour[2] + zColour[2]};
        String[] finalColourHex = new String[3];
        for (int i = 0; i < finalColour.length; i++) {
            finalColourHex[i] = Utils.getHex(Math.min(finalColour[i], 255));
        }
        Log.e("Test", String.valueOf(finalColourHex[0]));
        Log.e("Test", String.valueOf(finalColourHex[1]));
        Log.e("Test", String.valueOf(finalColourHex[2]));
        constraintLayout.setBackgroundColor(Color.parseColor("#FF" + finalColourHex[0] + finalColourHex[1] + finalColourHex[2]));
    }
}