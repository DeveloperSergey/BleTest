package com.example.bletest;

import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceActivity extends AppCompatActivity implements BleConnector.BleCallbacks {

    BleConnector bleConnector;
    SimpleExpandableListAdapter adapter;
    ExpandableListView expandableListView;

    Map<String, String> map;
    ArrayList<Map<String, String>> groupDataList = new ArrayList<>(); // коллекция для групп
    ArrayList<ArrayList<Map<String, String>>> сhildDataList = new ArrayList<>();
    ArrayList<Map<String, String>> сhildDataItemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device);

        expandableListView = (ExpandableListView) findViewById(R.id.expListView);

        Intent intent = getIntent();
        BluetoothDevice device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();


        // список атрибутов групп для чтения
        String groupFrom[] = new String[] { "groupName" };
        // список ID view-элементов, в которые будет помещены атрибуты групп
        int groupTo[] = new int[] { android.R.id.text1 };

        // список атрибутов элементов для чтения
        String childFrom[] = new String[] { "monthName" };
        // список ID view-элементов, в которые будет помещены атрибуты
        // элементов
        int childTo[] = new int[] { android.R.id.text1 };


        adapter = new SimpleExpandableListAdapter(
                this, groupDataList,
                android.R.layout.simple_expandable_list_item_1, groupFrom,
                groupTo, сhildDataList, android.R.layout.simple_list_item_1,
                childFrom, childTo);
        expandableListView.setAdapter(adapter);
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
    public void connectedCallback(List<BluetoothGattService> services) {
        Log.i("mytag", "CONNECTED CALLBACK");

        groupDataList.clear();
        сhildDataList.clear();

        for(BluetoothGattService service : services) {

            map = new HashMap<>();
            map.put("groupName", service.getUuid().toString());
            groupDataList.add(map);

            сhildDataItemList = new ArrayList<>();

            List<BluetoothGattCharacteristic> chars = service.getCharacteristics();
            for (BluetoothGattCharacteristic chara : chars) {
                map = new HashMap<>();
                map.put("monthName", chara.getUuid().toString());
                сhildDataItemList.add(map);
            }

            сhildDataList.add(сhildDataItemList);
        }
        adapter.notifyDataSetChanged();

        // Not work
        expandableListView.invalidateViews();
        expandableListView.refreshDrawableState();
    }

    @Override
    public void writedCharCallback() {

    }
}
