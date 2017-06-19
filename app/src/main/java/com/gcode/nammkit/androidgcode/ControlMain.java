package com.gcode.nammkit.androidgcode;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;
import java.util.Arrays;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import java.io.InputStream;
import android.os.Handler;
import android.widget.TextView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.content.Context;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.view.View;
import android.view.View.OnClickListener;


public class ControlMain extends AppCompatActivity {

    Button btnJog, btn_xMinusLarge, btn_xMinusSmall, btn_xHome, btn_xAddSmall, btn_xAddLarge;
    Button btnHome, btnOrigin;
    Handler bluetoothIn;

    final int handlerState = 0;                        //used to identify handler message
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private StringBuilder recDataString = new StringBuilder();

    private ConnectedThread mConnectedThread;

    // SPP UUID service - this should work for most devices
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // String for MAC address
    private static String address;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_control_main);

        //Link the buttons and textViews to respective views
        btnJog = (Button) findViewById(R.id.buttonJog);

        btnHome = (Button) findViewById(R.id.homeAll);
        btnOrigin = (Button) findViewById(R.id.origin);

        btn_xMinusLarge = (Button) findViewById(R.id.xMinusLarge);
        btn_xMinusSmall = (Button) findViewById(R.id.xMinusSmall);
        btn_xHome = (Button) findViewById(R.id.xHome);
        btn_xAddSmall = (Button) findViewById(R.id.xAddSmall);
        btn_xAddLarge = (Button) findViewById(R.id.xAddLarge);



        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    String bufferString=recDataString.toString();
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();       // get Bluetooth adapter
        checkBTState();

        btnJog.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Intent i = new Intent(ControlMain.this, ArduinoMain.class);
                i.putExtra(EXTRA_DEVICE_ADDRESS, address);
                startActivity(i);
            }
        });

        btnHome.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G28\n");
            }
        });
        btnOrigin.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G90\nG0X0Y0Z0\n");
            }
        });

        btn_xMinusLarge.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G91\nG0X-10\nG90\n");
            }
        });
        btn_xMinusSmall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G91\nG0X-1\nG90\n");
            }
        });
        btn_xHome.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G28X\n");
            }
        });
        btn_xAddSmall.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G91\nG0X1\nG90\n");
            }
        });
        btn_xAddLarge.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                mConnectedThread.write("G91\nG0X10\nG90\n");
            }
        });
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {

        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connecetion with BT device using UUID
    }

    @Override
    public void onResume() {
        super.onResume();

        //Get MAC address from DeviceListActivity via intent
        Intent intent = getIntent();

        //Get the MAC address from the DeviceListActivty via EXTRA
        address = intent.getStringExtra(DeviceList.EXTRA_DEVICE_ADDRESS);

        //create device and set the MAC address
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establish the Bluetooth socket connection.
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                //insert code to deal with this
            }
        }
        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        //I send a character when resuming.beginning transmission to check device is connected
        //If it is not an exception will be thrown in the write method and finish() will be called
        //recDataString.delete(0,recDataString.length());
        //mConnectedThread.write("M20\n");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Don't leave Bluetooth sockets open when leaving activity
            btSocket.close();
        } catch (IOException e2) {
            //insert code to deal with this
        }
    }

    //Checks that the Android device Bluetooth is available and prompts to be turned on if off
    private void checkBTState() {

        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    //create new class for connect thread
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //creation of the connect thread
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Create I/O streams for connection
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);            //read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();           //converts entered String into bytes
            try {
                mmOutStream.write(msgBuffer);                //write bytes over BT connection via outstream
            } catch (IOException e) {
                //if you cannot write, close the application
                Toast.makeText(getBaseContext(), "Connection Failure", Toast.LENGTH_LONG).show();
                finish();

            }
        }
    }
    private OnItemClickListener mDeviceClickListener = new OnItemClickListener()
    {
        public void onItemClick(AdapterView<?> l, View v, int position, long id)
        {
            String outputStr = String.valueOf(position);
            // Get the device MAC address, which is the last 17 chars in the View
            //String info = ((TextView) v).getText().toString();
            //String address = info.substring(info.length() - 17);
            Toast.makeText(getBaseContext(), outputStr, Toast.LENGTH_SHORT).show();
        }
    };
}
