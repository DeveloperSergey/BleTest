package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

public class ActivityCreateTimer extends AppCompatActivity implements FragmentPowerModes.OnFragmentInteractionListener {

    FragmentManager fragmentManager;

    protected RadioButton radioButtonHard, radioButtonSoft;
    protected TextView textViewTimeStart, textViewTimeStop;
    protected SeekBar seekBarTimeStart, seekBarTimeStop;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_timer);

        fragmentManager = getSupportFragmentManager();

        textViewTimeStart = (TextView)findViewById(R.id.textViewTimeStart);
        textViewTimeStop = (TextView)findViewById(R.id.textViewTimeStop);

        seekBarTimeStart = (SeekBar)findViewById(R.id.seekBarStart);
        seekBarTimeStart.setMax(24*60);
        seekBarTimeStop = (SeekBar)findViewById(R.id.seekBarStop);
        seekBarTimeStop.setMax(24*60);
        seekBarTimeStop.setEnabled(false);

        seekBarTimeStart.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBarTimeStop.getProgress() < seekBarTimeStart.getProgress())
                    seekBarTimeStop.setProgress(seekBarTimeStart.getProgress());
                textViewTimeStart.setText(String.valueOf(seekBar.getProgress() / 60) + ":" +
                        String.valueOf(seekBar.getProgress() % 60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                textViewTimeStart.setText(String.valueOf(seekBar.getProgress() / 60) + ":" +
                        String.valueOf(seekBar.getProgress() % 60));
            }
        });
        seekBarTimeStop.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if(seekBarTimeStart.getProgress() > seekBarTimeStop.getProgress())
                    seekBarTimeStart.setProgress(seekBarTimeStop.getProgress());
                textViewTimeStop.setText(String.valueOf(seekBar.getProgress() / 60) + ":" +
                        String.valueOf(seekBar.getProgress() % 60));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                textViewTimeStop.setText(String.valueOf(seekBar.getProgress() / 60) + ":" +
                        String.valueOf(seekBar.getProgress() % 60));
            }
        });

        radioButtonHard = (RadioButton) findViewById(R.id.radioButtonHard);
        radioButtonHard.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if(b == true) seekBarTimeStop.setEnabled(false);
                else seekBarTimeStop.setEnabled(true);
            }
        });
        radioButtonSoft = (RadioButton) findViewById(R.id.radioButtonSoft);

    }

    @Override
    public void onFragmentInteraction(Uri uri) {
        Log.i("mytag", "LISTENER IN ACTIVITY FROM FRAGMENT");
    }

    public void addOnClick(View view){
        Log.i("mytag","NEW TIMER");
        Log.i("mytag","radioHard: " + String.valueOf(radioButtonHard.isChecked()));
        Log.i("mytag","radioSoft: " + String.valueOf(radioButtonSoft.isChecked()));
        Log.i("mytag","timeStart: " + String.valueOf(seekBarTimeStart.getProgress()));
        Log.i("mytag","timeStop: " + String.valueOf(seekBarTimeStop.getProgress()));

        SeekBar seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod1Time));
        Log.i("mytag","time1: " + String.valueOf(seekBar.getProgress()));
        seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod2Time));
        Log.i("mytag","time2: " + String.valueOf(seekBar.getProgress()));
        seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod3Time));
        Log.i("mytag","time3: " + String.valueOf(seekBar.getProgress()));
        seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod1Val));
        Log.i("mytag","val1: " + String.valueOf(seekBar.getProgress()));
        seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod2Val));
        Log.i("mytag","val2: " + String.valueOf(seekBar.getProgress()));
        seekBar = (SeekBar)(fragmentManager.findFragmentById(R.id.fragmentPowerModes).getView()
                .findViewById(R.id.seekBarPowMod3Val));
        Log.i("mytag","val3: " + String.valueOf(seekBar.getProgress()));
    }
}
