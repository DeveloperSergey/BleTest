package com.example.bletest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BleConnector{

    interface BleCallbacks{
        void connectedCallback(List<BluetoothGattService> services);
        void disconnectedCallback();
        void writedCharCallback();
        void readedCharCallback(BluetoothGattCharacteristic characteristic);
        void notificationCallback(BluetoothGattCharacteristic characteristic);
    }

    private Context context;
    private BluetoothDevice device = null;
    private BleCallbacks callbacks;
    public BluetoothGatt bleGatt = null;
    private boolean connected = false;
    private ArrayList<BluetoothGattCharacteristic> sequenceToWrite = new ArrayList<>();

    final int STATE_DISCONNECTED = 0;
    final int STATE_CONNECTING = 1;
    final int STATE_CONNECTED = 2;

    public List<BluetoothGattService> services;

    final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            super.onConnectionStateChange(gatt, status, newState);

            switch (newState) {
                case STATE_DISCONNECTED:
                    connected = false;
                    Log.i("mytag", "STATE_DISCONNECTED");
                    break;
                case STATE_CONNECTING:
                    connected = false;
                    Log.i("mytag", "STATE_CONNECTING");
                    break;
                case STATE_CONNECTED:
                    connected = true;
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
                callbacks.disconnectedCallback();
            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            super.onServicesDiscovered(gatt, status);

            if (status == BluetoothGatt.GATT_SUCCESS) {
                services = gatt.getServices();
                callbacks.connectedCallback(services);

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
                callbacks.readedCharCallback(characteristic);
            } else
                Log.i("mytag", "Red char failed!");
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.i("mytag", "RECEIVED NOTI");
            callbacks.notificationCallback(characteristic);
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
        return connected;
    }
    public boolean writeChar(final BluetoothGattCharacteristic characteristic){

        //sequenceToWrite.add(characteristic);
        Thread thread = new Thread(
        new Runnable(){
            @Override
            public void run() {

                for(int tryNum = 0; tryNum < 10; tryNum++) {
                    if(bleGatt.writeCharacteristic(characteristic)){
                        Log.i("mytag", "Write success");
                        break;
                    }
                    else{
                        Log.i("mytag", "Write fail");
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        });
        thread.start();

        return true;
    }
    public boolean readChar(final BluetoothGattCharacteristic characteristic){

        Thread thread = new Thread(
        new Runnable(){
            @Override
            public void run() {
                if(bleGatt.readCharacteristic(characteristic))
                    Log.i("mytag", "Read success");
                else
                    Log.i("mytag", "Read fail");
            }
        });
        thread.start();

        return true;
    }
    public boolean writeDesc(final BluetoothGattDescriptor descriptor){
        Thread thread = new Thread(
                new Runnable(){
                    @Override
                    public void run() {

                        for(int tryNum = 0; tryNum < 10; tryNum++) {
                            if(bleGatt.writeDescriptor(descriptor)){
                                Log.i("mytag", "Write desc success");
                                break;
                            }
                            else{
                                Log.i("mytag", "Write desc fail");
                                try {
                                    Thread.sleep(1000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                });
        thread.start();

        return true;
    }
}