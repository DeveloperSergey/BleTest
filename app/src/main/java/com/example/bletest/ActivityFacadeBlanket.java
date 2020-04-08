package com.example.bletest;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ActivityFacadeBlanket extends AppCompatActivity implements BleConnector.BleCallbacks,
        FragmentPowerModes.OnFragmentInteractionListener, AdapterTimer.BlanketTimerCallback {

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcFactoryUUID = "0000fff1-0000-1000-8000-00805f9b34fb";
    final String svcTimeUUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    final String svcStatusUUID = "0000fff3-0000-1000-8000-00805f9b34fb";
    final String svcTemperatureUUID = "0000fff4-0000-1000-8000-00805f9b34fb";
    final String svcTimersUUID = "0000fff5-0000-1000-8000-00805f9b34fb";
    final String svcAlarmUUID = "0000fff6-0000-1000-8000-00805f9b34fb";

    final int REQUEST_ADD_TIMER = 101;

    FragmentManager fragmentManager;

    final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm");

    // GUI
    Context ctx;
    TextView textViewTime, textViewTimeDev, textViewFactory, textViewPwm, textViewCurrPowerMode, textViewVoltage;
    Timer timerApp, timerDev;
    ArrayList<TextView> textViewsTemp = new ArrayList<>();

    // Date
    Date dateDev;
    int hardware, firmware;

    // BLE
    BleConnector bleConnector;
    BluetoothDevice device = null;

    // Commands for characteristic "svcTimers"
    final int TIM_SET = 0;
    final int TIM_RESET = 1;
    final int TIM_CLEAR = 2;
    final int TIM_READ = 3;
    final int TIM_IMMEDIATELY = 4;

    // Blanker timers
    final int TIMERS_MAX_NUM = 10;
    ArrayList<BlanketTimer> timers = new ArrayList<>();
    AdapterTimer adapterTimer;
    ListView listViewTimers;

    // Android timers
    final int timerAppTimePeriod = 1000;
    final int timerStatusPeriod = 10000;


    // Life cycle Activity
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

        fragmentManager = getSupportFragmentManager();


        // GUI
        View header = getLayoutInflater().inflate(R.layout.blanket_list_header, null);
        adapterTimer = new AdapterTimer(this, ctx, timers);
        listViewTimers = (ListView)findViewById(R.id.listViewTimers);
        listViewTimers.addHeaderView(header, "", false);
        listViewTimers.setAdapter(adapterTimer);
        textViewPwm = (TextView)findViewById(R.id.textViewPwm);
        textViewTime = (TextView)findViewById(R.id.textViewTime);
        textViewTimeDev = (TextView)findViewById(R.id.textViewTimeDev);
        textViewFactory = (TextView)findViewById(R.id.textViewFactory);
        textViewCurrPowerMode = (TextView) findViewById(R.id.textViewCurrPowerModeVal);
        textViewVoltage = (TextView) findViewById(R.id.textViewVoltVal);

        // Fields for temperatures <-- temperatures from characteristics
        textViewsTemp.add((TextView) findViewById(R.id.textViewTempBoard));
        textViewsTemp.add((TextView) findViewById(R.id.textViewTemp1));

        // Timers for time & status update
        timerApp = new Timer();
        timerApp.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethodApp();
            }
        }, 0, timerAppTimePeriod);

        timerDev = new Timer();
        timerDev.schedule(new TimerTask() {
            @Override
            public void run() {
                TimerMethodDev();
            }
        }, 0, timerStatusPeriod);

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

    @Override
    public void onFragmentInteraction(Uri uri) { }


    // BLE callbacks
    @Override
    public void operationFailed(BleConnector.OPERATIONS operation) {
        Log.i("mytag", "FAILED: " + String.valueOf(operation));
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

                // Enable notifications
                bleConnector.notiEnable(svUUID, svcTemperatureUUID);
                bleConnector.notiEnable(svUUID, svcTimeUUID);
                bleConnector.notiEnable(svUUID, svcTimersUUID);
                bleConnector.notiEnable(svUUID, svcAlarmUUID);
            }
        });
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
    public void writeCharCallback() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //Log.i("mytag", "WRITED CALLBACK");
            }
        });
    }

    @Override
    public void readCharCallback(BluetoothGattCharacteristic characteristic) {

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
        else if(uuid.toString().equals(svcStatusUUID)) {
            int powerMode = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1);
            int pwm = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 2);
            int voltage = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 3);
            updateStatus(powerMode, pwm, voltage);
        }
    }

    @Override
    public void notificationCallback(final BluetoothGattCharacteristic characteristic) {

        UUID uuid = characteristic.getUuid();
        if (uuid.toString().equals(svcTemperatureUUID)) {
            //Log.i("mytag", "TEMP CALLBACK");
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    for(int i = 0; i < 2; i++) {
                        int temp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_SINT8, i);
                        textViewsTemp.get(i).setText(String.valueOf(temp) + "\u00B0 C");
                    }
                }
            });
        }
        else if(uuid.toString().equals(svcTimeUUID)) {
            //Log.i("mytag", "TIME CALLBACK");
            long value = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
            int lrc = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);

            int lrcSum = 0;
            for (byte b : characteristic.getValue())
                lrcSum = (byte) (lrcSum + b);

            //Log.i("mytag", String.valueOf(value) );
            //Log.i("mytag", String.valueOf(lrc) );
            //Log.i("mytag", String.valueOf(lrcSum) );

            if (lrcSum == 0) {
                dateDev = new Date((value * 1000) - new GregorianCalendar().getTimeZone().getRawOffset());
                this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textViewTimeDev.setText(dateFormat.format(dateDev));
                    }
                });
            }
        }
        else if(uuid.toString().equals(svcTimersUUID)){
            byte[] values = characteristic.getValue();
            Log.i("mytag",  Arrays.toString(values));

            boolean isEmpty = true;
            for(byte b : values) {
                if (b != 0) isEmpty = false;
            }

            if(!isEmpty) {
                addTimer(values);
            }
        }
        else if(uuid.toString().equals(svcAlarmUUID)){
            boolean alarmTemp, alarmVolt;
            if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 0) == 1)
                alarmTemp = true;
            else alarmTemp = false;
            if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 1) == 1)
                alarmVolt = true;
            else alarmVolt = false;

            showAlarm(alarmTemp, alarmVolt);
        }
        else Log.i("mytag", "UNKNOWN CALLBACK");
    }


    // Update time in app
    private void TimerMethodApp() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewTime.setText(dateFormat.format(new Date()));
            }
        });
    }

    // Read status from blanket
    private void TimerMethodDev() {
        if ( !bleConnector.isConnect() ) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        if(service == null) return;
        BluetoothGattCharacteristic charStatus = service.getCharacteristic(UUID.fromString(svcStatusUUID));
        bleConnector.readChar(charStatus);
    }

    // Write time
    private void setTimeInDevice(){
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTime = service.getCharacteristic(UUID.fromString(svcTimeUUID));

        long value = (new Date().getTime() / 1000) +
                (new GregorianCalendar().getTimeZone().getRawOffset() / 1000);

        byte[] values = new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >> 8) & 0xFF),
                (byte) ((value >> 16) & 0xFF),
                (byte) ((value >> 24) & 0xFF),
                0
        };
        values[4] = CheckSum.LRC(values);

        charTime.setValue(values);
        bleConnector.writeChar(charTime);
    }


    // Listeners for buttons
    public void timersAddOnClick(View view){

        if(timers.size() >= TIMERS_MAX_NUM){
            showOverflow();
        }
        else {
            Intent intent = new Intent(getApplicationContext(), ActivityCreateTimer.class);
            startActivityForResult(intent, REQUEST_ADD_TIMER);
        }
    }

    public void timersUpdate(View view){
        // Read timers
        timers.clear();
        adapterTimer.notifyDataSetChanged();
        blanketGetTimers();
    }

    public void timersClrOnClick(View view){
        Log.i("mytag", "CLEAR TIMERS");

        byte[] command = new byte[20];
        command[0] = TIM_CLEAR;
        command[19] = CheckSum.LRC(command);
        // Send to device
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
        charTimers.setValue(command);
        bleConnector.writeChar(charTimers);

        // GUI
        timers.clear();
        adapterTimer.notifyDataSetChanged();
    }

    public void startOnClick(View view){

        byte[] values = new byte[20];

        // Command
        values[0] = TIM_IMMEDIATELY;
        // Number
        values[1] = 0;
        //Time start
        values[2] = 0;
        values[3] = 0;
        // Time stop
        values[4] = 0;
        values[5] = 0;

        FragmentPowerModes fragment = (FragmentPowerModes)fragmentManager.findFragmentById(R.id.fragmentPowerModesBlt);
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

        // Type
        values[15] = 0;

        // Enable
        values[16] = 1;

        // Complete flag
        values[17] = 0;

        // Reserved
        values[18] = 0;

        // LRC
        values[19] = 0;
        values[19] = CheckSum.LRC(values);

        // Send to device
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
        charTimers.setValue(values);
        bleConnector.writeChar(charTimers);

        Toast.makeText(ctx, "START", Toast.LENGTH_LONG).show();
    }

    private void blanketGetTimers(){

        byte[] command = new byte[20];
        command[0] = TIM_READ;
        command[19] = CheckSum.LRC(command);
        // Send to device
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
        charTimers.setValue(command);
        bleConnector.writeChar(charTimers);
    }

    @Override // Result from ActivityCreateTimer
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if((requestCode == REQUEST_ADD_TIMER) && (resultCode == RESULT_OK)){
            byte[] result = data.getByteArrayExtra("data");
            Log.i("mytag", Arrays.toString(result));

            //Number
            BlanketTimer timer = new BlanketTimer(result);
            timer.number = timers.size();
            result = timer.getBytes();
            result[19] = 0;
            result[19] = CheckSum.LRC(result);


            // Send to device
            if (!bleConnector.isConnect()) return;
            BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
            BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
            charTimers.setValue(result);
            bleConnector.writeChar(charTimers);

            addTimer(result);
        }
    }


    // Update UI
    public void addTimer(final byte[] values){
        if(timers.size() >= TIMERS_MAX_NUM){
            Toast.makeText(ctx, "TIMERS MAX NUM: "
                    + String.valueOf(TIMERS_MAX_NUM), Toast.LENGTH_SHORT).show();;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                timers.add(new BlanketTimer(values));
                adapterTimer.notifyDataSetChanged();
            }
        });
    }

    public void showAlarm(final boolean alarmTemperature, final boolean alarmVoltage){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(alarmVoltage) Toast.makeText(ctx, "ALARM VOLTAGE", Toast.LENGTH_SHORT).show();
                if(alarmTemperature) Toast.makeText(ctx, "ALARM TEMPERATURE", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void showOverflow(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, "MAX NUM: 10", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void updateStatus(final int pm, final int pwm, final int vol){

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String powerModeStr;
                switch (pm){
                    case 0: powerModeStr = "DISABLE"; break;
                    case 1: powerModeStr = "MODE 1"; break;
                    case 2: powerModeStr = "MODE 2"; break;
                    case 3: powerModeStr = "MODE 3"; break;
                    default: powerModeStr = "NOT FOUND";
                }

                textViewCurrPowerMode.setText(powerModeStr);
                textViewPwm.setText(String.valueOf(pwm) + " %");
                textViewVoltage.setText(String.valueOf(vol/1000) + ","
                        + String.valueOf((vol/100)%10) + " V");
            }
        });
    }


    // Callbacks from custom adapter (listViewTimers)
    @Override
    public void removeTimer(int position) {
        Log.i("mytag", "REMOVE TIMER");
        byte[] values = timers.get(position).getBytes();
        values[0] = TIM_RESET;
        values[19] = 0;
        values[19] = CheckSum.LRC(values);

        // Send to device
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
        charTimers.setValue(values);
        bleConnector.writeChar(charTimers);

        // Remove from ArrayList
        timers.remove(position);
        adapterTimer.notifyDataSetChanged();
    }

    @Override
    public void enableTimer(int position, boolean enable) {
        Log.i("mytag", "ENABLE TIMER");

        BlanketTimer timer = timers.get(position);
        if(enable) timer.enable = 1;
        else timer.enable = 0;

        byte[] values = timer.getBytes();
        values[0] = TIM_SET;
        values[19] = 0;
        values[19] = CheckSum.LRC(values);

        // Send to device
        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charTimers = service.getCharacteristic(UUID.fromString(svcTimersUUID));
        charTimers.setValue(values);
        bleConnector.writeChar(charTimers);
    }
}
