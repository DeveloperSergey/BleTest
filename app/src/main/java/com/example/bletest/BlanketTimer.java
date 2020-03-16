package com.example.bletest;

import java.nio.ByteBuffer;

public class BlanketTimer {
    String text = "Hello";

    int command;
    int time_start, time_stop;
    int time1, time2, time3;
    int power1, power2, power3;
    int type, id, enable, res, lrc;

    BlanketTimer(){}
    BlanketTimer(byte[] values){
        if(values.length == 20){
            command =  ((values[0]&0xFF) | ((values[1]&0xFF) << 8));
            time_start =  ((values[2]&0xFF) | ((values[3]&0xFF) << 8));
            time_stop =  ((values[4]&0xFF) | ((values[5]&0xFF) << 8));
            time1 =  ((values[6]&0xFF) | ((values[7]&0xFF) << 8));
            time2 =  ((values[8]&0xFF) | ((values[9]&0xFF) << 8));
            time3 =  ((values[10]&0xFF) | ((values[11]&0xFF) << 8));
            power1 =  (int)values[12];
            power2 =  (int)values[13];
            power3 =  (int)values[14];
            type =  (int)values[15];
            id =  (int)values[16];
            enable =  (int)values[17];
            res =  (int)values[18];
            lrc =  (int)values[19];
        }
    }

    static String timeToString(int time){
        return String.valueOf(time / 60) + "h:" +
                String.valueOf(time % 60) + "m";
    }
}
