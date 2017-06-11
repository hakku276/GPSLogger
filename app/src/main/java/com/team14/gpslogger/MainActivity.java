package com.team14.gpslogger;

import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.Location;
import android.location.LocationManager;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import io.fabric.sdk.android.Fabric;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = MainActivity.class.getName();
    private static final long UPDATE_TIME = 10000;

    private ILocationService locationService;
    private boolean mRunning;

    private TextView dispLat;
    private TextView dispLong;
    private TextView dispSpeed;
    private TextView dispDistance;
    private CheckBox chkBoxAutoUpdate;
    private Button btnStartStop;

    private Timer timer;
    private TimerTask autoUpdateTask;

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
        dispSpeed = (TextView) findViewById(R.id.disp_speed);
        dispDistance = (TextView) findViewById(R.id.disp_distance);
        chkBoxAutoUpdate = (CheckBox) findViewById(R.id.chk_auto_update);
        btnStartStop = (Button) findViewById(R.id.btn_start_stop);

        timer = new Timer();

        launchLogcat();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (mRunning) {
            Intent intent = new Intent(this, GPSLogger.class);
            bindService(intent, mConnection, BIND_AUTO_CREATE);
        }
        //if the android location service is not running, start something
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Log.d(TAG, "onStart: Location Service not enabled");
            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            startActivity(intent);
        }
    }

    /**
     * Start the GPSLogger service
     *
     * @param v the view that made this call
     */
    public void toggleServiceStatus(View v) {
        if (!mRunning) {
            Log.d(TAG, "startService: Starting Service");
            if (locationService == null) {
                Intent intent = new Intent(this, GPSLogger.class);
                startService(intent);
                bindService(intent, mConnection, BIND_AUTO_CREATE);
                btnStartStop.setText(getString(R.string.btn_stop_service));
                mRunning = true;
                // TODO verify this
                if(chkBoxAutoUpdate.isChecked() && autoUpdateTask == null){
                    scheduleAutoUpdate();
                }
            }
        } else {
            Log.d(TAG, "stopService: Stopping Service");
            if (locationService != null) {
                unbindService(mConnection);
                Intent intent = new Intent(this, GPSLogger.class);
                stopService(intent);
                Toast.makeText(this, "GPS Logger Stopped", Toast.LENGTH_SHORT).show();
                btnStartStop.setText(getString(R.string.btn_start_service));
                locationService = null;
                mRunning = false;
                if(autoUpdateTask != null){
                    autoUpdateTask.cancel();
                }
            }
        }
    }

    public void enableDisableAutoUpdate(View v) {
        Log.d(TAG, "enableDisableAutoUpdate: Check box clicked");
        if (chkBoxAutoUpdate.isChecked() && mRunning) {
            Log.i(TAG, "enableDisableAutoUpdate: Auto update enabled");
            scheduleAutoUpdate();
        } else if(mRunning){
            Log.i(TAG, "enableDisableAutoUpdate: Auto update disabled");
            // the timer should be cancelled
            autoUpdateTask.cancel();
        }
    }

    private void scheduleAutoUpdate(){
        autoUpdateTask = new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateValue(chkBoxAutoUpdate);
                    }
                });
            }
        };
        //start the timer
        timer.scheduleAtFixedRate(autoUpdateTask, UPDATE_TIME, UPDATE_TIME);
    }

    /**
     * Request the GPSLogger for a location information
     *
     * @param v the view that made this call
     */
    public void updateValue(View v) {
        Log.d(TAG, "updateValue: Updating Value");
        if (locationService != null) {
            try {
                Location location = locationService.getLocation();
                if (location != null) {
                    dispLat.setText(Double.toString(location.getLatitude()));
                    dispLong.setText(Double.toString(location.getLongitude()));
                    dispSpeed.setText(Double.toString(locationService.getAverageSpeed()));
                    dispDistance.setText(Double.toString(locationService.getDistanceTravelled()));
                } else if (v != chkBoxAutoUpdate) {
                    //if the view that is calling this is not a check box!! check box was sent as param for auto update calls
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
        if (locationService != null) {
            unbindService(mConnection);
        }
    }

    private Process launchLogcat() {
        Process process = null;
        //TODO shift this file later into local private storage
        File file = new File(getExternalFilesDir("GPSLogger/logs"), "syslog.txt");
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

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            Log.d(TAG, "onServiceConnected: Service connected");
            locationService = ILocationService.Stub.asInterface(iBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            Log.d(TAG, "onServiceDisconnected: Service disconnected");
            locationService = null;
        }
    };

}
