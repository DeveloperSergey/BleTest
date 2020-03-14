package com.example.bletest;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FragmentPowerModes.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link FragmentPowerModes#newInstance} factory method to
 * create an instance of this fragment.
 */
public class FragmentPowerModes extends Fragment implements PowerMode.PowerModeCallback {

    private SeekBar seekBarPowMod1Time, seekBarPowMod1Val;
    private SeekBar seekBarPowMod2Time, seekBarPowMod2Val;
    private SeekBar seekBarPowMod3Time, seekBarPowMod3Val;
    private TextView textViewPowMod1Time, textViewPowMod1Val;
    private TextView textViewPowMod2Time, textViewPowMod2Val;
    private TextView textViewPowMod3Time, textViewPowMod3Val;
    private PowerMode powerMode1, powerMode2, powerMode3;

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public FragmentPowerModes() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment FragmentPowerModes.
     */
    // TODO: Rename and change types and number of parameters
    public static FragmentPowerModes newInstance(String param1, String param2) {
        FragmentPowerModes fragment = new FragmentPowerModes();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_power_modes, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Power modes
        seekBarPowMod1Time = (SeekBar) getView().findViewById(R.id.seekBarPowMod1Time);
        seekBarPowMod1Time.setMax(10*60);
        seekBarPowMod2Time = (SeekBar) getView().findViewById(R.id.seekBarPowMod2Time);
        seekBarPowMod2Time.setMax(10*60);
        seekBarPowMod3Time = (SeekBar) getView().findViewById(R.id.seekBarPowMod3Time);
        seekBarPowMod3Time.setMax(10*60);
        seekBarPowMod1Val = (SeekBar) getView().findViewById(R.id.seekBarPowMod1Val);
        seekBarPowMod2Val = (SeekBar) getView().findViewById(R.id.seekBarPowMod2Val);
        seekBarPowMod3Val = (SeekBar) getView().findViewById(R.id.seekBarPowMod3Val);
        textViewPowMod1Time = (TextView) getView().findViewById(R.id.textViewPowMod1Time);
        textViewPowMod2Time = (TextView) getView().findViewById(R.id.textViewPowMod2Time);
        textViewPowMod3Time = (TextView) getView().findViewById(R.id.textViewPowMod3Time);
        textViewPowMod1Val = (TextView) getView().findViewById(R.id.textViewPowMod1Val);
        textViewPowMod2Val = (TextView) getView().findViewById(R.id.textViewPowMod2Val);
        textViewPowMod3Val = (TextView) getView().findViewById(R.id.textViewPowMod3Val);
        powerMode1 = new PowerMode(0,this, textViewPowMod1Time, textViewPowMod1Val, seekBarPowMod1Time,seekBarPowMod1Val);
        powerMode2 = new PowerMode(1,this, textViewPowMod2Time, textViewPowMod2Val, seekBarPowMod2Time, seekBarPowMod2Val);
        powerMode3 = new PowerMode(2,this, textViewPowMod3Time, textViewPowMod3Val, seekBarPowMod3Time, seekBarPowMod3Val);

        // Initialization
        seekBarPowMod1Time.setProgress(60*1);
        seekBarPowMod2Time.setProgress(60*1);
        seekBarPowMod3Time.setProgress(60*1);
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }

    @Override
    public void powerModeChangedCallback(int id) {
        Log.i("mytag", "power mode changed in fragment");
    }

    // My method
    public byte[] getBytes(){

        byte[] values = new byte[9];
        SeekBar seekBar;

        // Time mode 1
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod1Time));
        values[0] = (byte)(seekBar.getProgress() & 0xFF);
        values[1] = (byte)((seekBar.getProgress() >> 8) & 0xFF);
        // Time mode 2
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod2Time));
        values[2] = (byte)(seekBar.getProgress() & 0xFF);
        values[3] = (byte)((seekBar.getProgress() >> 8) & 0xFF);
        // Time mode 3
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod3Time));
        values[4] = (byte)(seekBar.getProgress() & 0xFF);
        values[5] = (byte)((seekBar.getProgress() >> 8) & 0xFF);
        // Value mode 1
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod1Val));
        values[6] = (byte)(seekBar.getProgress() & 0xFF);
        // Value mode 2
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod2Val));
        values[7] = (byte)(seekBar.getProgress() & 0xFF);
        // Value mode 3
        seekBar = (SeekBar)(getView().findViewById(R.id.seekBarPowMod3Val));
        values[8] = (byte)(seekBar.getProgress() & 0xFF);

        return values;
    }
}
