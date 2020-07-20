package com.example.bletest;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ActivitySkintest extends AppCompatActivity implements BleConnector.BleCallbacks {

    class Settings{

        final int MODE_I = 0;
        final int MODE_Q = 1;
        final int MODE_IQ = 2;
        final int MODE_MPh = 3;
        final int MODE_WORK = 4;

        int mux;
        int afe;
        int dac;
        int adc;
        int mode;
        int num;

        Settings(){
            mux = 0;
            afe = 0;
            dac = 0;
            adc = 0;
            mode = 0;
            num = 20;
        }

        byte[] getBytes(){
            byte nMode = (mode == MODE_MPh) ? (byte)MODE_IQ : (byte)mode;
            byte[] values = { (byte)mux, (byte)afe, (byte)dac, (byte)adc, (byte)(num), (byte)nMode };
            return values;
        }

        @NonNull
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();

            String[] strings = getResources().getStringArray(R.array.sk_mux);
            sb.append(strings[mux]);
            sb.append(" / ");

            strings = getResources().getStringArray(R.array.sk_afe);
            sb.append(strings[afe]);
            sb.append(" / ");

            strings = getResources().getStringArray(R.array.sk_dac);
            sb.append(strings[dac]);
            sb.append("kHz");
            sb.append(" / ");

            strings = getResources().getStringArray(R.array.sk_adc);
            sb.append(strings[adc]);
            sb.append("S/s");
            sb.append(" / ");

            strings = getResources().getStringArray(R.array.sk_mode);
            sb.append(strings[mode]);
            sb.append(" / ");

            sb.append(String.valueOf(num));
            sb.append("pcs");

            return sb.toString();
        }
    }

    final static int STATUS_NONE = 0;
    final int STATUS_START = 1;
    final int STATUS_RESULT = 2;
    final int STATUS_FINISH = 3;
    final int STATUS_TEST_GOOD = 4;
    final int STATUS_TEST_BED = 5;
    final int STATUS_CHARG = 6;
    final int STATUS_STAB = 7;

    Context context;
    BleConnector bleConnector;
    BluetoothDevice device = null;
    final String svUUID = "0000fff0-0000-1000-8000-00805f9b34fb";
    final String svcSettingsUUID = "0000fff2-0000-1000-8000-00805f9b34fb";
    final String svcStartUUID = "0000fff3-0000-1000-8000-00805f9b34fb";
    final String svcResultUUID = "0000fff4-0000-1000-8000-00805f9b34fb";

    Settings settings = new Settings();
    Spinner spinnerMUX, spinnerAFE, spinnerDAC, spinnerADC, spinnerMODE;
    TextView textViewSettings, textViewAvr;
    GraphView graphView, graphView2, graphView3;
    ProgressBar progressBar;
    EditText editTextNum;

    ArrayList<Integer> resultsI = new ArrayList();
    ArrayList<Integer> resultsQ = new ArrayList();
    ArrayList<Integer> resultsM = new ArrayList();
    ArrayList<Integer> resultsPh = new ArrayList<>();
    ArrayList<Integer>[] results = new ArrayList[4];
    ArrayList<Integer> resultsMath = new ArrayList<>();

    long time;
    int averageValue = 0;
    int testToBodyTransitionIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_skintest);

        results[0] = resultsI;
        results[1] = resultsQ;
        results[2] = resultsM;
        results[3] = resultsPh;

        context = getApplicationContext();
        getSupportActionBar().hide();
        Intent intent = getIntent();
        device = (BluetoothDevice)intent.getParcelableExtra("device");
        bleConnector = new BleConnector(this, device, this);
        bleConnector.connect();

        spinnerMUX = (Spinner)findViewById(R.id.spinnerMUX);
        spinnerAFE = (Spinner)findViewById(R.id.spinnerAFE);
        spinnerDAC = (Spinner)findViewById(R.id.spinnerDAC);
        spinnerADC = (Spinner)findViewById(R.id.spinnerADC);
        spinnerMODE = (Spinner)findViewById(R.id.spinnerMode);
        textViewSettings = (TextView)findViewById(R.id.textViewSettings);
        textViewAvr = (TextView)findViewById(R.id.textViewAvr);
        textViewAvr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                averageValue = 0;
                textViewAvr.setText("None");
            }
        });
        progressBar = (ProgressBar)findViewById(R.id.progressBarRes);

        graphView = (GraphView)findViewById(R.id.graphResults);
        graphView.getViewport().setYAxisBoundsManual(true);
        graphView.getViewport().setXAxisBoundsManual(true);
        graphView.getViewport().setScalableY(true);
        graphView.getViewport().setScrollableY(true);
        graphView.getGridLabelRenderer().setNumVerticalLabels(5);
        graphView.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        graphView.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GREEN);
        graphView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                updateGraph();
                return false;
            }
        });

        graphView2 = (GraphView)findViewById(R.id.graphResults2);
        graphView2.getViewport().setYAxisBoundsManual(true);
        graphView2.getViewport().setXAxisBoundsManual(true);
        graphView2.getViewport().setScalableY(true);
        graphView2.getViewport().setScrollableY(true);
        graphView2.getGridLabelRenderer().setNumVerticalLabels(5);
        graphView2.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        graphView2.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GREEN);
        graphView2.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                updateGraph();
                return false;
            }
        });

        graphView3 = (GraphView)findViewById(R.id.graphMath);
        //graphView3.getViewport().setYAxisBoundsManual(true);
        graphView3.getViewport().setXAxisBoundsManual(true);
        graphView3.getViewport().setScalableY(true);
        graphView3.getViewport().setScrollableY(true);
        graphView3.getGridLabelRenderer().setNumVerticalLabels(5);
        graphView3.getGridLabelRenderer().setVerticalLabelsColor(Color.RED);
        graphView3.getGridLabelRenderer().setVerticalLabelsSecondScaleColor(Color.GREEN);
        graphView3.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                updateGraph();
                return false;
            }
        });

        ArrayAdapter<?> adapterMUX = ArrayAdapter.createFromResource(this, R.array.sk_mux, android.R.layout.simple_spinner_item);
        adapterMUX.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMUX.setAdapter(adapterMUX);
        spinnerMUX.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.mux = position;
                textViewSettings.setText(settings.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<?> adapterAFE = ArrayAdapter.createFromResource(this, R.array.sk_afe, android.R.layout.simple_spinner_item);
        adapterMUX.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerAFE.setAdapter(adapterAFE);
        spinnerAFE.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.afe = position;
                textViewSettings.setText(settings.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<?> adapterDAC = ArrayAdapter.createFromResource(this, R.array.sk_dac, android.R.layout.simple_spinner_item);
        adapterMUX.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDAC.setAdapter(adapterDAC);
        spinnerDAC.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.dac = position;
                textViewSettings.setText(settings.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<?> adapterADC = ArrayAdapter.createFromResource(this, R.array.sk_adc, android.R.layout.simple_spinner_item);
        adapterMUX.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerADC.setAdapter(adapterADC);
        spinnerADC.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.adc = position;
                textViewSettings.setText(settings.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        ArrayAdapter<?> adapterNUM = ArrayAdapter.createFromResource(this, R.array.sk_mode, android.R.layout.simple_spinner_item);
        adapterMUX.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMODE.setAdapter(adapterNUM);
        spinnerMODE.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                settings.mode = position;
                textViewSettings.setText(settings.toString());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        editTextNum = (EditText)findViewById(R.id.editTextNum);
        editTextNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String str = editTextNum.getText().toString();
                if(str.length() > 0) {
                    int value = Integer.parseInt(str);
                    settings.num = value;
                    textViewSettings.setText(settings.toString());
                }
            }
        });

        ((ImageView)findViewById(R.id.imageViewStatus)).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                writeDebugCommand((byte)0xBB, (byte)0xBB);
                showTest("BOOTLOADER");
                return false;
            }
        });
    }

    @Override
    public void connectedCallback(List<BluetoothGattService> services) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView)findViewById(R.id.imageViewStatus)).setImageDrawable(context.getDrawable(R.drawable.state_green));
                if (!bleConnector.isConnect()) return;

                // Enable notifications
                bleConnector.notiEnable(svUUID, svcResultUUID);

                // Debug mode
                writeDebugCommand((byte)0xAB, (byte)0xCD);
            }
        });
    }

    @Override
    public void disconnectedCallback() {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((ImageView)findViewById(R.id.imageViewStatus)).setImageDrawable(context.getDrawable(R.drawable.state_red));
            }
        });
    }

    @Override
    public void writeCharCallback() {

    }

    @Override
    public void readCharCallback(BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void notificationCallback(BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().toString().equals(svcResultUUID)){
            byte[] values = characteristic.getValue();
            int status = (short)(0x00ff&(values[0]));
            int value = (short)(0x00ff&(values[1]));
            int real = (short)(0x00ff&(values[2]) | (0xff00&(values[3] << 8)));
            int imaginary = (short)(0x00ff&(values[4]) | (0xff00&(values[5] << 8)));
            int impedance = (short)(0x00ff&(values[6]) | (0xff00&(values[7] << 8)));
            int magnitude = (int)Math.sqrt((real * real) + (imaginary * imaginary));
            int phase = (int)(Math.atan((float)imaginary / real) * 57.2958 * -1);

            if(status == STATUS_START){
                time = System.currentTimeMillis();
                showStart();
            }
            if(status == STATUS_RESULT){
                resultsI.add(real);
                resultsQ.add(imaginary);
                resultsM.add(magnitude);
                resultsPh.add(phase);
                Log.i("mDEBUG", "RES " +
                        String.valueOf(real) + " / " +
                        String.valueOf(imaginary) + " / " +
                        String.valueOf(magnitude) + " / " +
                        String.valueOf(phase));
                updateProgress();
            }
            if(status == STATUS_FINISH){
                time = System.currentTimeMillis() - time;
                if(averageValue == 0) averageValue = resultsM.get(resultsM.size()-1);
                averageValue = (int)((0.7 * averageValue) + (0.3 * resultsM.get(resultsM.size()-1)));
                updateAvr();
                showFinish();
                updateGraph();
            }
            if(status == STATUS_STAB){
                progressBar.setMax(value);
                Log.i("mDEBUG", "MAX = " + String.valueOf(value));
            }
            if( (status == STATUS_TEST_GOOD) || (status == STATUS_TEST_BED) ){
                testToBodyTransitionIndex = impedance;
                Log.i("mDEBUG", "Transition index: " + String.valueOf(testToBodyTransitionIndex));
                if(status == STATUS_TEST_BED) showBadContact();
            }
        }
    }

    @Override
    public void operationFailed(BleConnector.OPERATIONS operation) {

    }

    public void buttonTestStart(View view){
        resultsI.clear();
        resultsQ.clear();
        resultsM.clear();
        resultsPh.clear();
        resultsMath.clear();
        testToBodyTransitionIndex = 0;
        progressBar.setProgress(0);
        progressBar.setMax(settings.num);

        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charSettings = service.getCharacteristic(UUID.fromString(svcSettingsUUID));
        charSettings.setValue(settings.getBytes());
        bleConnector.writeChar(charSettings);
    }

    public void buttonBaseStart(View view){
        resultsI.clear();
        resultsQ.clear();
        resultsM.clear();
        resultsPh.clear();
        resultsMath.clear();
        testToBodyTransitionIndex = 0;
        progressBar.setProgress(0);
        progressBar.setMax(settings.num);

        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charStart = service.getCharacteristic(UUID.fromString(svcStartUUID));
        byte[] value = { (byte)settings.num};
        charStart.setValue(value);
        bleConnector.writeChar(charStart);

        spinnerMODE.setSelection(4);
    }

    protected void updateGraph(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                LineGraphSeries<DataPoint> seriesI = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesQ = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesM = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesPh = new LineGraphSeries<>();

                LineGraphSeries<DataPoint> seriesI2 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesQ2 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesM2 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesPh2 = new LineGraphSeries<>();

                // TODO correction
                LineGraphSeries<DataPoint> seriesM3 = new LineGraphSeries<>();
                LineGraphSeries<DataPoint> seriesMath = new LineGraphSeries<>();

                int index;

                graphView.removeAllSeries();
                graphView.getSecondScale().removeAllSeries();
                graphView2.removeAllSeries();
                graphView2.getSecondScale().removeAllSeries();
                graphView3.removeAllSeries();
                resultsMath.clear();

                for(int array = 0; array < 4; array++){

                    index = 0;
                    int indexMath = 0;
                    for(int i = 0; i < results[array].size();) {

                        if((settings.afe == 0) || (settings.afe == 1) || (settings.mode == settings.MODE_WORK)){
                            int v = results[array].get(i);

                            if((settings.mode != settings.MODE_WORK) || (i < testToBodyTransitionIndex)) {
                                switch (array) {
                                    case 0:
                                        seriesI.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 1:
                                        seriesQ.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 2:
                                        seriesM.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 3:
                                        seriesPh.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                }
                            }
                            else {
                                switch (array) {
                                    case 0:
                                        seriesI2.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 1:
                                        seriesQ2.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 2:
                                        seriesM2.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                    case 3:
                                        seriesPh2.appendData(new DataPoint(index++, v), true, results[array].size());
                                        break;
                                }
                            }
                            i++;
                        }
                        else{
                            int v1 = results[array].get(i);

                            /*/ TODO ----------------------------------------------------------------
                            int g1 = results[array].get(i);
                            int g2 = (i < results[array].size() - 2) ? results[array].get(i+2) : g1;
                            int avr = (g1 + g2) / 2 ;
                            //Log.i("mDEBUG", "AVR: " + String.valueOf(g1) + " / " + String.valueOf(g2) + " / " + String.valueOf(avr));
                            v1 = avr;
                            // ---------------------------------------------------------------------*/

                            int v2 = results[array].get(i + 1);

                            /*/ Normalization
                            float vNorm = 0;
                            if(array == 2) {
                                vNorm =(float) v1 / results[array].get(0);
                                vNorm = v2 / (vNorm);
                                //Log.i("mDEBUG", "Norm " + String.valueOf(vNorm));
                            }*/

                            /*/ Math
                            if( (array == 2) && (i > 1) ){
                                int x1 = resultsM.get(i-1);
                                int x2 = resultsM.get(i+1);
                                int delta = x2 - x1;
                                //float delta = (float)v2 / v1;
                                resultsMath.add((int)delta);

                                seriesMath.appendData(new DataPoint(indexMath+1, delta), true, resultsMath.size());
                                indexMath++;
                                //Log.i("mDEBUG", String.valueOf(x1) + " / " + String.valueOf(x2) + " / " + String.valueOf(delta));
                            }*/

                            switch (array){
                                case 0: seriesI.appendData(new DataPoint(index, v1), true, results[array].size());
                                    seriesI2.appendData(new DataPoint(index++, v2), true, results[array].size());
                                    break;
                                case 1: seriesQ.appendData(new DataPoint(index, v1), true, results[array].size());
                                    seriesQ2.appendData(new DataPoint(index++, v2), true, results[array].size());
                                    break;
                                case 2: seriesM.appendData(new DataPoint(index, v1), true, results[array].size());
                                    seriesM2.appendData(new DataPoint(index++, v2), true, results[array].size());
                                    //seriesM3.appendData(new DataPoint(index++, (int)vNorm), true, results[array].size());
                                    break;
                                case 3:
                                    seriesPh.appendData(new DataPoint(index, v1), true, results[array].size());
                                    seriesPh2.appendData(new DataPoint(index++, v2), true, results[array].size());
                                    break;
                            }
                            i += 2;
                        }
                    }
                }


                seriesI.setDrawDataPoints(true);
                seriesI.setDataPointsRadius(5);
                seriesI.setColor(Color.RED);
                seriesI2.setDrawDataPoints(true);
                seriesI2.setDataPointsRadius(5);
                seriesI2.setColor(Color.RED);

                seriesQ.setDrawDataPoints(true);
                seriesQ.setDataPointsRadius(5);
                seriesQ.setColor(Color.GREEN);
                seriesQ2.setDrawDataPoints(true);
                seriesQ2.setDataPointsRadius(5);
                seriesQ2.setColor(Color.GREEN);

                seriesM.setDrawDataPoints(true);
                seriesM.setDataPointsRadius(5);
                seriesM.setColor(Color.RED);
                seriesM2.setDrawDataPoints(true);
                seriesM2.setDataPointsRadius(5);
                seriesM2.setColor(Color.RED);

                seriesPh.setDrawDataPoints(true);
                seriesPh.setDataPointsRadius(5);
                seriesPh.setColor(Color.GREEN);
                seriesPh2.setDrawDataPoints(true);
                seriesPh2.setDataPointsRadius(5);
                seriesPh2.setColor(Color.GREEN);

                switch (settings.mode){
                    case 0: graphView.addSeries(seriesI); graphView2.addSeries(seriesI2);
                            break;
                    case 1: graphView.addSeries(seriesQ); graphView2.addSeries(seriesQ2);
                            break;
                    case 2: graphView.addSeries(seriesI); graphView2.addSeries(seriesI2);
                            graphView.addSeries(seriesQ); graphView2.addSeries(seriesQ2);
                            break;
                    case 3:
                    case 4:
                            graphView.addSeries(seriesM); graphView2.addSeries(seriesM2); graphView2.addSeries(seriesM3);
                            graphView.getSecondScale().addSeries(seriesPh); graphView2.getSecondScale().addSeries(seriesPh2);
                            graphView3.addSeries(seriesMath);
                            break;
                }
                setAxisGraph();
            }
        });
    }

    protected void updateAvr(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textViewAvr.setText(String.valueOf(averageValue));
            }
        });
    }

    protected void updateProgress(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progressBar.getProgress() + 1);
            }
        });
    }

    protected void showStart(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, "START", Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void showFinish(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, String.format("%.1f",time / 1000.0), Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void showBadContact(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, getResources().getString(R.string.bad_contact), Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void showTest(final String text){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
            }
        });
    }

    protected void setAxisGraph(){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                int[][] min = {
                        {1000000, 1000000},     // I
                        {1000000, 1000000},     // Q
                        {1000000, 1000000},     // M
                        {1000000, 1000000}      // Ph
                };

                int[][] max = {
                        {-1000000, -1000000},   // I
                        {-1000000, -1000000},   // Q
                        {-1000000, -1000000},   // M
                        {-1000000, -1000000}    // Ph
                };

                for(int array = 0; array < 4; array++){
                    for(int i = 0; i < results[array].size();){

                        if( (settings.afe == 0) || (settings.afe == 1) || (settings.mode == settings.MODE_WORK) ){
                            int v = results[array].get(i);
                            if((settings.mode != settings.MODE_WORK) || (i < testToBodyTransitionIndex)) {
                                if (v > max[array][0]) max[array][0] = v;
                                if (v < min[array][0]) min[array][0] = v;
                            }
                            else{
                                if (v > max[array][1]) max[array][1] = v;
                                if (v < min[array][1]) min[array][1] = v;
                            }
                            i++;
                        }
                        else{
                            int v = results[array].get(i);
                            int v2 = results[array].get(i+1);

                            if(v > max[array][0]) max[array][0] = v;
                            if(v < min[array][0]) min[array][0] = v;
                            if(v2 > max[array][1]) max[array][1] = v2;
                            if(v2 < min[array][1]) min[array][1] = v2;
                            i+=2;
                        }
                    }
                }

                for(int array = 0; array < 4; array++){
                    for(int i = 0; i < 2; i++) {
                        if (min[array][i] == max[array][i]) {
                            min[array][i] -= 1;
                            max[array][i] += 1;
                        }
                    }
                }

                int maxX = (settings.afe != 2) ? resultsI.size() : resultsI.size() / 2;
                graphView.getViewport().setMinX(0);
                graphView.getViewport().setMaxX(maxX);

                //int minForSecondGraph = (settings.mode == settings.MODE_WORK) ? testToBodyTransitionIndex : 0;
                //graphView2.getViewport().setMinX(minForSecondGraph);
                graphView2.getViewport().setMinX(0);
                graphView2.getViewport().setMaxX(maxX);

                graphView3.getViewport().setMinX(0);
                graphView3.getViewport().setMaxX(resultsMath.size());

                switch (settings.mode){
                    case 0: {
                        graphView.getViewport().setMinY(min[0][0]);
                        graphView.getViewport().setMaxY(max[0][0]);

                        graphView2.getViewport().setMinY(min[0][1]);
                        graphView2.getViewport().setMaxY(max[0][1]);
                        break;
                    }
                    case 1: {
                        graphView.getViewport().setMinY(min[1][0]);
                        graphView.getViewport().setMaxY(max[1][0]);

                        graphView2.getViewport().setMinY(min[1][1]);
                        graphView2.getViewport().setMaxY(max[1][1]);
                        break;
                    }
                    case 2: {
                        graphView.getViewport().setMinY(min[0][0]);
                        graphView.getViewport().setMaxY(max[0][0]);
                        graphView.getSecondScale().setMinY(min[1][0]);
                        graphView.getSecondScale().setMaxY(max[1][0]);

                        graphView2.getViewport().setMinY(min[0][1]);
                        graphView2.getViewport().setMaxY(max[0][1]);
                        graphView2.getSecondScale().setMinY(min[1][1]);
                        graphView2.getSecondScale().setMaxY(max[1][1]);
                        break;
                    }
                    case 3:
                    case 4:
                        {
                        graphView.getViewport().setMinY(min[2][0]);
                        graphView.getViewport().setMaxY(max[2][0]);
                        graphView.getSecondScale().setMinY(min[3][0]);
                        graphView.getSecondScale().setMaxY(max[3][0]);

                        graphView2.getViewport().setMinY(min[2][1]);
                        graphView2.getViewport().setMaxY(max[2][1]);
                        graphView2.getSecondScale().setMinY(min[3][1]);
                        graphView2.getSecondScale().setMaxY(max[3][1]);
                        break;
                    }
                }
                graphView.invalidate();
                graphView2.invalidate();
            }
        });
    }

    protected void writeDebugCommand(byte command, byte value) {
        byte[] values = new byte[]{
                (byte) 'd', (byte) 'e', (byte) 'b', (byte) 'u', (byte) 'g',
                (byte) 's', (byte) 't', (byte) 'a', (byte) 'r', (byte) 't',
                (byte) command, (byte) value
        };


        if (!bleConnector.isConnect()) return;
        BluetoothGattService service = bleConnector.bleGatt.getService(UUID.fromString(svUUID));
        BluetoothGattCharacteristic charSettings = service.getCharacteristic(UUID.fromString(svcSettingsUUID));
        charSettings.setValue(values);
        bleConnector.writeChar(charSettings);
    }
}