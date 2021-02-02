package com.example.mdp3_android.bluetooth;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.example.mdp3_android.R;
import com.example.mdp3_android.adapters.DevicesListAdapter;
import com.example.mdp3_android.helper.Constants;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class BluetoothActivity extends AppCompatActivity {

    BluetoothAdapter bluetoothAdapter;
    ArrayList<BluetoothDevice> pairedDevices;
    ArrayList<BluetoothDevice> availDevices;
    DevicesListAdapter pairedDeviceListAdapter;
    DevicesListAdapter availDeviceListAdapter;

    ListView pairedDeviceListView;
    ListView availDeviceListView;
    TextView connectStatus;

    SharedPreferences sharedPreferences;
    SharedPreferences.Editor editor;

    BluetoothService bluetoothConnection;
    public static BluetoothDevice bTDevice;

    boolean retryConnect = false;
    Handler handler = new Handler();

    Runnable runnable = new Runnable() {
        @Override
        public void run() {
           try {
               if(BluetoothService.connStatusFlag == false){
                   connectDevice(bTDevice, Constants.MY_UUID);
                   Toast.makeText(BluetoothActivity.this,
                           "Successfuly reconnected!", Toast.LENGTH_SHORT).show();
               }
               handler.removeCallbacks(runnable);
               retryConnect = false;
           } catch (Exception e){
               Toast.makeText(BluetoothActivity.this,
                                "Unable to reconnect, trying in 5 seconds",
                                Toast.LENGTH_SHORT).show();
           }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        //get default adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        pairedDevices = new ArrayList<>();
        availDevices = new ArrayList<>();

        Switch bluetoothSwitch = (Switch) findViewById(R.id.bluetoothSwitch);
        pairedDeviceListView = (ListView) findViewById(R.id.lvPairedDevices);
        availDeviceListView = (ListView) findViewById(R.id.lvAvailDevices);
        connectStatus = (TextView) findViewById(R.id.tvDeviceStatus);

        if(bluetoothAdapter.isEnabled()){
            bluetoothSwitch.setChecked(true);
            bluetoothSwitch.setText(Constants.BLUETOOTH_ON);
            scanNewDevices();

        } else {
            bluetoothSwitch.setChecked(false);
            bluetoothSwitch.setText(Constants.BLUETOOTH_OFF);
        }

        IntentFilter bondfilter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        registerReceiver(bluetoothBondReceiver, bondfilter);

        IntentFilter connectionfilter = new IntentFilter("ConnectionStatus");
        LocalBroadcastManager.getInstance(this).registerReceiver(connectionReceiver, connectionfilter);

        bluetoothSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isToggled) {
                //check if device supports bluetooth
                if(bluetoothAdapter == null){
                    Toast.makeText(BluetoothActivity.this,
                            "Device does not support Bluetooth", Toast.LENGTH_LONG).show();
                    compoundButton.setChecked(false);
                } else { //device supports bluetooth
                    //device bluetooth is OFF
                    if(!bluetoothAdapter.isEnabled()){

                        //pop up dialog to allow user to allow bluetooth visibility
                        Intent reqDiscoverIntent =
                                new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                        reqDiscoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
                                                    600);

                        startActivity(reqDiscoverIntent);

                        //toggle bluetooth switches
                        compoundButton.setChecked(true);
                        compoundButton.setText(Constants.BLUETOOTH_ON);

                        IntentFilter stateIntent =
                                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(bluetoothStateReceiver, stateIntent);

                        IntentFilter scanIntent =
                                new IntentFilter((BluetoothAdapter.ACTION_SCAN_MODE_CHANGED));
                        registerReceiver(bluetoothScanReceiver, scanIntent);

                    } else { //device bluetooth is ON

                        bluetoothAdapter.disable();
                        compoundButton.setText(Constants.BLUETOOTH_OFF);

                        IntentFilter stateIntent =
                                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
                        registerReceiver(bluetoothStateReceiver, stateIntent);

                        availDeviceListView.setVisibility(View.GONE);
                        pairedDeviceListView.setVisibility(View.GONE);

                    }
                }
            }
        });

        pairedDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                availDeviceListView.setAdapter(availDeviceListAdapter);

//                String deviceName = pairedDevices.get(i).getName();
//                String deviceAddress = pairedDevices.get(i).getAddress();

                bluetoothConnection = new BluetoothService(BluetoothActivity.this);
                bTDevice = pairedDevices.get(i);

                if(bTDevice != null){
                    connectDevice(bTDevice, Constants.MY_UUID);
                }
            }
        });

        availDeviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                bluetoothAdapter.cancelDiscovery();
                pairedDeviceListView.setAdapter(pairedDeviceListAdapter);

                availDevices.get(i).createBond();

                bluetoothConnection = new BluetoothService(BluetoothActivity.this);
                bTDevice = availDevices.get(i);

                if(bTDevice != null){
                    connectDevice(bTDevice, Constants.MY_UUID);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.bluetooth_menu, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        boolean isSelected = false;

        switch (item.getItemId()){
            case android.R.id.home:

                onBackPressed();
                return true;
            case R.id.btnRefreshDevicesList:
                //device bluetooth not on
                //device supports bluetooth
                if (bluetoothAdapter != null) {
                    if (!bluetoothAdapter.isEnabled()) {
                        Toast.makeText(BluetoothActivity.this,
                                        "Please turn on Bluetooth",
                                        Toast.LENGTH_LONG).show();
                    }
                    scanNewDevices();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    public void scanNewDevices(){
        //scan devices
        if(bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();

            checkPermissions();

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(bluetoothAvailDevicesReceiver, discoverIntent);

        } else if(!bluetoothAdapter.isDiscovering()){

            checkPermissions();

            bluetoothAdapter.startDiscovery();
            IntentFilter discoverIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(bluetoothAvailDevicesReceiver, discoverIntent);
        }

        pairedDevices.clear();
        //get paired devices
        Set<BluetoothDevice> pairedDevicesList = bluetoothAdapter.getBondedDevices();
        for (BluetoothDevice device : pairedDevicesList){
            pairedDevices.add(device);
            pairedDeviceListAdapter = new DevicesListAdapter(this, R.layout.lv_devices_list, pairedDevices);
            pairedDeviceListView.setAdapter(pairedDeviceListAdapter);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            int permissionCheck = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                permissionCheck = this.checkSelfPermission("Manifest.permission.ACCESS_COARSE_LOCATION")
                        + this.checkSelfPermission("Manifest.permission.ACCESS_FINE_LOCATION");
            }

            if(permissionCheck != 0){
                this.requestPermissions(new String[] {Manifest.permission.ACCESS_FINE_LOCATION,
                                                    Manifest.permission.ACCESS_COARSE_LOCATION},
                                                    1001);
            }
        }
    }

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                                                    BluetoothAdapter.ERROR);

                switch (state){
                    case BluetoothAdapter.STATE_OFF:
                    case BluetoothAdapter.STATE_TURNING_OFF:
                    case BluetoothAdapter.STATE_TURNING_ON:
                    case BluetoothAdapter.STATE_ON:
                        break;
                }
            }

        }
    };

    private final BroadcastReceiver bluetoothScanReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED.equals(action)){
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE,
                                                    BluetoothAdapter.ERROR);

                switch (mode){
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                    case BluetoothAdapter.SCAN_MODE_NONE:
                    case BluetoothAdapter.STATE_CONNECTING:
                    case BluetoothAdapter.STATE_CONNECTED:
                        break;
                }
            }
        }
    };

     BroadcastReceiver bluetoothAvailDevicesReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            // finding devices
            if(BluetoothDevice.ACTION_FOUND.equals(action)){

                //get BluetoothDevice object from Intent
                BluetoothDevice bluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                //add name and address to an array adapter
                availDevices.add(bluetoothDevice);
                Log.d("Bluetooth Activity", "onReceive: "+ bluetoothDevice.getName() +" : " + bluetoothDevice.getAddress());

                availDeviceListAdapter = new DevicesListAdapter(context,
                        R.layout.lv_devices_list,
                        availDevices);
                availDeviceListView.setAdapter(availDeviceListAdapter);
            }
        }
    };

    private BroadcastReceiver bluetoothBondReceiver = (new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)){
                BluetoothDevice bluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDED){
                    Toast.makeText(BluetoothActivity.this, "Successfully paired with "
                            + bluetoothDevice.getName(), Toast.LENGTH_LONG).show();
                    bTDevice = bluetoothDevice;
                }
                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING){
                    Log.e("Bluetooth Bond", "BOND_BONDING");
                }
                if(bluetoothDevice.getBondState() == BluetoothDevice.BOND_NONE){
                    Log.e("Bluetooth Bond", "BOND_NONE");
                }
            }
        }
    });

    private BroadcastReceiver connectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

        }
    };

    public void connectDevice(BluetoothDevice bluetoothDevice, UUID uuid){
        bluetoothConnection.activateClientThread(bluetoothDevice, uuid);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try{
            unregisterReceiver(bluetoothBondReceiver);
            unregisterReceiver(bluetoothScanReceiver);
            unregisterReceiver(bluetoothStateReceiver);
            unregisterReceiver(bluetoothAvailDevicesReceiver);
        } catch (IllegalArgumentException exception){
            exception.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        try{
            unregisterReceiver(bluetoothBondReceiver);
            unregisterReceiver(bluetoothScanReceiver);
            unregisterReceiver(bluetoothStateReceiver);
            unregisterReceiver(bluetoothAvailDevicesReceiver);
        } catch (IllegalArgumentException exception){
            exception.printStackTrace();
        }
    }
}