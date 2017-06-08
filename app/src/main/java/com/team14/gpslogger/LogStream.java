package com.team14.gpslogger;

import android.content.Context;
import android.location.Location;
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
 * Created by aanal on 6/8/17.
 */

public class LogStream {

    private static final String TAG = LogStream.class.getName();

    private static final String XML_HEADER = "<?xml version=\"1.0\" encoding=\"UTF-8\" ?>";

    private static final SimpleDateFormat POINT_DATE_FORMATTER = new SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());

    private static final String TAG_GPX = "<gpx"
            + " xmlns=\"http://www.topografix.com/GPX/1/1\""
            + " version=\"1.1\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd\">";

    private FileWriter fw;

    public LogStream(Context context) throws IOException {
        File sd = context.getExternalFilesDir(null);
        File logDir = new File(sd,"/GPSLogger");
        if(!logDir.exists() && !logDir.mkdirs()){
            Log.e(TAG, "LogStream: Could not Open File To write");
            throw new IllegalStateException("Cannot make log directory");
        }
        File configFile = new File(logDir,"config.conf");
        File logFile = null;
        if(!configFile.exists()){
            //config file does not exist, new configuration
            logFile = new File(logDir, "log.gpx");
            FileOutputStream stream = new FileOutputStream(configFile);
            stream.write(1);
            stream.flush();
            stream.close();
        } else {
            //config file exists
            FileInputStream stream = new FileInputStream(configFile);
            int count =  stream.read();
            stream.close();
            logFile = new File(logDir, String.format("log-%d.gpx",count));
            //update the config count
            count++;
            FileOutputStream outStream = new FileOutputStream(configFile);
            outStream.write(count);
            outStream.flush();
            outStream.close();
        }

        fw = new FileWriter(logFile);

        fw.write(XML_HEADER + "\n");
        fw.write(TAG_GPX + "\n");
        fw.write("\t" + "<trk>" + "\n");
        fw.write("\t\t" + "<name>Mobile Computing Track</name>" + "\n");
        fw.write("\t\t" + "<trkseg>" + "\n");
    }

    public void log(Location data) throws IOException {
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
        }
    }

    public void closeLog() throws IOException {
        fw.write("\t\t" + "</trkseg>" + "\n");
        fw.write("\t" + "</trk>" + "\n");
        fw.write("</gpx>");
        fw.close();
    }

}
