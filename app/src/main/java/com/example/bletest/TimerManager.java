package com.example.bletest;

import android.content.Context;
import android.text.Layout;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

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

        /*LinearLayout layout = new LinearLayout(ctx);
        layout.setOrientation(LinearLayout.HORIZONTAL);

        TextView textView = new TextView(ctx);
        textView.setText("Hello");
        layout.addView(textView);

        Button button = new Button(ctx);
        button.setText("Del");
        layout.addView(button);

        parentLayout.addView(layout);*/
        //R.id.timer_item
    }
}
