package com.example.mdp3_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Switch;

public class BluetoothActivity extends AppCompatActivity {

//    BluetoothAdapter bluetoothAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

//        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
//        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
//        if(bluetoothAdapter.isEnabled()){
//            bluetoothSwitch.setChecked(true);
//            bluetoothSwitch.setText("ON");
//        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.btnRefreshDevicesList) {
            //call a function here to scan device list

            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}