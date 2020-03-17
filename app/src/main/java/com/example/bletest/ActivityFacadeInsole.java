package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityFacadeInsole extends AppCompatActivity implements BleConnector.BleCallbacks {

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcResultUUID = "0000fff3-0000-1000-8000-00805f9b34fb";
    final String svcUserSettingsUUID = "0000fff4-0000-1000-8000-00805f9b34fb";

    Context ctx;
    BluetoothDevice device;
    BleConnector bleConnector;
    ArrayList<Integer> values = new ArrayList<>();
    protected GraphView graph;
    protected ListView listView;
    protected ArrayAdapter<Integer> adapter;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_facade_insole);

        ctx = getApplicationContext();
        getSupportActionBar().hide();
        handler = new Handler();

        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();

        // Graph
        graph = (GraphView) findViewById(R.id.graph);
        graph.getViewport().setYAxisBoundsManual(true);
        graph.getViewport().setXAxisBoundsManual(true);
        graph.setTitle("Humidity");
        graph.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("mytag", "Graph clicked");
            }
        });

        listView = (ListView)findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, values);
        listView.setAdapter(adapter);
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {

        Log.i("mytag", "Connected");
        handler.post(showToastConnect);

        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        UUID CLIENT_CHARACTERISTIC_CONFIG_UUID = BleConnector.convertFromInteger(0x2902);
        BluetoothGattDescriptor descriptor;

        // Enable notification Result
        BluetoothGattCharacteristic charResult = service.getCharacteristic(UUID.fromString(svcResultUUID));
        bleConnector.bleGatt.setCharacteristicNotification(charResult, true);
        descriptor = charResult.getDescriptor(CLIENT_CHARACTERISTIC_CONFIG_UUID);
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        if(bleConnector.writeDesc(descriptor)) Log.i("mytag", "BleConnector");
        else Log.i("mytag", "BleConnector is null");

        // Write settings
        if (bleConnector.bleGatt == null) return;
        BluetoothGattCharacteristic charUserSettings = service.getCharacteristic(UUID.fromString(svcUserSettingsUUID));
        byte[] values = new byte[]{
                (byte) 0, // Time
                (byte) 0,
                (byte) 0,
                (byte) 0,

                (byte) 0, // Pedometer
                (byte) 1, // Measurement period

                (byte) 15, // Temperature
                (byte) 15,

                (byte) 100, // Humidity
                (byte) 100,
        };
        charUserSettings.setValue(values);
        bleConnector.writeChar(charUserSettings);
    }

    @Override
    public void disconnectedCallback() {
        handler.post(showToastDisconnect);
    }

    @Override
    public void writeCharCallback() {

    }

    @Override
    public void readCharCallback(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void notificationCallback(BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();

        if(uuid.toString().equals(svcResultUUID)) {
            //Log.i("mytag", "TIME CALLBACK");
            int humidity = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 6);
            int adc = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);
            values.add(adc);
            handler.post(updateGraph);
            Log.i("mytag", "Humidity = " + String.valueOf(humidity) +
                    " ADC = " + String.valueOf(adc));
        }
    }

    Runnable updateGraph = new Runnable() {
        @Override
        public void run() {
            // ListView
            adapter.notifyDataSetChanged();

            // Graph
            graph.removeAllSeries();

            int maxY = -1000000, minY = 1000000;

            LineGraphSeries<DataPoint> mySeries = new LineGraphSeries<>();

            for(int index = 0; index < values.size(); index++){
                int value = values.get(index);
                mySeries.appendData(new DataPoint(index, value), true, values.size());
                    /*Log.i("mytag", String.valueOf(index) + " " +
                            String.valueOf(value));*/
                if (value > maxY) maxY = value;
                if (value < minY) minY = value;
            }

            mySeries.setDrawDataPoints(true);
            mySeries.setDataPointsRadius(5);

            graph.getViewport().setMinY(minY);
            graph.getViewport().setMaxY(maxY);
            graph.getViewport().setMinX(0);
            graph.getViewport().setMaxX(values.size() - 1);
            LineGraphSeries<DataPoint> series = mySeries;
            graph.addSeries(series);
            graph.invalidate();
        }
    };

    Runnable showToastConnect = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ctx, "CONNECT", Toast.LENGTH_LONG).show();
        }
    };

    Runnable showToastDisconnect = new Runnable() {
        @Override
        public void run() {
            Toast.makeText(ctx, "DISCONNECT", Toast.LENGTH_LONG).show();
        }
    };

}
