package com.team14.gpslogger;

import android.Manifest;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.IntDef;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;

public class GPSLogger extends Service implements LocationListener {
    private static final String TAG = GPSLogger.class.getName();
    private static final String EXTRA_DISTANCE = "EXTRA_DISTANCE";
    private static final String EXTRA_TIME = "EXTRA_TIME";

    private static final long MIN_TIME = 1000;
    private static final float MIN_DISTANCE = 10;

    /**
     * Holds the last updated location of the device
     */
    private Location location;

    /**
     * A flag that denotes whether the location service is available or not
     */
    private boolean serviceAvailable;

    /**
     * The Total Distance travelled till now in m
     */
    private double distanceTravelled = 0.0;

    /**
     * The current average speed in m/s
     */
    private double averageSpeed = 0.0;

    private LogStream logStream;

    @Override
    public void onCreate() {
        super.onCreate();
        try {
            logStream = new LogStream(this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "onStartCommand: GPS Logger Started");
        //reset system every time a start command is issued
        distanceTravelled = 0.0;
        averageSpeed = 0.0;
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            float minDistance = MIN_DISTANCE;
            long minTime = MIN_TIME;
            if (intent != null) {
                minDistance = intent.getFloatExtra(EXTRA_DISTANCE, MIN_DISTANCE);
                minTime = intent.getLongExtra(EXTRA_TIME, MIN_TIME);
            }

            //check minimum time to scan
            if(minTime < 1000){
                Log.w(TAG, "onStartCommand: Minimum time is less than a second");
            }

            //check minimum distance
            if (minDistance < 10){
                Log.w(TAG, "onStartCommand: Minimum Distance is less than 10 meters");
            }

            //register for location service
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "onStartCommand: No permission available");
                serviceAvailable = false;
                return START_NOT_STICKY;
            }
            Log.d(TAG, "onStartCommand: Requesting location updates");

            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, this);

            //Restart service even if it has been closed by the system
            serviceAvailable = true;
            Toast.makeText(this, "GPS Logger Started", Toast.LENGTH_SHORT).show();
            return START_STICKY;
        } else {
            Log.d(TAG, "onStartCommand: No Location Manager available");
            serviceAvailable = false;
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: Stopping service");
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager != null) {
            locationManager.removeUpdates(this);
        }
        if(logStream != null){
            try {
                logStream.closeLog();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind: Service Bound");
        return mBinder;
    }

    @Override
    public void onLocationChanged(Location location) {
        //TODO make calculations here
        Log.d(TAG, "onLocationChanged: long=" + location.getLongitude() + " lat= " + location.getLatitude());
        this.location = location;
        this.averageSpeed = (this.averageSpeed + location.getSpeed())/2.0;
        try {
            logStream.log(location);
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Some Issue with logging", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onStatusChanged(String s, int status, Bundle extras) {
        if(status == LocationProvider.AVAILABLE){
            Log.d(TAG, "onStatusChanged: Location Service Available");
            serviceAvailable = true;
        } else if (status == LocationProvider.OUT_OF_SERVICE){
            Log.e(TAG, "onStatusChanged: Location Service not available");
            serviceAvailable = false;
        } else if (status == LocationProvider.TEMPORARILY_UNAVAILABLE){
            Log.d(TAG, "onStatusChanged: Location Temporarily unavailable");
            serviceAvailable = false;
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.d(TAG, "onProviderEnabled: Provider Enabled");
    }

    @Override
    public void onProviderDisabled(String s) {
        Log.d(TAG, "onProviderDisabled: Provider Disabled");
    }

    private ILocationService.Stub mBinder = new ILocationService.Stub() {
        @Override
        public Location getLocation() throws RemoteException {
            return location;
        }

        @Override
        public boolean isServiceAvailable() throws RemoteException {
            return serviceAvailable;
        }

        @Override
        public double getDistanceTravelled() throws RemoteException {
            return distanceTravelled;
        }

        @Override
        public double getAverageSpeed() throws RemoteException {
            return averageSpeed;
        }
    };
}
