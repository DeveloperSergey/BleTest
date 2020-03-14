package com.example.bletest;

public class CheckSum {
    static byte LRC(byte[] values){
        int lrc = 0;
        for(byte b : values){
            lrc = (byte)(lrc + b);
        }
        lrc = (255 - lrc) + 1;
        return (byte)lrc;
    }
}
