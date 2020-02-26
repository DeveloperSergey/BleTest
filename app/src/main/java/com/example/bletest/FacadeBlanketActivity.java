package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
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

import org.florescu.android.rangeseekbar.RangeSeekBar;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class FacadeBlanketActivity extends AppCompatActivity implements BleConnector.BleCallbacks,
        PowerMode.PowerModeCallback {

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcFactoryUUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    final String svcTimeUUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    final String svcEnableUUID = "0000fff3-0000-1000-8000-00805f9b34fb";
    final String svcTemperatureUUID = "0000fff4-0000-1000-8000-00805f9b34fb";

    final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

    // GUI
    Context ctx;
    SeekBar seekBarPwm, seekBarHard;
    TextView textViewTime, textViewTimeDev, textViewFactory, textViewSoftMin, textViewSoftMax, textViewHard;
    Timer timerApp, timerDev;
    ArrayList<TextView> textViewsTemp = new ArrayList<>();
    RangeSeekBar rangeSeekBarSoftMode;

    SeekBar seekBarPowMod1Time, seekBarPowMod1Val;
    SeekBar seekBarPowMod2Time, seekBarPowMod2Val;
    SeekBar seekBarPowMod3Time, seekBarPowMod3Val;
    TextView textViewPowMod1Time, textViewPowMod1Val;
    TextView textViewPowMod2Time, textViewPowMod2Val;
    TextView textViewPowMod3Time, textViewPowMod3Val;

    // Data
    Date dateDev;
    int hardware, firmware;

    // BLE
    BleConnector bleConnector;
    BluetoothDevice device = null;
    PowerMode powerMode1, powerMode2, powerMode3;

    @Override
    public void powerModeChangedCallback() {
        Log.i("mytag", "POWER MODE CHANGED");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facade_blanket);
        ctx = getApplicationContext();
        getSupportActionBar().hide();

        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();




        // Power modes
        seekBarPowMod1Time = (SeekBar) findViewById(R.id.seekBarPowMod1Time);
        seekBarPowMod2Time = (SeekBar) findViewById(R.id.seekBarPowMod2Time);
        seekBarPowMod3Time = (SeekBar) findViewById(R.id.seekBarPowMod3Time);
        seekBarPowMod1Val = (SeekBar) findViewById(R.id.seekBarPowMod1Val);
        seekBarPowMod2Val = (SeekBar) findViewById(R.id.seekBarPowMod2Val);
        seekBarPowMod3Val = (SeekBar) findViewById(R.id.seekBarPowMod3Val);
        textViewPowMod1Time = (TextView) findViewById(R.id.textViewPowMod1Time);
        textViewPowMod2Time = (TextView) findViewById(R.id.textViewPowMod2Time);
        textViewPowMod3Time = (TextView) findViewById(R.id.textViewPowMod3Time);
        textViewPowMod1Val = (TextView) findViewById(R.id.textViewPowMod1Val);
        textViewPowMod2Val = (TextView) findViewById(R.id.textViewPowMod2Val);
        textViewPowMod3Val = (TextView) findViewById(R.id.textViewPowMod3Val);
        powerMode1 = new PowerMode(this, textViewPowMod1Time, textViewPowMod1Val, seekBarPowMod1Time, seekBarPowMod1Val);
        powerMode1 = new PowerMode(this, textViewPowMod2Time, textViewPowMod2Val, seekBarPowMod2Time, seekBarPowMod2Val);
        powerMode1 = new PowerMode(this, textViewPowMod3Time, textViewPowMod3Val, seekBarPowMod3Time, seekBarPowMod3Val);


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
                Log.i("mytag", String.valueOf(values[0]));
                charEnable.setValue(values);
                bleConnector.writeChar(charEnable);
                // Add sequence!
            }
        });

        textViewTime = (TextView)findViewById(R.id.textViewTime);
        textViewTimeDev = (TextView)findViewById(R.id.textViewTimeDev);
        textViewFactory = (TextView)findViewById(R.id.textViewFactory);
        textViewSoftMin = (TextView)findViewById(R.id.textViewSoftMin);
        textViewSoftMax = (TextView)findViewById(R.id.textViewSoftMax);
        textViewHard = (TextView)findViewById(R.id.textViewHard);

        // 7 fields for temperatures <-- temperatures from char
        textViewsTemp.add((TextView) findViewById(R.id.textViewTempBoard));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp1));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp2));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp3));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp4));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp5));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp6));

        rangeSeekBarSoftMode = (RangeSeekBar) findViewById(R.id.rsbSoft);
        rangeSeekBarSoftMode.setRangeValues(0, 24*60);
        rangeSeekBarSoftMode.setOnRangeSeekBarChangeListener(new RangeSeekBar.OnRangeSeekBarChangeListener() {
            @Override
            public void onRangeSeekBarValuesChanged(RangeSeekBar bar, Object minValue, Object maxValue) {
                int value = bar.getSelectedMinValue().intValue();
                //Log.i("mytag", String.valueOf(value / 60) + " " + String.valueOf(value % 60));
                textViewSoftMin.setText(String.valueOf(value / 60) + "h:" + String.valueOf(value % 60) + "m");
                value = bar.getSelectedMaxValue().intValue();
                //Log.i("mytag", String.valueOf(value / 60) + " " + String.valueOf(value % 60));
                textViewSoftMax.setText(String.valueOf(value / 60) + "h:" + String.valueOf(value % 60) + "m");
            }
        });

        seekBarHard = (SeekBar)findViewById(R.id.seekBarHard);
        seekBarHard.setMax(24 * 60);
        seekBarHard.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int value = (seekBar.getProgress() / 30) * 30;
                textViewHard.setText(String.valueOf(value / 60) + "h:" + String.valueOf(value % 60) + "m");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


        // Test time UNIX - OK

        /*Date date = new Date();
        Log.i("mytag", (date.toString()));

        // Convert to UNIX
        long value = date.getTime()/1000;
        Log.i("mytag", String.valueOf(value));

        // Back convert to Date
        Date date2 = new Date(value * 1000);
        Log.i("mytag", (date2.toString()));*/

        // Timer for time updating
        timerApp = new Timer();
        timerApp.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethodApp();
            }
        }, 0, 1000);

        timerDev = new Timer();
        timerDev.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethodDev();
            }
        }, 0, 3000);
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.i("mytag", "CONNECTED CALLBACK");
                ((ImageView)findViewById(R.id.imageView)).setImageDrawable(ctx.getDrawable(R.drawable.state_green));

                if (!bleConnector.isConnect()) return;

                // Read Factory
                BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
                BluetoothGattCharacteristic charFactory = service.getCharacteristic(UUID.fromString(svcFactoryUUID));
                bleConnector.readChar(charFactory);
                setTimeInDevice();

                // Enable notification
                UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = convertFromInteger(0x2902);
                BluetoothGattCharacteristic charTemperature = service.getCharacteristic(UUID.fromString(svcTemperatureUUID));
                bleConnector.bleGatt.setCharacteristicNotification(charTemperature, true);
                BluetoothGattDescriptor descriptor = charTemperature.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                if(bleConnector.writeDesc(descriptor));
            }
        });
    }

    public UUID convertFromInteger(int i) {
        final long MSB = 0x0000000000001000L;
        final long LSB = 0x800000805f9b34fbL;
        long value = i & 0xFFFFFFFF;
        return new UUID(MSB | (value << 32), LSB);
    }


    @Override
    public void disconnectedCallback() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.i("mytag", "DISCONNECTED CALLBACK");
                ((ImageView)findViewById(R.id.imageView)).setImageDrawable(ctx.getDrawable(R.drawable.state_red));
            }
        });

    }

    @Override
    public void writedCharCallback() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i("mytag", "WRITED CALLBACK");
            }
        });
    }

    @Override
    public void readedCharCallback(BluetoothGattCharacteristic characteristic) {

        UUID uuid = characteristic.getUuid();
        if(uuid.toString().equals(svcFactoryUUID)){
            hardware = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            firmware = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 2);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    textViewFactory.setText("HW: " + String.valueOf(hardware) + " " +
                            "FW: " + String.valueOf(firmware));
                }
            });
        }
        else if(uuid.toString().equals(svcTimeUUID)) {
            //Log.i("mytag", "READED CALLBACK");
            long value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            int lrc = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);

            int lrcSum = 0;
            for (byte b : characteristic.getValue())
                lrcSum = (byte) (lrcSum + b);

            //Log.i("mytag", String.valueOf(value) );
            //Log.i("mytag", String.valueOf(lrc) );
            //Log.i("mytag", String.valueOf(lrcSum) );

            if (lrcSum == 0) {
                dateDev = new Date(value * 1000);
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewTimeDev.setText(dateFormat.format(dateDev));
                    }
                });
            }
        }
    }

    @Override
    public void notificationCallback(final BluetoothGattCharacteristic characteristic) {

        UUID uuid = characteristic.getUuid();
        if (uuid.toString().equals(svcTemperatureUUID)) {
            //final int tempBoard = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, 0);
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i < 7; i++) {
                        int temp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, i);
                        textViewsTemp.get(i).setText(String.valueOf(temp) + "\u00B0 C");
                    }
                }
            });
        }
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
        Toast.makeText(this, "clicked", Toast.LENGTH_SHORT).show();
    }

    private void TimerMethodApp() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewTime.setText(dateFormat.format(new Date()));
            }
        });
    }

    private void TimerMethodDev() {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        if ( !bleConnector.isConnect() ) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTime = service.getCharacteristic(UUID.fromString(svcTimeUUID));
        bleConnector.readChar(charTime);
    }

    private void setTimeInDevice(){
        // Write time
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTime = service.getCharacteristic(UUID.fromString(svcTimeUUID));
        long value = new Date().getTime()/1000;
        //Log.i("mytag", String.valueOf(value));

        byte[] values = new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                0
        };
        int lrc = 0;
        for(byte b : values){
            lrc = (byte)(lrc + b);
        }
        lrc = (255 - lrc) + 1;
        //Log.i("mytag", String.valueOf(lrc));
        values[4] = (byte)lrc;

        charTime.setValue(values);
        bleConnector.writeChar(charTime);
    }

    public void resetOnClick(View view){

    }
}
