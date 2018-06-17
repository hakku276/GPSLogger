package com.team18.gpslogger;

import android.content.Context;
import android.location.Location;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * The GPX Log Stream
 * Created by aanal on 6/8/17.
 */

class LogStream {

    /**
     * The tag for logcat
     */
    private static final String TAG = LogStream.class.getName();

    /**
     * XML Header
     */
    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

    /**
     * Date formatter for logging the date and time
     */
    private static final SimpleDateFormat POINT_DATE_FORMATTER = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    /**
     * GPX standard meta data
     */
    private static final String TAG_GPX = "<gpx"
            + " xmlns=\"http://www.topografix.com/GPX/1/1\""
            + " version=\"1.1\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";

    /**
     * The file writer that handles writing the file
     */
    private FileWriter fw;

    LogStream() throws IOException {
        //File sd = context.getExternalFilesDir(null);
        File sd = Environment.getExternalStorageDirectory();
        File logDir = new File(sd,"/GPSLogger");

        //create the log dir if it doesn't exist
        if(!logDir.exists() && !logDir.mkdirs()){
            Log.e(TAG, "LogStream: Could not Open File To write");
            throw new IllegalStateException("Cannot make log directory");
        }
        //the config file to hold the current index of the log file
        File configFile = new File(logDir,"config");
        File logFile = null;
        if(!configFile.exists()){
            //config file does not exist, new configuration
            logFile = new File(logDir, "log.gpx");
            FileOutputStream stream = new FileOutputStream(configFile);
            stream.write(1);
            stream.flush();
            stream.close();
        } else {
            //config file exists read the current index of the log file
            FileInputStream stream = new FileInputStream(configFile);
            int count =  stream.read();
            stream.close();

            //the log file
            logFile = new File(logDir, String.format(Locale.getDefault(),"log-%d.gpx",count));

            //update the index count
            count++;
            FileOutputStream outStream = new FileOutputStream(configFile);
            outStream.write(count);
            outStream.flush();
            outStream.close();
        }

        //write the logfile with initial data
        fw = new FileWriter(logFile);

        fw.write(XML_HEADER + "\n");
        fw.write(TAG_GPX + "\n");
        fw.write("\t" + "<trk>" + "\n");
        fw.write("\t\t" + "<name>Mobile Computing Track</name>" + "\n");
        fw.write("\t\t" + "<trkseg>" + "\n");
    }

    /**
     * Log the location data
     * @param data the location data
     * @throws IOException if the data could not be written onto the file
     */
    void log(Location data) throws IOException {
        if(fw != null){
            String out = ("\t\t\t" + "<trkpt lat=\""
                    + data.getLatitude() + "\" " + "lon=\""
                    + data.getLongitude() + "\">" + "\n") +
                    "\t\t\t\t" + "<ele>" + data.getAltitude()
                    + "</ele>" + "\n" +
                    "\t\t\t\t" + "<time>"
                    + POINT_DATE_FORMATTER.format(new Date())
                    + "</time>" + "\n" +
                    "\t\t\t\t" + "<cmt>speed="
                    + data.getSpeed() + "</cmt>" + "\n" +
                    "\t\t\t\t" + "<hdop>" + data.getAccuracy()
                    + "</hdop>" + "\n" +
                    "\t\t\t" + "</trkpt>" + "\n";

            fw.write(out);
        } else {
            throw new IllegalStateException("File Writer is null");
        }
    }

    /**
     * Close the log file, call to this is always necessary to properly maintain the log file
     * @throws IOException if the file could not be written
     */
    void closeLog() throws IOException {
        if(fw != null) {
            fw.write("\t\t" + "</trkseg>" + "\n");
            fw.write("\t" + "</trk>" + "\n");
            fw.write("</gpx>");
            fw.close();
        } else {
            throw new IllegalStateException("File Writer is null");
        }
    }

}
