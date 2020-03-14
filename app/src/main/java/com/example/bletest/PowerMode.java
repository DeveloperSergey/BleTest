package com.example.bletest;

import android.util.Log;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Date;

public class PowerMode {
    private int id;
    private TextView textViewTime, textViewVal;
    private SeekBar seekBarTime, seekBarVal;
    PowerModeCallback callback;

    interface PowerModeCallback{
        void powerModeChangedCallback(int id);
    }

    PowerMode(final int id, final PowerModeCallback callback,
              TextView textViewTime1, TextView textViewVal1,
              SeekBar seekBarTime1, SeekBar seekBarVal1){
        this.id = id;
        this.callback = callback;
        this.textViewTime = textViewTime1;
        this.textViewVal = textViewVal1;
        this.seekBarTime = seekBarTime1;
        this.seekBarVal = seekBarVal1;

        if(this.seekBarTime != null) {
            this.seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textViewTime.setText(String.valueOf(seekBar.getProgress() / 60) + "h:" +
                            String.valueOf(seekBar.getProgress() % 60) + "m");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    callback.powerModeChangedCallback(id);
                }
            });
        }

        if(this.seekBarVal != null) {
            this.seekBarVal.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textViewVal.setText(String.valueOf(seekBar.getProgress()) + "%");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    textViewVal.setText(String.valueOf(seekBar.getProgress()) + "%");
                    callback.powerModeChangedCallback(id);
                }
            });
        }
    }

    int getTime(){
        return this.seekBarTime.getProgress();
    }
    void setTime(int time){
        if(time <= this.seekBarTime.getMax()){
            this.seekBarTime.setProgress(time);
        }
        else
            Log.i("mytag", "Error range of seekBar");
    }

    int getValue(){
        return seekBarVal.getProgress();
    }
    void setValue(int value){
        if(value <= this.seekBarVal.getMax())
            this.seekBarVal.setProgress(value);
        else
            Log.i("mytag", "Error range of seekBar");
    }
}
