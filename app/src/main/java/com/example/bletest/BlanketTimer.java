package com.example.bletest;

import java.nio.ByteBuffer;

public class BlanketTimer {
    String text = "Hello";

    int command, number;
    int time_start, time_stop;
    int time1, time2, time3;
    int power1, power2, power3;
    int type, complete_flag, enable, res, lrc;

    BlanketTimer(){}
    BlanketTimer(byte[] values){
        if(values.length == 20){
            command =  (int)values[0];
            number = (int)values[1];
            time_start =  ((values[2]&0xFF) | ((values[3]&0xFF) << 8));
            time_stop =  ((values[4]&0xFF) | ((values[5]&0xFF) << 8));
            time1 =  ((values[6]&0xFF) | ((values[7]&0xFF) << 8));
            time2 =  ((values[8]&0xFF) | ((values[9]&0xFF) << 8));
            time3 =  ((values[10]&0xFF) | ((values[11]&0xFF) << 8));
            power1 =  (int)values[12];
            power2 =  (int)values[13];
            power3 =  (int)values[14];
            type =  (int)values[15];
            enable =  (int)values[16];
            complete_flag =  (int)values[17];
            res =  (int)values[18];
            lrc =  (int)values[19];
        }
    }
    byte[] getBytes(){
        byte[] values = new byte[20];

        values[0] = (byte)(command & 0xFF);
        values[1] = (byte)(number & 0xFF);
        values[2] = (byte)(time_start & 0xFF);
        values[3] = (byte)((time_start >> 8) & 0xFF);
        values[4] = (byte)(time_stop & 0xFF);
        values[5] = (byte)((time_stop >> 8) & 0xFF);
        values[6] = (byte)(time1 & 0xFF);
        values[7] = (byte)((time1 >> 8) & 0xFF);
        values[8] = (byte)(time2 & 0xFF);
        values[9] = (byte)((time2 >> 8) & 0xFF);
        values[10] = (byte)(time3 & 0xFF);
        values[11] = (byte)((time3 >> 8) & 0xFF);
        values[12] = (byte)(power1 & 0xFF);
        values[13] = (byte)(power2 & 0xFF);
        values[14] = (byte)(power3 & 0xFF);
        values[15] = (byte)type;
        values[16] = (byte)enable;
        values[17] = (byte)complete_flag;
        values[18] = (byte)res;
        values[19] = (byte)lrc;

        return values;
    }

    static String timeToString(int time){
        return String.valueOf(time / 60) + "h:" +
                String.valueOf(time % 60) + "m";
    }
}
