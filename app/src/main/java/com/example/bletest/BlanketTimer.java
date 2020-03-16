package com.example.bletest;

import java.nio.ByteBuffer;

public class BlanketTimer {
    String text;

    int command;
    int time_start, time_stop;
    int time1, time2, time3;
    int power1, power2, power3;
    int type, id, enable, res, lrc;

    BlanketTimer(){}
    BlanketTimer(byte[] values){
        if(values.length == 20){
            command =  (int)(values[0] | (values[1] << 8));
            time_start =  (int)(values[2] | (values[3] << 8));
            time_stop =  (int)(values[4] | (values[5] << 8));
            time1 =  (int)(values[6] | (values[7] << 8));
            time2 =  (int)(values[8] | (values[9] << 8));
            time3 =  (int)(values[10] | (values[11] << 8));
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
}
