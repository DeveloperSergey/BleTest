package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
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
    ArrayList<Integer> valuesHumidityAdc = new ArrayList<>();
    ArrayList<Integer> valuesHumidityNull = new ArrayList<>();
    ArrayList<Integer> valuesHumidityThreshold = new ArrayList<>();
    ArrayList<Integer> valuesHumidity = new ArrayList<>();
    ArrayList<Integer> valuesTemperature = new ArrayList<>();
    protected GraphView graphHumi, graphTemp;
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
        graphHumi = (GraphView) findViewById(R.id.graphHumi);
        graphHumi.getViewport().setYAxisBoundsManual(true);
        graphHumi.getViewport().setXAxisBoundsManual(true);
        graphHumi.setTitle("Humidity");
        graphHumi.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i("mytag", "Graph clicked");
            }
        });
        graphTemp = (GraphView) findViewById(R.id.graphTemp);
        graphTemp.getViewport().setYAxisBoundsManual(true);
        graphTemp.getViewport().setXAxisBoundsManual(true);
        graphTemp.setTitle("Temperature");

        /*listView = (ListView)findViewById(R.id.listView);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, valuesHumidity);
        listView.setAdapter(adapter);*/
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

                (byte) 20, // Temperature
                (byte) 20,

                (byte) 0xc8, // Humidity
                (byte) 0xc8,
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
            int temperature = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 4);
            int adc_temperature = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 5);
            int humidity = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 6);
            int humidityAdc = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 7);
            int humidityNull = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 8);
            int humidityThreshold = characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8, 9);
            valuesHumidity.add(humidity);
            valuesHumidityAdc.add(humidityAdc);
            valuesHumidityNull.add(humidityNull);
            valuesHumidityThreshold.add(humidityThreshold);

            valuesTemperature.add(temperature);
            handler.post(updateGraph);


            Log.i("mytag", "Humidity = " + String.valueOf(humidity) +
                    " ADC = " + String.valueOf(humidityAdc));
        }
    }

    @Override
    public void operationFailed(BleConnector.OPERATIONS operation) {

    }

    Runnable updateGraph = new Runnable() {
        @Override
        public void run() {
            // ListView
            //adapter.notifyDataSetChanged();

            // Graph
            graphHumi.removeAllSeries();
            graphTemp.removeAllSeries();

            int maxHumidityY = -1000000, minHumidityY = 1000000;
            int maxTemperatureY = -1000000, minTemperatureY = 1000000;

            LineGraphSeries<DataPoint> mySeriesHumidity = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesHumidityAdc = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesHumidityNull = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesHumidityThreshold = new LineGraphSeries<>();
            LineGraphSeries<DataPoint> mySeriesTemperature = new LineGraphSeries<>();

            int lastNum = 300;
            int size = valuesHumidityAdc.size();
            int index = 0;
            int graphIndex = 0;
            if(valuesHumidityAdc.size() > lastNum) {
                index = valuesHumidityAdc.size() - lastNum;
                size = lastNum;
            }
            while(graphIndex < size){

                // Humidity
                int value = valuesHumidity.get(index);
                mySeriesHumidity.appendData(new DataPoint(graphIndex, value), true, valuesHumidity.size());
                if (value > maxHumidityY) maxHumidityY = value;
                if (value < minHumidityY) minHumidityY = value;

                // Humidity ADC
                value = valuesHumidityAdc.get(index);
                mySeriesHumidityAdc.appendData(new DataPoint(graphIndex, value), true, valuesHumidityAdc.size());
                if (value > maxHumidityY) maxHumidityY = value;
                if (value < minHumidityY) minHumidityY = value;

                // Humidity Nul
                value = valuesHumidityNull.get(index);
                mySeriesHumidityNull.appendData(new DataPoint(graphIndex, value), true, valuesHumidityNull.size());
                if (value > maxHumidityY) maxHumidityY = value;
                if (value < minHumidityY) minHumidityY = value;

                // Humidity Threshold
                value = valuesHumidityThreshold.get(index);
                mySeriesHumidityThreshold.appendData(new DataPoint(graphIndex, value), true, valuesHumidityThreshold.size());
                if (value > maxHumidityY) maxHumidityY = value;
                if (value < minHumidityY) minHumidityY = value;

                //Temperature
                value = valuesTemperature.get(index);
                mySeriesTemperature.appendData(new DataPoint(graphIndex, value), true, valuesTemperature.size());
                if (value > maxTemperatureY) maxTemperatureY = value;
                if (value < minTemperatureY) minTemperatureY = value;
                index++;
                graphIndex++;
            }

            mySeriesHumidity.setDrawDataPoints(true);
            mySeriesHumidity.setDataPointsRadius(5);
            mySeriesHumidity.setColor(Color.argb(0xff, 0, 0, 255));

            mySeriesHumidityAdc.setDrawDataPoints(true);
            mySeriesHumidityAdc.setDataPointsRadius(5);
            mySeriesHumidityAdc.setColor(Color.argb(0xff, 0, 255, 0));

            mySeriesHumidityNull.setDrawDataPoints(true);
            mySeriesHumidityNull.setDataPointsRadius(5);
            mySeriesHumidityNull.setColor(Color.argb(0xff, 0, 0, 0));

            mySeriesHumidityThreshold.setDrawDataPoints(true);
            mySeriesHumidityThreshold.setDataPointsRadius(5);
            mySeriesHumidityThreshold.setColor(Color.argb(0xff, 255, 0, 0));

            mySeriesTemperature.setDrawDataPoints(true);
            mySeriesTemperature.setDataPointsRadius(5);

            graphHumi.getViewport().setMinY(minHumidityY);
            graphHumi.getViewport().setMaxY(maxHumidityY);
            graphHumi.getViewport().setMinX(0);
            graphHumi.getViewport().setMaxX(size);//(valuesHumidity.size() - 1);

            // Add series
            LineGraphSeries<DataPoint> series;
            series = mySeriesHumidity;
            graphHumi.addSeries(series);
            series = mySeriesHumidityAdc;
            graphHumi.addSeries(series);
            series = mySeriesHumidityNull;
            graphHumi.addSeries(series);
            series = mySeriesHumidityThreshold;
            graphHumi.addSeries(series);
            graphHumi.invalidate();

            graphTemp.getViewport().setMinY(minTemperatureY);
            graphTemp.getViewport().setMaxY(maxTemperatureY);
            graphTemp.getViewport().setMinX(0);
            graphTemp.getViewport().setMaxX(size);//(valuesTemperature.size() - 1);
            series = mySeriesTemperature;
            graphTemp.addSeries(series);
            graphTemp.invalidate();
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
