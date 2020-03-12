package com.example.bletest;

import android.content.Context;
import android.util.Log;
import android.widget.LinearLayout;

public class TimerManager {


    private Context ctx;
    private LinearLayout parentLayout;

    TimerManager(Context ctx) {
        this.ctx = ctx;
    }

    public void setParentLayout(LinearLayout parentLayout){
        this.parentLayout = parentLayout;
    }
    public void addTimer() {
        Log.i("mytag", "ADD TIMER");
    }
}
