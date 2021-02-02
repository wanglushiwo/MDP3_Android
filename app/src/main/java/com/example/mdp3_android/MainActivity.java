package com.example.mdp3_android;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

//import com.example.mdp3_android.adapters.ViewPagerAdapter;
import com.example.mdp3_android.bluetooth.BluetoothActivity;
import com.example.mdp3_android.bluetooth.BluetoothService;
import com.google.android.material.tabs.TabLayout;

import java.nio.charset.Charset;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.btnBluetooth) {
            startActivity(new Intent(this, BluetoothActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public static void printBTMessage(String msg){
        if(BluetoothService.connStatusFlag){
            byte[] bytes = msg.getBytes(Charset.defaultCharset());
            BluetoothService.write(bytes);
        }
    }
}