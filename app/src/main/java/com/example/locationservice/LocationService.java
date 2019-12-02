package com.example.locationservice;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by tilakpoudel on 30/11/2019.
 */
public class LocationService extends Service {
    private static final String ANDROID_CHANNEL_ID ="location service" ;
    MediaPlayer player;
    private LocationListener listener;
    private LocationManager locationManager;


    OkHttpClient httpClient = new OkHttpClient();
    String url = "http://www.itandrc.com/NepathyaRestApi/api/location";
    Double latitude, longitude;
    String time;
    String device_id;
    FusedLocationProviderClient fusedLocationClient;
    Boolean data_sent_to_server =false;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onCreate() {
        Toast.makeText(this,"Android service created",Toast.LENGTH_LONG).show();

        Log.d("location", "onCreate: service has created");
        player = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
        player.setLooping(true);
//        player.start();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentTitle("Location Service Running")
                    .setSmallIcon(R.drawable.ic_android)
                    .setContentText("Getting Location data")
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);

        }
        else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("location service running")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.drawable.ic_android)
                    .setAutoCancel(true);

            Notification notification = builder.build();

            startForeground(1, notification);
        }
        Log.d("location", "onStartCommand: getting current location.....");

        listener = new LocationListener() {
            @Override
            public void onLocationChanged( Location location) {
                Log.d("location", "onLocationChanged:" + location.getTime() + location.getLatitude());

                String msg="New Latitude: "+location.getLatitude() + "New Longitude: "+location.getLongitude();
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();

                latitude = location.getLatitude();
                longitude = location.getLongitude();
//                time = location.getTime();
                Date currentTime = Calendar.getInstance().getTime();

                Log.d("location", "onLocationChanged: current time"+currentTime);
                time = currentTime.toString();
                device_id = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("device_id", device_id)
                        .addFormDataPart("latitude", latitude.toString())
                        .addFormDataPart("longitude", longitude.toString())
                        .addFormDataPart("time", time.toString())
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .addHeader("AUTH_KEY", "NEPATHYATILAK")
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        e.printStackTrace();
                        final String error ="Failed to push due to :"+ e.toString();
                        Log.d("location", "Failureto push location: "+e.getMessage());
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        Log.d("location", response.body().string());
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getApplicationContext(),
                                        "LOcation pushed", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });

                Intent i = new Intent("location_update");
                i.putExtra("coordinates", location.getLongitude() + " " + location.getLatitude());
                sendBroadcast(i);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
//            return TODO;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                15000,
                1,
                listener);//time is in millisecond and distance in meter
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 15000, 1, listener);



    }

    @SuppressLint("MissingPermission")
    @RequiresApi(api = Build.VERSION_CODES.M)
    /*@Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this,"Android service started",Toast.LENGTH_LONG).show();
        Log.d("location", "onStartCommand: service started");


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentTitle("Location Service Running")
                    .setSmallIcon(R.drawable.ic_android)
                    .setContentText("Getting Location data")
                    .setAutoCancel(true)
                    .build();

            startForeground(1, notification);

        }
        else {

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText("location service running")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.drawable.ic_android)
                    .setAutoCancel(true);

            Notification notification = builder.build();

            startForeground(1, notification);
        }

        Log.d("location", "onStartCommand: getting current location.....");

        listener = new LocationListener() {
            @Override
            public void onLocationChanged( Location location) {
                Log.d("location", "onLocationChanged:" + location.getTime() + location.getLatitude());

                String msg="New Latitude: "+location.getLatitude() + "New Longitude: "+location.getLongitude();
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_LONG).show();

                latitude = location.getLatitude();
                longitude = location.getLongitude();
//                time = location.getTime();
                Date currentTime = Calendar.getInstance().getTime();

                Log.d("location", "onLocationChanged: current time"+currentTime);
                time = currentTime.toString();
                device_id = Settings.Secure.getString(getContentResolver(),
                        Settings.Secure.ANDROID_ID);

                RequestBody requestBody = new MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("device_id", device_id)
                        .addFormDataPart("latitude", latitude.toString())
                        .addFormDataPart("longitude", longitude.toString())
                        .addFormDataPart("time", time.toString())
                        .build();

                Request request = new Request.Builder()
                        .url(url)
                        .post(requestBody)
                        .addHeader("AUTH_KEY", "NEPATHYATILAK")
                        .build();

                httpClient.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        e.printStackTrace();
                        Log.d("location", "Failureto push location: "+e.getMessage());
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        Log.d("location", response.body().string());
                        data_sent_to_server =true;
                    }
                });
                if (data_sent_to_server == true){
                    Log.d("location", "onLocationChanged: location pushed");
                    Toast.makeText(getApplicationContext(),"Location pushed",Toast.LENGTH_LONG).show();

                }
                Intent i = new Intent("location_update");
                i.putExtra("coordinates", location.getLongitude() + " " + location.getLatitude());
                sendBroadcast(i);
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {

            }

            @Override
            public void onProviderEnabled(String s) {

            }

            @Override
            public void onProviderDisabled(String s) {
                Intent i = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            }
        };

        locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    Activity#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for Activity#requestPermissions for more details.
//            return TODO;
        }
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                5000,
                10,
                listener);//time is in millisecond and distance in meter

        return START_REDELIVER_INTENT;
    }
    */
    @Override

    public void onDestroy() {
        player.stop();
        super.onDestroy();
        if(locationManager != null){
            //noinspection MissingPermission
            locationManager.removeUpdates(listener);
        }
    }
}