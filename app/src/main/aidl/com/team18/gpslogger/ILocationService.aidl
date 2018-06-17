// ILocationService.aidl
package com.team18.gpslogger;

// Declare any non-default types here with import statements

interface ILocationService {

    /**
    * Get the current location of the device, returns null if the location has not been obtained
    * or if the service is not available
    */
    Location getLocation();

    /**
    * Check if the location service is available
    */
    boolean isServiceAvailable();

    /**
    * Get the distance travelled until now in m
    */
    double getDistanceTravelled();

    /**
    * Get the average speed of travel in m/s
    */
    double getAverageSpeed();
}
