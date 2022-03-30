package com.example.mycompass;

public class Utils {
    private static final float ALPHA = 0.8f;
    public  Utils(){

    }
    public float lowPassFilter(float input){
        float output = input;
        output = output + ALPHA * (input-output);
        return output;
    }
}
