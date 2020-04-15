package com.example.bletest;

import android.app.Dialog;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogChooseFacade extends DialogFragment {

    private  BluetoothDevice device;

    public void setDevice(BluetoothDevice device){
        this.device = device;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose Facade")
                .setItems(R.array.test_array, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent intent;
                        switch (which) {
                            case 0:
                                intent = new Intent(getActivity().getApplicationContext(),
                                        ActivityFacadeDevice.class);
                                intent.putExtra("device", device);
                                startActivity(intent);
                                break;
                            case 1:
                                intent = new Intent(getActivity().getApplicationContext(),
                                        ActivityFacadeBlanket.class);
                                intent.putExtra("device", device);
                                startActivity(intent);
                                break;
                            case 2:
                                intent = new Intent(getActivity().getApplicationContext(),
                                        ActivityFacadeInsole.class);
                                intent.putExtra("device", device);
                                startActivity(intent);
                                break;
                            case 3:
                                intent = new Intent(getActivity().getApplicationContext(),
                                        ActivityImageLoader.class);
                                intent.putExtra("device", device);
                                startActivity(intent);
                        }
                    }
                });
        return builder.create();
    }
}
