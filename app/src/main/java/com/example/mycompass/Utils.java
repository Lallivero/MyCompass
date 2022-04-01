package com.example.mycompass;

public class Utils {
    private static final float ALPHA = 0.075f;
    public  Utils(){

    }
    public static float[] lowPassFilter(float[] input, float[] output){
        if(output == null)
            return input;
        for(int i = 0; i < input.length; i++)
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        return output;
    }
    public static String getHex(int i) {
        if (i < 16) {
            return "0" + Integer.toHexString(i);
        }
        return Integer.toHexString(i);
    }
}
