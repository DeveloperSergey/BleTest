package com.example.bletest;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

public class AdapterTimer extends BaseAdapter {

    interface BlanketTimerCallback{
        void removeTimer(int position);
        void enableTimer(int position, boolean enable);
    }
    BlanketTimerCallback callback;

    Context ctx;
    LayoutInflater lInflater;
    ArrayList<BlanketTimer> timers;

    AdapterTimer(BlanketTimerCallback callback, Context ctx, ArrayList<BlanketTimer> timers){
        this.callback = callback;
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

        TextView textView;
        ((TextView) view.findViewById(R.id.textViewId)).setText(String.valueOf(position+1));
        textView = ((TextView) view.findViewById(R.id.textViewType));
        String s, s2;
        if(timer.type == 0){
            textView.setText("Hard");
            s = "";
        }
        else{
            textView.setText("Soft");
            s = BlanketTimer.timeToString(timer.time_stop);
        }
        if(timer.complete_flag == 0) s2 = "ready";
        else s2 = "complete";

        ((TextView) view.findViewById(R.id.textViewStart)).setText(BlanketTimer.timeToString(timer.time_start));
        ((TextView) view.findViewById(R.id.textViewStop)).setText(s);
        ((TextView) view.findViewById(R.id.textViewState)).setText(s2);
        Switch sw = ((Switch)view.findViewById(R.id.switchOn));
        sw.setTag(position);
        if(timer.enable == 1) sw.setChecked(true);
        else sw.setChecked(false);

        Button button = ((Button)view.findViewById(R.id.button));
        button.setTag(position);
        button.setOnClickListener(myOnClickListener);
        sw.setOnCheckedChangeListener(myCheckedChangeListener);

        return view;
    }

    BlanketTimer getTimer(int position) {
        return ((BlanketTimer) getItem(position));
    }

    View.OnClickListener myOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Toast.makeText(ctx, "DELETE: " + String.valueOf((Integer) v.getTag()), Toast.LENGTH_SHORT).show();
            if(callback != null) callback.removeTimer((Integer) v.getTag());
        }
    };

    CompoundButton.OnCheckedChangeListener myCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Toast.makeText(ctx, "CHANGE: " + String.valueOf(buttonView.getTag()), Toast.LENGTH_SHORT).show();
            if(callback != null) callback.enableTimer((Integer)buttonView.getTag(), isChecked);
        }
    };
}
