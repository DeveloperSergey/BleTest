package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityFacadeInsoleArchive extends AppCompatActivity implements BleConnector.BleCallbacks{

    class ArchiveState{
        protected int length;
        protected long startTimeStamp;
        protected long stopTimeStamp;
    }

    abstract class Record{
        int timeStamp;
        Record(byte[] values){
            timeStamp = ((values[0] & 0xFF) << 0) |
                    ((values[1] & 0xFF) << 8) |
                    ((values[2] & 0xFF) << 16) |
                    ((values[3] & 0xFF) << 24);

        }
    }
    class RecordAlarm extends Record{
        int temperature;
        int humidity;
        RecordAlarm(byte[] values){
            super(values);
            temperature = values[5];
            humidity = values[6];
        }
    }
    class RecordTemperature extends Record{
        int temperature;
        RecordTemperature(byte[] values){
            super(values);
            temperature = values[5];
        }
    }

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcArchiveUUID = "0000fff6-0000-1000-8000-00805f9b34fb";

    Context ctx;
    BluetoothDevice device;
    BleConnector bleConnector;
    Handler handler;

    protected ArrayList<RecordTemperature> valuesTemperature = new ArrayList<>();
    protected ArrayList<Integer> valuesHumidity = new ArrayList<>();
    protected ArrayList<Integer> valuesSteps = new ArrayList<>();
    protected ArrayList<RecordAlarm> valuesAlarm = new ArrayList<>();

    protected int rxCounter;

    ArchiveState archiveState;

    // GUI
    ProgressBar progressBar;
    TextView textViewCount, textViewNum, textViewStartTimeStamp, textViewStopTimeStamp;
    EditText editTextStartTimeStamp, editTextStopTimeStamp;
    GraphView graphView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facade_insole_archive);

        ctx = getApplicationContext();
        getSupportActionBar().hide();
        handler = new Handler();

        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();

        // GUI
        progressBar = (ProgressBar)findViewById(R.id.progressBar);
        textViewCount = (TextView)findViewById(R.id.textViewCount);
        textViewNum = (TextView)findViewById(R.id.textViewNum);
        textViewStartTimeStamp = (TextView)findViewById(R.id.textViewStartTimeStamp);
        textViewStopTimeStamp = (TextView)findViewById(R.id.textViewStopTimeStamp);
        editTextStartTimeStamp = (EditText) findViewById(R.id.editTextStartTimeStamp);
        editTextStopTimeStamp = (EditText) findViewById(R.id.editTextStopTimeStamp);

        graphView = (GraphView) findViewById(R.id.graph);
        if(graphView != null) {
            graphView.getViewport().setYAxisBoundsManual(true);
            graphView.getViewport().setXAxisBoundsManual(true);
            graphView.setTitle("Values");
            graphView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("mytag", "Graph clicked");
                }
            });
        }

        // Classes
        archiveState = new ArchiveState();
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {

    }

    @Override
    public void disconnectedCallback() {

    }

    @Override
    public void writeCharCallback() {

    }

    @Override
    public void readCharCallback(BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();

        if(uuid.toString().equals(svcArchiveUUID)) {
            archiveState.length = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT16, 0);
            archiveState.startTimeStamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 2);
            archiveState.stopTimeStamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 6);
            handler.post(showArchiveState);
        }
    }

    @Override
    public void notificationCallback(BluetoothGattCharacteristic characteristic) {
        rxCounter++;
        Log.i("mytag", "RX: " + bytesToString(characteristic.getValue()));
        long timeStamp = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0);
        int type = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
        switch (type){
            case 1:{    // Alarm
                Log.i("mytag", "ALARM type");
                valuesAlarm.add(new RecordAlarm(characteristic.getValue()));
                break;
            }
            case 2:{    // Temperature
                Log.i("mytag", "TEMP type");
                valuesTemperature.add(new RecordTemperature(characteristic.getValue()));
                break;
            }
            case 8:{    // Steps
                Log.i("mytag", "STEPS type");
                break;
            }
            default: Log.i("mytag", "Unknown type");
        }

        if(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT32, 0) == 0xFFFFFFFF)
            handler.post(showFinished);
        else
            handler.post(showReadProgress);
    }

    @Override
    public void operationFailed(BleConnector.OPERATIONS operation) {

    }

    // GUI
    Runnable showArchiveState = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ctx, "STATE UPDATE", Toast.LENGTH_LONG).show();

            progressBar.setMax(archiveState.length);
            textViewNum.setText(String.valueOf(archiveState.length));
            textViewStartTimeStamp.setText(String.valueOf(archiveState.startTimeStamp));
            textViewStopTimeStamp.setText(String.valueOf(archiveState.stopTimeStamp));
            editTextStartTimeStamp.setText(String.valueOf(archiveState.startTimeStamp));
            editTextStopTimeStamp.setText(String.valueOf(archiveState.stopTimeStamp));
        }
    };

    Runnable showFinished = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ctx, "FINISHED", Toast.LENGTH_LONG).show();

            int maxTemperatureX = -1000000, minTemperatureX = 1000000;
            int maxTemperatureY = -1000000, minTemperatureY = 1000000;

            graphView.removeAllSeries();
            LineGraphSeries<DataPoint> mySeriesTemperature = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesAlarmTemp = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesAlarmHumi = new LineGraphSeries<>();

            for(RecordTemperature record : valuesTemperature){
                Log.i("mytag", "Temp: " + String.valueOf(record.temperature));
                try {
                    mySeriesTemperature.appendData(new DataPoint(record.timeStamp, record.temperature), true, valuesTemperature.size());
                }
                catch (Exception e){
                    Log.i("mytag", e.toString());
                }

                if (record.timeStamp > maxTemperatureX) maxTemperatureX = record.timeStamp;
                if (record.timeStamp < minTemperatureX) minTemperatureX = record.timeStamp;

                if (record.temperature > maxTemperatureY) maxTemperatureY = record.temperature;
                if (record.temperature < minTemperatureY) minTemperatureY = record.temperature;
            }

            for(RecordAlarm record: valuesAlarm){
                Log.i("mytag", "Alarm: " + String.valueOf(record.temperature) + " / " + String.valueOf(record.humidity));
                try {
                    mySeriesAlarmTemp.appendData(new DataPoint(record.timeStamp, 0), true, valuesAlarm.size() * 3);
                    mySeriesAlarmTemp.appendData(new DataPoint(record.timeStamp + 1, record.temperature), true, valuesAlarm.size() * 3);
                    mySeriesAlarmTemp.appendData(new DataPoint(record.timeStamp + 2, 0), true, valuesAlarm.size() * 3);

                    mySeriesAlarmHumi.appendData(new DataPoint(record.timeStamp + 3, 0), true, valuesAlarm.size() * 3);
                    mySeriesAlarmHumi.appendData(new DataPoint(record.timeStamp + 4, record.humidity / 10), true, valuesAlarm.size() * 3);
                    mySeriesAlarmHumi.appendData(new DataPoint(record.timeStamp + 5, 0), true, valuesAlarm.size() * 3);
                }
                catch (Exception e){
                    Log.i("mytag", e.toString());
                }

                if (record.timeStamp > maxTemperatureX) maxTemperatureX = record.timeStamp + 6;
                if (record.timeStamp < minTemperatureX) minTemperatureX = record.timeStamp + 6;

                if (record.temperature > maxTemperatureY) maxTemperatureY = record.temperature;
                if (record.temperature < minTemperatureY) minTemperatureY = record.temperature;
            }

            mySeriesTemperature.setDrawDataPoints(true);
            mySeriesTemperature.setDataPointsRadius(5);
            mySeriesTemperature.setColor(Color.argb(0xff, 0, 0, 255));

            mySeriesAlarmTemp.setDrawDataPoints(true);
            mySeriesAlarmTemp.setDataPointsRadius(5);
            mySeriesAlarmTemp.setColor(Color.argb(0xff, 255, 0, 0));

            mySeriesAlarmHumi.setDrawDataPoints(true);
            mySeriesAlarmHumi.setDataPointsRadius(5);
            mySeriesAlarmHumi.setColor(Color.argb(0xff, 0, 255, 0));

            graphView.addSeries(mySeriesTemperature);
            graphView.addSeries(mySeriesAlarmTemp);
            graphView.addSeries(mySeriesAlarmHumi);

            graphView.getViewport().setMinY(minTemperatureY);
            graphView.getViewport().setMaxY(maxTemperatureY);
            graphView.getViewport().setMinX(minTemperatureX);
            graphView.getViewport().setMaxX(maxTemperatureX);

            graphView.invalidate();
        }
    };

    Runnable showReadProgress = new Runnable() {
        @Override
        public void run() {
            progressBar.setProgress(rxCounter);
            textViewCount.setText(String.valueOf(rxCounter));
        }
    };

    public void getStateOnClick(View view){
        if (!bleConnector.isConnect()) return;

        // Read Archive
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charArchive = service.getCharacteristic(UUID.fromString(svcArchiveUUID));
        bleConnector.readChar(charArchive);

        // Enable notifications
        bleConnector.notiEnable(svUUID, svcArchiveUUID);
    }

    public void readArchiveOnClick(View view){
        valuesTemperature.clear();
        valuesHumidity.clear();
        valuesSteps.clear();
        valuesAlarm.clear();
        byte[] values = new byte[4];
        values[0] = (byte)((archiveState.startTimeStamp >> 0) & 0xFF);
        values[1] = (byte)((archiveState.startTimeStamp >> 8) & 0xFF);
        values[2] = (byte)((archiveState.startTimeStamp >> 16) & 0xFF);
        values[3] = (byte)((archiveState.startTimeStamp >> 24) & 0xFF);

        rxCounter = 0;

        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charArchive = service.getCharacteristic(UUID.fromString(svcArchiveUUID));
        charArchive.setValue(values);
        bleConnector.writeChar(charArchive);
    }

    protected String bytesToString(byte[] bytes){
        StringBuffer sb = new StringBuffer();
        for(byte b : bytes){
            sb.append(String.format("%02x" ,b));
        }
        return sb.toString();
    }
}
