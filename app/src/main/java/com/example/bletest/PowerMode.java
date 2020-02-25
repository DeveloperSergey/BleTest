package com.example.bletest;

import android.widget.SeekBar;
import android.widget.TextView;

import java.util.Date;

public class PowerMode {
    private TextView textViewTime, textViewVal;
    private SeekBar seekBarTime, seekBarVal;
    PowerModeCallback callback;

    interface PowerModeCallback{
        void powerModeChangedCallback();
    }

    PowerMode(final PowerModeCallback callback,
              TextView textViewTime1, TextView textViewVal1,
              SeekBar seekBarTime1, SeekBar seekBarVal1){
        this.callback = callback;
        this.textViewTime = textViewTime1;
        this.textViewVal = textViewVal1;
        this.seekBarTime = seekBarTime1;
        this.seekBarVal = seekBarVal1;

        if(this.seekBarTime != null) {
            this.seekBarTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    textViewTime.setText(String.valueOf(seekBar.getProgress()) + "h");
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    textViewTime.setText(String.valueOf(seekBar.getProgress()) + "h");
                    callback.powerModeChangedCallback();
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
                    callback.powerModeChangedCallback();
                }
            });
        }
    }

    int getTime(){
        return this.seekBarTime.getProgress();
    }

    int getValue(){
        return seekBarVal.getProgress();
    }
}
