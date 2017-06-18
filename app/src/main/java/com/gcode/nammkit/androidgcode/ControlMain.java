package com.gcode.nammkit.androidgcode;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class ControlMain extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_control_main);
        //ArduinoMain.mConnectedThread.write("M20\n");
    }
}
