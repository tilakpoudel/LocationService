package com.example.locationservice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

//    private Button btn_start, btn_stop;
    private TextView textView;
    private BroadcastReceiver broadcastReceiver;


    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.action_menu,menu);

        MenuItem itemSwitch = menu.findItem(R.id.mySwitch);
        itemSwitch.setActionView(R.layout.switch_layout);
        final Switch sw = (Switch) menu.findItem(R.id.mySwitch).getActionView().findViewById(R.id.switch1);

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if(!runtime_permissions()) {
                    if (isChecked) {
                        Log.d("location", "onCheckedChanged: ture" + isChecked);
                        Intent i = new Intent(getApplicationContext(), LocationService.class);

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                            ContextCompat.startForegroundService(getApplicationContext(),i);
//                        startForegroundService(i);
                        else {
                            startService(i);
                        }

//                        startService(i);
                    Toast.makeText(getBaseContext(),"Location on",Toast.LENGTH_LONG).show();
                    } else {
                        Intent i = new Intent(getApplicationContext(), LocationService.class);
                        Log.d("location", "onCheckedChanged: flase" + isChecked);
                        stopService(i);
                    Toast.makeText(getBaseContext(),"Location off",Toast.LENGTH_LONG).show();

                    }
                }
            }
        });


        return true;
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {

                    textView.append("\nLocation \n" +intent.getExtras().get("coordinates"));

                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        btn_start = (Button) findViewById(R.id.button);
//        btn_stop = (Button) findViewById(R.id.button2);
        textView = (TextView) findViewById(R.id.textView);

//        if(!runtime_permissions())
//            enable_buttons();

    }

 /*   private void enable_buttons() {

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i =new Intent(getApplicationContext(),LocationService.class);
                startService(i);
            }
        });

        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent(getApplicationContext(),LocationService.class);
                stopService(i);

            }
        });

    }
*/
    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
//                enable_buttons();
            }else {
                runtime_permissions();
            }
        }
    }
}