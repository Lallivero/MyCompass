package com.example.mycompass;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {
//    public static final String EXTRA_MESSAGE = "com.example.mycompass.ACCELEROMETER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void getAccelerometerValues(View view) {
        Intent intent = new Intent(this, DisplayAccelerometerActivity.class);
        startActivity(intent);
    }

    public void getCompassValues(View view){
        Intent intent = new Intent(this, DisplayCompassActivity.class);
        startActivity(intent);
    }
}