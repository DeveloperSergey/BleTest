package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.ParcelUuid;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    /*TEST GITHUB*/

    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";

    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothLeScanner bleScanner = null;
    private BluetoothDevice bleDevice = null;
    private ArrayList<BluetoothDevice> devices = new ArrayList<>();
    private ArrayList<String> deviceNames = new ArrayList<>();
    BleConnector bleConnector;

    // GUI
    Context ctx;
    EditText editTextDevName;
    ListView listViewDevices;
    ArrayAdapter<String> adapter;

    // Settings
    SharedPreferences sPref;

    // Permission
    final int BT_ENABLE_REQUEST = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this.getApplicationContext();

        // GUI
        editTextDevName = (EditText) findViewById(R.id.editTextDevName);
        editTextDevName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                devices.clear();
                deviceNames.clear();
            }
        });

        listViewDevices = (ListView) findViewById(R.id.listViewDevices);
        adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, deviceNames);
        listViewDevices.setAdapter(adapter);
        listViewDevices.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("mytag", String.valueOf(position));
                if (position < devices.size()) {
                    BluetoothDevice device = devices.get(position);
                    Log.i("mytag", device.getName() + device.getAddress());

                    DialogChooseFacade dialog = new DialogChooseFacade();
                    dialog.setDevice(device);
                    dialog.show(getSupportFragmentManager(), "dialog");

                    /*Intent intent = new Intent(ctx, ActivityFacadeBlanket.class);
                    intent.putExtra("device", device);
                    startActivity(intent);*/
                }
            }
        });

        // Adapter
        BluetoothManager bluetoothManager = (BluetoothManager) this.getSystemService(Context.BLUETOOTH_SERVICE);
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) return;

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, BT_ENABLE_REQUEST);
        } else startScan();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bleScanner != null)
            bleScanner.stopScan(scanCallback);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bleScanner != null)
            bleScanner.stopScan(scanCallback);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (bleScanner != null)
            bleScanner.stopScan(scanCallback);
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == BT_ENABLE_REQUEST && resultCode == Activity.RESULT_OK)
            startScan();
    }

    // Scanner callback
    final ScanCallback scanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            BluetoothDevice device = result.getDevice();

            if (result.getDevice().getName().contains(editTextDevName.getText().toString())) {
                if (!devices.contains(device)) {
                    devices.add(device);
                    deviceNames.add(device.getName());
                    Log.i("mytag", "Name: " + result.getDevice().getName() + " " +
                            "Address: " + result.getDevice().getAddress());
                    Log.i("mytag", String.valueOf(devices.size()));
                    adapter.notifyDataSetChanged();
                }
            }
        }

        @Override
        public void onBatchScanResults(List results) {
        }

        @Override
        public void onScanFailed(int errorCode) {
        }
    };

    public void startScan() {

        devices.clear();
        // Scanner
        if ((bluetoothAdapter != null) && (bluetoothAdapter.isEnabled())) {
            if (bleScanner == null)
                bleScanner = bluetoothAdapter.getBluetoothLeScanner();
            if (bleScanner != null) {
                ScanFilter filter = new ScanFilter.Builder().setServiceUuid(ParcelUuid.fromString(svUUID)).build();
                ArrayList<ScanFilter> filters = new ArrayList<ScanFilter>();
                filters.add(filter);
                ScanSettings settings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
                bleScanner.startScan(filters, settings, scanCallback);

                Log.i("mytag", "Scan is started");
            }
        } else {
            Log.i("mytag", "Bluetooth is disable!");
        }
    }

}
