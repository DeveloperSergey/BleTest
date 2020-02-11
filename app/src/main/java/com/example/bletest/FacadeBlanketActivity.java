package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.SeekBar;
import android.widget.Toast;

import java.util.List;
import java.util.UUID;

public class FacadeBlanketActivity extends AppCompatActivity implements BleConnector.BleCallbacks {

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcEnableUUID = "0000fff1-0000-1000-8000-00805f9b34fb";

    // GUI
    Context ctx;
    SeekBar seekBarPwm;

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
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {
        Log.i("mytag", "CONNECTED CALLBACK");
        Toast.makeText(ctx, "Connected", Toast.LENGTH_LONG).show();
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
}
