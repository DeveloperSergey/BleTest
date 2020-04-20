package com.example.bletest;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class DialogChooseFile extends DialogFragment {

    interface DialogChooseFileCallback{
        void fileSelected(int fileIndex);
    }
    private DialogChooseFileCallback callbacks;
    public String fileName = null;
    private String[] files;

    DialogChooseFile(DialogChooseFileCallback callbacks, String[] files){
        this.callbacks = callbacks;
        this.files = files;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Choose File")
                .setItems(files, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Log.i("mytag", String.valueOf(which));
                        fileName = files[which];
                        callbacks.fileSelected(which);
                    }
                });
        return builder.create();
    }
}
