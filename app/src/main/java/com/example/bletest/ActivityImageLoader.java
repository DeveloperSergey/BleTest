package com.example.bletest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.UUID;

public class ActivityImageLoader extends AppCompatActivity implements BleConnector.BleCallbacks{

    final String svUUID = "f000ffc0-0451-4000-b000-000000000000";
    final String svcIdentifyUUID = "f000ffc1-0451-4000-b000-000000000000";
    final String svcBlockUUID = "f000ffc2-0451-4000-b000-000000000000";

    // GUI
    TextView textViewDevImgTypeVal, textViewDevImageLenVal;
    TextView textViewUpdateImgTypeVal, textViewUpdateImageLenVal;
    TextView textViewSumImageVal;

    class ImageHeader{
        ImageHeader(){}
        ImageHeader(byte[] values){
            Log.i("mytag", "values len: " + String.valueOf(values.length));
            if(values.length == 8){
                ver = (short)((values[0] & 0xFF) | ((values[1] & 0xFF) << 8));
                len = (short)((values[2] & 0xFF) | ((values[3] & 0xFF) << 8));
                uid[0] = values[4];
                uid[1] = values[5];
                uid[2] = values[6];
                uid[3] = values[7];
            }
        }

        protected short crc16 = 0;
        protected short ver = 0;
        protected short len = 0;
        byte[] uid = {0,0,0,0};
        byte[] res = {0,0,0,0};

        public byte[] getBytes(){
            byte[] values = new byte[6];

            // CRC16
            values[0] = (byte)(crc16 & 0xFF);
            values[1] = (byte)(crc16 & 0xFF);

            // Version
            values[2] = (byte)(ver & 0xFF);
            values[3] = (byte)(ver & 0xFF);

            // Length
            values[4] = (byte)(len & 0xFF);
            values[5] = (byte)(len & 0xFF);

            // UID
            values[6] = uid[0];
            values[7] = uid[1];
            values[8] = uid[2];
            values[9] = uid[3];

            // RESERVED
            values[10] = res[0];
            values[11] = res[1];
            values[12] = res[2];
            values[13] = res[3];

            return values;
        }

        public char getImageType(){
            return (char)uid[0];
        }

        @NonNull
        @Override
        public String toString() {
            return Integer.toHexString(crc16) + " " +
                    Integer.toHexString(ver) + " " +
                    Integer.toHexString(len) + " " +
                    Integer.toHexString(uid[0]) + Integer.toHexString(uid[1]) + Integer.toHexString(uid[2]) + Integer.toHexString(uid[3]) + " " +
                    Integer.toHexString(res[0]) + Integer.toHexString(res[1]) + Integer.toHexString(res[2]) + Integer.toHexString(res[3]);
        }
    }

    ImageHeader imageHeaderDevice, imageHeaderUpdate;

    // App
    Context ctx;

    // BLE
    BleConnector bleConnector;
    BluetoothDevice device = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_loader);

        ctx = getApplicationContext();
        getSupportActionBar().hide();

        // GUI
        textViewDevImgTypeVal = (TextView)findViewById(R.id.textViewDevImgTypeVal);
        textViewDevImageLenVal = (TextView)findViewById(R.id.textViewDevImageLenVal);
        textViewUpdateImgTypeVal = (TextView)findViewById(R.id.textViewUpdateImgTypeVal);
        textViewUpdateImageLenVal = (TextView)findViewById(R.id.textViewUpdateImageLenVal);
        textViewSumImageVal = (TextView)findViewById(R.id.textViewSumImageVal);


        // BLE
        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {
        Log.i("mytag", "CONNECTED!");
        if (!bleConnector.isConnect()) return;

        // Enable notifications
        bleConnector.notiEnable(svUUID, svcIdentifyUUID);
        bleConnector.notiEnable(svUUID, svcBlockUUID);

        /* HEADER
        u16 crc16
        u16 version
        u16 length
        u8x4 uid (AAAA / BBBB)
        u8x4 res
        */
        ImageHeader imgHdr = new ImageHeader();
        imgHdr.ver = 0;

        // Read Identification
        //byte[] values = imgHdr.getBytes();
        Log.i("mytag", "GET HDR");
        byte[] values = {0, 0, 0, 0, 0, 0};
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charIdentify = service.getCharacteristic(UUID.fromString(svcIdentifyUUID));
        charIdentify.setValue(values);
        bleConnector.writeChar(charIdentify);
    }

    @Override
    public void disconnectedCallback() {

    }

    @Override
    public void writeCharCallback() {

    }

    @Override
    public void readCharCallback(BluetoothGattCharacteristic characteristic) { }

    @Override
    public void notificationCallback(BluetoothGattCharacteristic characteristic) {
        UUID uuid = characteristic.getUuid();
        if (uuid.toString().equals(svcIdentifyUUID)) {
            Log.i("mytag", "NOTI IDENTIFY: " + characteristic.getValue().toString());

            byte[] values = characteristic.getValue();
            imageHeaderDevice = new ImageHeader(values);
            Log.i("mytag", imageHeaderDevice.toString());
            Log.i("mytag", String.valueOf(imageHeaderDevice.getImageType()));

            updateImages();
        }
        else if (uuid.toString().equals(svcIdentifyUUID)) {
            Log.i("mytag", "NOTI BLOCK: " + characteristic.getValue().toString());
        }
    }

    @Override
    public void operationFailed(BleConnector.OPERATIONS operation) {
        Log.i("mytag", "OPERATION FAILED!");
    }

    protected void updateImages(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewDevImgTypeVal.setText(String.valueOf(imageHeaderDevice.getImageType()));
                textViewDevImageLenVal.setText(Integer.toHexString(imageHeaderDevice.len));
            }
        });
    }
}
