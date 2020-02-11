package com.example.bletest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.List;
import java.util.UUID;

public class BleConnector{

    interface BleCallbacks{
        void connectedCallback(List<BluetoothGattService> services);
        void writedCharCallback();
    }

    private Context context;
    private BluetoothDevice device = null;
    private BleCallbacks callbacks;
    public BluetoothGatt bleGatt = null;

    final int STATE_DISCONNECTED = 0;
    final int STATE_CONNECTING = 1;
    final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> services;
    public List<BluetoothGattCharacteristic> chars;

    final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case STATE_DISCONNECTED:
                    Log.i("mytag", "STATE_DISCONNECTED");
                    break;
                case STATE_CONNECTING:
                    Log.i("mytag", "STATE_CONNECTING");
                    break;
                case STATE_CONNECTED:
                    Log.i("mytag", "STATE_CONNECTED");
                    break;
            };

            if (newState == STATE_CONNECTED) {
                if (gatt.discoverServices())
                    Log.i("mytag", "discaverService STARTED");
                else
                    Log.i("mytag", "discaverService FAILE");
            } else if (newState == STATE_DISCONNECTED) {
                bleGatt.disconnect();
                bleGatt.close();
                bleGatt = null;
                device = null;
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt.getServices();
                callbacks.connectedCallback(services);

                /*/ Services
                Log.i("mytag", "SERVICES: ");
                for (int i = 0; i < services.size(); i++) {
                    Log.i("mytag", services.get(i).getUuid().toString());
                }
                BluetoothGattService service = gatt.getService(UUID.fromString(svUUID));*/

                /*/ Characteristics
                Log.i("mytag", "CHARS: ");
                chars = service.getCharacteristics();
                for (int i = 0; i < chars.size(); i++) {
                    Log.i("mytag", chars.get(i).getUuid().toString());
                }*/
            } else {
                Log.i("mytag", "onServicesDiscovered failed!");
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);

            if (status == BluetoothGatt.GATT_SUCCESS)
                callbacks.writedCharCallback();
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicRead(gatt, characteristic, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
            } else
                Log.i("mytag", "Red char failed!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };

    BleConnector(Context ctx, BluetoothDevice device, BleCallbacks callbacks){
        this.context = ctx;
        this.device = device;
        this.callbacks = callbacks;
    }

    public void connect(){
        bleGatt = device.connectGatt(context, false, gattCallback);
    }
    public void disconnect(){
        bleGatt.disconnect();
    }
    public boolean isConnect(){
        if(bleGatt != null){
            if(bleGatt.getConnectionState(device) == STATE_CONNECTED)
                return true;
        }
        return false;
    }

}