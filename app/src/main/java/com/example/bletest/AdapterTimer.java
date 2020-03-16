package com.example.bletest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AdapterTimer extends BaseAdapter {

    interface BlanketTimerCallback{
        void removeTimer();
    }
    BlanketTimerCallback callback;

    Context ctx;
    LayoutInflater lInflater;
    ArrayList<BlanketTimer> timers;

    AdapterTimer(Context ctx, ArrayList<BlanketTimer> timers){
        this.ctx = ctx;
        this.timers = timers;
        lInflater = (LayoutInflater) ctx
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return timers.size();
    }

    @Override
    public Object getItem(int position) {
        return timers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view = convertView;
        if (view == null) {
            view = lInflater.inflate(R.layout.timer_item, parent, false);
        }

        BlanketTimer timer = getTimer(position);

        ((TextView) view.findViewById(R.id.textViewTitle)).setText(timer.text);
        ((ImageButton)view.findViewById(R.id.imageButton)).setOnClickListener(myOnClickListener);
        ((Switch)view.findViewById(R.id.switchOn)).setOnCheckedChangeListener(myCheckedChangeListener);

        return view;
    }

    BlanketTimer getTimer(int position) {
        return ((BlanketTimer) getItem(position));
    }

    View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Toast.makeText(ctx, "DELETE", Toast.LENGTH_SHORT).show();
            if(callback != null) callback.removeTimer();
        }
    };

    CompoundButton.OnCheckedChangeListener myCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Toast.makeText(ctx, "CHANGE", Toast.LENGTH_SHORT).show();
            if(callback != null) callback.removeTimer();
        }
    };
}
