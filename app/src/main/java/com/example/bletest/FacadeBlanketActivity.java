package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Timer;
import java.util.TimerTask;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Handler.Callback;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class FacadeBlanketActivity extends AppCompatActivity implements BleConnector.BleCallbacks {

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcEnableUUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    // GUI
    Context ctx;
    SeekBar seekBarPwm;
    TextView textViewTime;
    private Timer myTimer;

    // BLE
    BleConnector bleConnector;
    BluetoothDevice device = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facade_blanket);
        ctx = getApplicationContext();

        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();

        seekBarPwm = (SeekBar)findViewById(R.id.seekBarPwm);
        seekBarPwm = (SeekBar)findViewById(R.id.seekBarPwm);
        seekBarPwm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                Log.i("mytag", String.valueOf(seekBar.getProgress()));
                if (bleConnector.bleGatt == null) return;
                BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
                BluetoothGattCharacteristic charEnable = service.getCharacteristic(UUID.fromString(svcEnableUUID));
                byte[] values = new byte[]{
                        (byte) seekBar.getProgress(),
                        (byte) 0,
                        (byte) 0,
                        (byte) 0
                };
                charEnable.setValue(values);
                bleConnector.bleGatt.writeCharacteristic(charEnable);
            }
        });

        textViewTime = (TextView)findViewById(R.id.textViewTime);

        // Test time UNIX - OK
        // Get current timestamp
        Date date = new Date();
        Log.i("mytag", (date.toString()));

        // Convert to UNIX
        long value = date.getTime()/1000;
        Log.i("mytag", String.valueOf(value));

        // Back convert to Date
        Date date2 = new Date(value * 1000);
        Log.i("mytag", (date2.toString()));

        // Timer for time updating
        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethod();
            }
        }, 0, 1000);
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {
        Log.i("mytag", "CONNECTED CALLBACK");
        ((ImageView)findViewById(R.id.imageView)).setImageDrawable(ctx.getDrawable(R.drawable.state_green));
    }

    @Override
    public void disconnectedCallback() {
        Log.i("mytag", "DISCONNECTED CALLBACK");
        ((ImageView)findViewById(R.id.imageView)).setImageDrawable(ctx.getDrawable(R.drawable.state_red));
    }

    @Override
    public void writedCharCallback() {
        Log.i("mytag", "WRITED CALLBACK");
        Toast.makeText(ctx, "Writed", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        bleConnector.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        bleConnector.disconnect();
    }

    public void ledClicked(View view){
        ((ImageView)findViewById(R.id.imageView)).setImageDrawable(ctx.getDrawable(R.drawable.state_green));
    }

    private void TimerMethod() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(Timer_Tick);
    }


    private Runnable Timer_Tick = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.

            //Do something to the UI thread here
            textViewTime.setText(new Date().toString());
        }
    };

}
