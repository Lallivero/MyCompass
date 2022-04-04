package com.example.mycompass;

import java.text.DecimalFormat;

public class Utils {
    private static final float ALPHA = 0.09f;

    public Utils() {

    }

    public static float[] lowPassFilter(float[] input, float[] output) {
        if (output == null)
            return input;
        for (int i = 0; i < input.length; i++)
            output[i] = output[i] + ALPHA * (input[i] - output[i]);
        return output;
    }

    public static String getHex(int i) {
        if (i < 16) {
            return "0" + Integer.toHexString(i);
        }
        return Integer.toHexString(i);
    }

    public static String decimalFormat(float value, String pattern) {
        DecimalFormat dF = new DecimalFormat(pattern);
        return dF.format(value);
    }

    public static float normalise(float value, float maxValue) {
        float normalisedValue = Math.abs(value) / maxValue;
        return (normalisedValue > 1 ? 1 : normalisedValue);
    }

    public static void intArrayMultiplyByN(int[] array, float n) {

        for (int i = 0; i < array.length; i++) {
            array[i] = (int) (array[i] * n);
        }
    }

    public static void invertColours(int[] array){
        for(int i = 0; i < array.length; i++){
            array[i] = 255 - array[i];
        }
    }
}
