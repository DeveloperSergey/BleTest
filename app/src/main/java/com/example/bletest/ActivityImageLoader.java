package com.example.bletest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivityImageLoader extends AppCompatActivity implements BleConnector.BleCallbacks, DialogChooseFile.DialogChooseFileCallback {

    final String svUUID = "f000ffc0-0451-4000-b000-000000000000";
    final String svcIdentifyUUID = "f000ffc1-0451-4000-b000-000000000000";
    final String svcBlockUUID = "f000ffc2-0451-4000-b000-000000000000";

    // GUI
    TextView textViewDevImgTypeVal, textViewDevImageLenVal;
    TextView textViewUpdateImgTypeVal, textViewUpdateImageLenVal;
    TextView textViewSumImageVal;
    Button buttonUpdate;
    ProgressBar progressBar;

    // Upload
    File[] files;
    class ImageHeader{
        /* Description 16 bytes
        * 2 crc
        * 2 crc shadow
        * 2 ver
        * 2 len
        * 4 uid
        * 4 res*/
        ImageHeader(){}
        ImageHeader(byte[] values){
            if(values.length == 8){
                ver = (int)((values[0] & 0xFF) | ((values[1] & 0xFF) << 8));
                len = (int)((values[2] & 0xFF) | ((values[3] & 0xFF) << 8));
                uid[0] = values[4];
                uid[1] = values[5];
                uid[2] = values[6];
                uid[3] = values[7];
            }
            else if(values.length >= 16){

                /*
                byte[] somebytes = { 1, 5, 5, 0, 1, 0, 5 };
                ByteBuffer bb = ByteBuffer.wrap(somebytes);
                int first = bb.getShort(); //pull off a 16 bit short (1, 5)
                int second = bb.get(); //pull off the next byte (5)
                int third = bb.getInt(); //pull off the next 32 bit int (0, 1, 0, 5)
                */

                ByteBuffer bb = ByteBuffer.wrap(values);
                bb.order(ByteOrder.LITTLE_ENDIAN);
                crc16 = bb.getShort() & 0xFFFF;
                crc16_sh = bb.getShort() & 0xFFFF;
                ver = bb.getShort() & 0xFFFF;
                len = bb.getShort() & 0xFFFF;

                /*crc16 = (int)((values[0] & 0xFF) | ((values[1] & 0xFF) << 8));
                crc16_sh = (int)((values[2] & 0xFF) | ((values[3] & 0xFF) << 8));
                ver = (int)((values[4] & 0xFF) | ((values[5] & 0xFF) << 8));
                len = (int)((values[6] & 0xFF) | ((values[7] & 0xFF) << 8));*/
                uid[0] = values[8];
                uid[1] = values[9];
                uid[2] = values[10];
                uid[3] = values[11];

                res[0] = values[12];
                res[1] = values[13];
                res[2] = values[14];
                res[3] = values[15];
            }
        }

        protected int crc16 = 0;
        protected int crc16_sh = 0;
        protected int ver = 0;
        protected int len = 0;
        byte[] uid = {0,0,0,0};
        byte[] res = {0,0,0,0};

        public byte[] getBytes(){
            byte[] values = new byte[16];

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
            return String.format("%04X", crc16 & 0xFFFF) + "|" +
                    String.format("%04X", crc16_sh & 0xFFFF) + "|" +
                    String.format("%04X", ver & 0xFFFF) + "|" +
                    String.format("%04X", len & 0xFFFF) + "|";
        }
    }
    ImageHeader imageHeaderDevice, imageHeaderUpdate;
    int blockNum, blockNumMax;
    byte[] fileBytes;
    final byte BLOCKS_PER_CONNECTION = 1;
    final int BLOCK_SIZE = 16;

    // App
    Context ctx;
    DialogChooseFile.DialogChooseFileCallback callback = this;

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
        buttonUpdate = (Button)findViewById(R.id.buttonUpdate);
        progressBar = (ProgressBar)findViewById(R.id.progressBar);


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

        pos 12 - oad_blocks_per_connection
        */
        ImageHeader imgHdr = new ImageHeader();
        imgHdr.ver = 0;

        // Read Identification
        //byte[] values = imgHdr.getBytes();
        Log.i("mytag", "GET HDR");
        byte[] values = {0, 0, 0, 0};
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
            Log.i("mytag", "NOTI IDENTIFY");

            byte[] values = characteristic.getValue();
            imageHeaderDevice = new ImageHeader(values);
            Log.i("mytag", imageHeaderDevice.toString());
            Log.i("mytag", String.valueOf(imageHeaderDevice.getImageType()));

            updateImages();
        }
        else if (uuid.toString().equals(svcBlockUUID)) {
            byte[] values = characteristic.getValue();
            blockNum = ((values[1] << 8) & 0xff00) + (values[0] & 0x00ff);
            progressBar.setProgress(blockNum);
            Log.i("mytag", "NEXT BLOCK: " + String.valueOf(blockNum));

            if(blockNum < blockNumMax){
                byte[] data = new byte[18];
                data[0] = (byte)(blockNum & 0xFF);
                data[1] = (byte)((blockNum >> 8) & 0xFF);

                int byteIndex = blockNum * BLOCK_SIZE;
                for(int dataIndex = 2; dataIndex < (BLOCK_SIZE + 2); dataIndex++) {
                    data[dataIndex] = fileBytes[byteIndex++];
                }

                StringBuffer stringBuffer = new StringBuffer();
                for(byte b : data){
                    stringBuffer.append(String.format("%02X", b & 0xFF));
                }
                String strValues = stringBuffer.toString();
                Log.i("mytag", "Block data: " + strValues);

                BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
                BluetoothGattCharacteristic charBlock = service.getCharacteristic(UUID.fromString(svcBlockUUID));
                charBlock.setValue(data);
                bleConnector.writeChar(charBlock);
            }

            if(blockNum == (blockNumMax - 1)){
                showSuccess();
                progressBar.setProgress(0);
            }
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
                if(imageHeaderDevice != null) {
                    textViewDevImgTypeVal.setText(String.valueOf(imageHeaderDevice.getImageType()));
                    textViewDevImageLenVal.setText(Integer.toHexString(imageHeaderDevice.len));
                }

                if(imageHeaderUpdate != null) {
                    textViewUpdateImgTypeVal.setText(String.valueOf(imageHeaderUpdate.getImageType()));
                    textViewUpdateImageLenVal.setText(String.format("%04X", imageHeaderUpdate.len & 0xFFFF));
                    textViewSumImageVal.setTextColor(Color.GREEN);
                }
                if((imageHeaderDevice != null) && (imageHeaderUpdate != null))
                    textViewSumImageVal.setText(String.format("%04X", (imageHeaderDevice.len + imageHeaderUpdate.len) & 0xFFFF));
            }
        });
    }

    protected void showSuccess(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(ctx, "Success", Toast.LENGTH_LONG).show();
            }
        });
    }

    public void chooseFileClicked(View view){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // File
                try {
                    File downloadFolder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    files = downloadFolder.listFiles(
                            new FilenameFilter() {
                                public boolean accept(File directory, String fileName) {
                                    return fileName.endsWith(".bin");
                                }
                            }
                    );
                    Log.i("mytag", "LENGTH " + String.valueOf(files.length));
                    String[] fileNames = new String[files.length];
                    for(int i = 0; i < files.length; i++) {
                        fileNames[i] = files[i].getName();
                    }
                    for (File f : files) {
                        Log.i("mytag", "file: " + f.getName());
                    }

                    // Choose
                    DialogChooseFile dialogChooseFile = new DialogChooseFile(callback, fileNames);
                    dialogChooseFile.show(getSupportFragmentManager(), "dialog");

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void updateClicked(View view){
        blockNum = 0;

        // Start process
        byte[] values = {
                (byte)((imageHeaderUpdate.ver) & 0xFF), (byte)((imageHeaderUpdate.ver >> 8) & 0xFF),
                (byte)((imageHeaderUpdate.len) & 0xFF), (byte)((imageHeaderUpdate.len >> 8) & 0xFF),
                0,0,0,0,0,0,0,0,
                BLOCKS_PER_CONNECTION
        };
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charIdentify = service.getCharacteristic(UUID.fromString(svcIdentifyUUID));
        charIdentify.setValue(values);
        bleConnector.writeChar(charIdentify);
    }
    @Override
    public void fileSelected(int fileIndex) {
        Log.i("mytag", String.valueOf(fileIndex));
        File file = files[fileIndex];
        if(file.exists()){
            Log.i("mytag", "FILE found");

            // Read file
            try {
                InputStream inputStream = new FileInputStream(file);

                fileBytes = new byte[(int)file.length()];
                inputStream.read(fileBytes);
                imageHeaderUpdate = new ImageHeader(fileBytes);

                Log.i("mytag", "File size: " + String.valueOf(fileBytes.length));
                Log.i("mytag", "Len: " + String.valueOf(imageHeaderUpdate.len));
                Log.i("mytag", "Block num: " + String.valueOf(imageHeaderUpdate.len / BLOCK_SIZE));
                progressBar.setMax(fileBytes.length / BLOCK_SIZE);
                blockNumMax = fileBytes.length / BLOCK_SIZE;


                Log.i("mytag", imageHeaderUpdate.toString());
                Log.i("mytag", String.valueOf(imageHeaderUpdate.getImageType()));
                buttonUpdate.setEnabled(true);
                updateImages();

            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
