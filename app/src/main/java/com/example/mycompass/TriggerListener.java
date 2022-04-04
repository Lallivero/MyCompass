package com.example.mycompass;

import android.content.Context;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;

public class TriggerListener extends TriggerEventListener {
    private Context context;
    public TriggerListener(Context context){
        this.context =  context;

    }
    @Override
    public void onTrigger(TriggerEvent triggerEvent) {
        if(triggerEvent.values[0] == 1){
            Log.e("Test", "SHAKING!");
        }
    }
}
