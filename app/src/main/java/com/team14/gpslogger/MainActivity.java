package com.team14.gpslogger;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.IOException;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final long MIN_TIME = 1500;
    private static final long MIN_DIST = 100;

    private ILocationService locationService;
    private boolean mRunning;

    private TextView dispLat;
    private TextView dispLong;
    private TextView dispAvgSpeed;

    private static final String[] LOGGING_TAGS = {
            MainActivity.class.getName(),
            GPSLogger.class.getName(),
            LogStream.class.getName()
    };

    private static final char LOGGING_LEVEL = 'D';

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_main);
        dispLat = (TextView) findViewById(R.id.disp_lat);
        dispLong = (TextView) findViewById(R.id.disp_long);
        launchLogcat();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if(mRunning){
            Intent intent = new Intent(this, GPSLogger.class);
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        }
        //if the android location service is not running, start something
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if(!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Log.d(TAG, "onStart: Location Service not enabled");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    public Process launchLogcat(){
        Process process = null;
        //TODO shift this file later into local private storage
        File file = new File(getExternalFilesDir("GPSLogger/logs"),"syslog.txt");
        String cmd = "logcat -r 16 -v time -f " + file.getAbsolutePath();

        //create the ui logging command filter options
        for (String c :
                LOGGING_TAGS) {
            cmd += " " + c + ":" + LOGGING_LEVEL;
        }

        //generic suppression
        cmd += " *:E";

        try {
            process = Runtime.getRuntime().exec(cmd);
        } catch (IOException e) {
            process = null;
        }
        return process;
    }

    /**
     * Start the GPSLogger service
     * @param v the view that made this call
     */
    public void startService(View v){
        Log.d(TAG, "startService: Starting Service");
        if(locationService == null) {
            Intent intent = new Intent(this, GPSLogger.class);
            startService(intent);
            bindService(intent, mConnection, BIND_AUTO_CREATE);
            mRunning = true;
        } else {
            Log.d(TAG, "startService: Service already started");
        }
    }

    /**
     * Stp the GPSLogger Service
     * @param v the view that made this call
     */
    public void stopService(View v){
        Log.d(TAG, "stopService: Stopping Service");
        if(locationService != null){
            unbindService(mConnection);
            Intent intent = new Intent(this, GPSLogger.class);
            stopService(intent);
            Toast.makeText(this, "GPS Logger Stopped", Toast.LENGTH_SHORT).show();
            locationService = null;
            mRunning = false;
        }
    }

    /**
     * Request the GPSLogger for a location information
     * @param v the view that made this call
     */
    public void updateValue(View v) {
        Log.d(TAG, "updateValue: Updating Value");
        if(locationService != null) {
            try {
                Location location = locationService.getLocation();
                if (location != null){
                    dispLat.setText(Double.toString(location.getLatitude()));
                    dispLong.setText(Double.toString(location.getLongitude()));
                } else {
                    Log.d(TAG, "updateValue: No location till now");
                    Toast.makeText(this, "No Location", Toast.LENGTH_SHORT).show();
                }
            } catch (RemoteException e) {
                e.printStackTrace();
                Toast.makeText(this, "Location Service Unavailable", Toast.LENGTH_SHORT).show();
            }

        } else {
            Log.d(TAG, "updateValue: Service not bound");
            Toast.makeText(this, "Location Service Unavailable", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(locationService != null) {
            unbindService(mConnection);
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: Service connected");
            locationService = ILocationService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Service disconnected");
            locationService= null;
        }
    };

}
