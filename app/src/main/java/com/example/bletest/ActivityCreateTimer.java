package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
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
        seekBarTimeStop.setProgress(24*60);
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
    public void onFragmentInteraction(Uri uri) { }

    public void addOnClick(View view){
        byte[] values = new byte[20];

        // Command
        values[0] = 0;
        // Number
        values[1] = 0;
        //Time start
        values[2] = (byte)(seekBarTimeStart.getProgress() & 0xFF);
        values[3] = (byte)((seekBarTimeStart.getProgress() >> 8) & 0xFF);
        // Time stop
        values[4] = (byte)(seekBarTimeStop.getProgress() & 0xFF);
        values[5] = (byte)((seekBarTimeStop.getProgress() >> 8) & 0xFF);

        FragmentPowerModes fragment = (FragmentPowerModes)fragmentManager.findFragmentById(R.id.fragmentPowerModes);
        byte[] valuesPower = fragment.getBytes();

        values[6] = valuesPower[0];
        values[7] = valuesPower[1];
        values[8] = valuesPower[2];
        values[9] = valuesPower[3];
        values[10] = valuesPower[4];
        values[11] = valuesPower[5];
        values[12] = valuesPower[6];
        values[13] = valuesPower[7];
        values[14] = valuesPower[8];

        // Type soft
        if(radioButtonSoft.isChecked()) values[15] = 1;
        else values[15] = 0;

        // Enable
        values[16] = 1;

        // Complete flag
        values[17] = 0;

        // Reserved
        values[18] = 0;

        // LRC
        values[19] = 0;
        values[19] = CheckSum.LRC(values);


        Intent intent = new Intent();
        intent.putExtra("data", values);
        setResult(RESULT_OK, intent);
        finish();
    }
}
