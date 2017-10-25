/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.apps.location.gps.gnsslogger;

import android.content.Context;
import android.content.Intent;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;
import com.google.android.apps.location.gps.gnsslogger.LoggerFragment.UIFragmentComponent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.text.format.Time;
import java.util.ArrayList;

/**
 * A GNSS logger to store information to a file.
 */
public class FileLogger implements GnssListener {

    private static final String TAG = "FileLogger";
    private static final String FILE_PREFIX = "pseudoranges";
    private static final String FILE_PREFIXSUB = "Locations";
    private static final String FILE_PREFIXACCAZI = "AccAzi";
    private static final String ERROR_WRITING_FILE = "Problem writing to file.";
    private static final String COMMENT_START = "# ";
    private static final char RECORD_DELIMITER = ',';
    private static final String VERSION_TAG = "Version: ";
    private static final String FILE_VERSION = "1.4.0.0, Platform: N";

    private static final int MAX_FILES_STORED = 100;
    private static final int MINIMUM_USABLE_FILE_SIZE_BYTES = 1000;

    private final Context mContext;

    private final Object mFileLock = new Object();
    private  final Object mFileSubLock = new Object();
    private final Object mFileAccAzLock = new Object();
    private BufferedWriter mFileWriter;
    private BufferedWriter mFileSubWriter;
    private BufferedWriter mFileAccAzWriter;
    private File mFile;
    private File mFileSub;
    private File mFileAccAzi;
    private boolean firsttime;
    private UIFragmentComponent mUiComponent;
    private boolean notenoughsat = false;
    private boolean firstOBSforAcc = true;
    private ArrayList<Integer> UsedInFixList = new ArrayList<Integer>() ;

    public synchronized UIFragmentComponent getUiComponent() {
        return mUiComponent;
    }

    public synchronized void setUiComponent(UIFragmentComponent value) {
        mUiComponent = value;
    }
    public FileLogger(Context context) {
        this.mContext = context;
    }
    /**
     * Start a new file logging process.
     */
    public void startNewLog() {
        synchronized (mFileSubLock){
            File baseSubDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseSubDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIXSUB);
                baseSubDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            Date now = new Date();
            int observation = now.getYear() - 100;
            String fileNameSub = String.format("AndroidLocation.kml", FILE_PREFIXSUB);
            File currentFileSub = new File(baseSubDirectory, fileNameSub);
            String currentFileSubPath = currentFileSub.getAbsolutePath();
            BufferedWriter currentFileSubWriter;
            try {
                currentFileSubWriter = new BufferedWriter(new FileWriter(currentFileSub));
            } catch (IOException e) {
                logException("Could not open subobservation file: " + currentFileSubPath, e);
                return;
            }

            // サブ観測ファイルへのヘッダ書き出し
            try {
                currentFileSubWriter.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("<kml xmlns=\"http://earth.google.com/kml/2.0\">");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("<Document>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("<Placemark>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("  <name>Rover Track</name>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("  <Style>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("    <LineStyle>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("      <color>aa00FFFF</color>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("    </LineStyle>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("  </Style>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("  <LineString>");
                currentFileSubWriter.newLine();
                currentFileSubWriter.write("    <coordinates>");
                currentFileSubWriter.newLine();
            } catch (IOException e) {
                Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize subobservation file: " + currentFileSubPath, e);
                return;
            }

            if (mFileSubWriter != null) {
                try {
                    mFileSubWriter.close();
                } catch (IOException e) {
                    logException("Unable to close sub observation file streams.", e);
                    return;
                }
            }
            mFileSub = currentFileSub;
            mFileSubWriter = currentFileSubWriter;
            Toast.makeText(mContext, "File opened: " + currentFileSubPath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:
            // - Remove all empty files
            FileFilter filter = new FileToDeleteFilter(mFileSub);
            for (File existingFile : baseSubDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseSubDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }
        }
        synchronized (mFileAccAzLock){
            if(SettingsFragment.ResearchMode) {
                File baseAccAziDirectory;
                String state = Environment.getExternalStorageState();
                if (Environment.MEDIA_MOUNTED.equals(state)) {
                    baseAccAziDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIXACCAZI);
                    baseAccAziDirectory.mkdirs();
                } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                    logError("Cannot write to external storage.");
                    return;
                } else {
                    logError("Cannot read external storage.");
                    return;
                }

                Date now = new Date();
                String fileNameAccAzi = String.format("AndroidAccAzi.csv", FILE_PREFIXSUB);
                File currentFileAccAzi = new File(baseAccAziDirectory, fileNameAccAzi);
                String currentFileAccAziPath = currentFileAccAzi.getAbsolutePath();
                BufferedWriter currentFileAccAziWriter;
                try {
                    currentFileAccAziWriter = new BufferedWriter(new FileWriter(currentFileAccAzi));
                } catch (IOException e) {
                    logException("Could not open subobservation file: " + currentFileAccAziPath, e);
                    return;
                }

                // サブ観測ファイルへのヘッダ書き出し
                try {
                    currentFileAccAziWriter.write("Android Acc\nEast,North ");
                    currentFileAccAziWriter.newLine();
                } catch (IOException e) {
                    Toast.makeText(mContext, "Count not initialize Sub observation file", Toast.LENGTH_SHORT).show();
                    logException("Count not initialize subobservation file: " + currentFileAccAziPath, e);
                    return;
                }

                if (mFileAccAzWriter != null) {
                    try {
                        mFileAccAzWriter.close();
                    } catch (IOException e) {
                        logException("Unable to close sub observation file streams.", e);
                        return;
                    }
                }
                mFileAccAzi = currentFileAccAzi;
                mFileAccAzWriter = currentFileAccAziWriter;
                Toast.makeText(mContext, "File opened: " + currentFileAccAziPath, Toast.LENGTH_SHORT).show();

                // To make sure that files do not fill up the external storage:
                // - Remove all empty files
                FileFilter filter = new FileToDeleteFilter(mFileAccAzi);
                for (File existingFile : baseAccAziDirectory.listFiles(filter)) {
                    existingFile.delete();
                }
                // - Trim the number of files with data
                File[] existingFiles = baseAccAziDirectory.listFiles();
                int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
                if (filesToDeleteCount > 0) {
                    Arrays.sort(existingFiles);
                    for (int i = 0; i < filesToDeleteCount; ++i) {
                        existingFiles[i].delete();
                    }
                }
            }
        }

        synchronized (mFileLock) {
            File baseDirectory;
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                baseDirectory = new File(Environment.getExternalStorageDirectory(), FILE_PREFIX);
                baseDirectory.mkdirs();
            } else if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
                logError("Cannot write to external storage.");
                return;
            } else {
                logError("Cannot read external storage.");
                return;
            }

            Date now = new Date();
            int observation = now.getYear() - 100;
            String fileName = String.format("AndroidOBS." + observation + "o", FILE_PREFIX);
            File currentFile = new File(baseDirectory, fileName);
            String currentFilePath = currentFile.getAbsolutePath();
            BufferedWriter currentFileWriter;
            try {
                currentFileWriter = new BufferedWriter(new FileWriter(currentFile));
            } catch (IOException e) {
                logException("Could not open observation file: " + currentFilePath, e);
                return;
            }

            // initialize the contents of the file
            try {
                //RINEX Version Type
                currentFileWriter.write("     2.11           OBSERVATION DATA    G (GPS)             RINEX VERSION / TYPE");
                currentFileWriter.newLine();
                //PGM RUNBY DATE
                String PGM = String.format("%-20s","AndroidGNSSReceiver");
                String RUNBY = String.format("%-20s","RITSUMEIKAN KUBOLAB");
                String DATE = String.format("%-20s", now.getTime());
                currentFileWriter.write(PGM + RUNBY + DATE +  "PGM / RUN BY / DATE");
                currentFileWriter.newLine();
                //COMMENT
                //String COMMENT = String.format("%-60s","Android Ver7.0 Nougat");
                //currentFileWriter.write( COMMENT +  "COMMENT");
                //currentFileWriter.newLine();
                //MARKER NAME
                String MARKERNAME = String.format("%-60s",Build.DEVICE);
                currentFileWriter.write(MARKERNAME +  "MARKER NAME");
                currentFileWriter.newLine();
                //MARKER NUMBER
                //OBSERVER AGENCY
                String OBSERVER = String.format("%-20s","GNSSLogger+R");
                String AGENCY = String.format("%-40s","KUBOLAB");
                currentFileWriter.write(OBSERVER + AGENCY +  "OBSERVER / AGENCY");
                currentFileWriter.newLine();
                //REC TYPE VERS
                String REC = String.format("%-20s","0");
                String TYPE = String.format("%-20s","Android Receiver");
                String VERS = String.format("%-20s", Build.VERSION.BASE_OS);
                currentFileWriter.write(REC + TYPE + VERS + "REC # / TYPE / VERS");
                currentFileWriter.newLine();
                //ANT TYPE
                String ANT = String.format("%-20s","0");
                String ANTTYPE = String.format("%-40s","Android Anttena");
                currentFileWriter.write(ANT + ANTTYPE + "ANT # / TYPE");
                currentFileWriter.newLine();
                //APPROX POSITION XYZ
                String X = String.format("%14.4f",0.0);
                String Y = String.format("%14.4f",0.0);
                String Z = String.format("%14.4f",0.0);
                currentFileWriter.write(X + Y + Z + "                  " + "APPROX POSITION XYZ");
                currentFileWriter.newLine();
                //ANTENNA: DELTA H/E/N
                String H = String.format("%14.4f",0.0);
                String E = String.format("%14.4f",0.0);
                String N = String.format("%14.4f",0.0);
                currentFileWriter.write(H + E + N + "                  " + "ANTENNA: DELTA H/E/N");
                currentFileWriter.newLine();
                //WAVELENGTH FACT L1/2
                String WAVELENGTH = String.format("%-6d%-54d",1,0);
                currentFileWriter.write(WAVELENGTH + "WAVELENGTH FACT L1/2");
                currentFileWriter.newLine();
                //# / TYPES OF OBSERV
                if(SettingsFragment.CarrierPhase) {
                    String NUMBEROFOBS = String.format("%-6d", 3);
                    String OBSERV = String.format("%-54s", "    L1    C1    S1");
                    currentFileWriter.write(NUMBEROFOBS + OBSERV + "# / TYPES OF OBSERV");
                    currentFileWriter.newLine();
                }else {
                    String NUMBEROFOBS = String.format("%-6d", 2);
                    String OBSERV = String.format("%-54s", "    C1    S1");
                    currentFileWriter.write(NUMBEROFOBS + OBSERV + "# / TYPES OF OBSERV");
                    currentFileWriter.newLine();
                }
                //INTERVAL
                String INTERVAL = String.format("%-60.3f",1.0);
                currentFileWriter.write(INTERVAL + "INTERVAL");

                currentFileWriter.newLine();
                firsttime = true;
            } catch (IOException e) {
                Toast.makeText(mContext, "Count not initialize observation file", Toast.LENGTH_SHORT).show();
                logException("Count not initialize file: " + currentFilePath, e);
                return;
            }

            if (mFileWriter != null) {
                try {
                    mFileWriter.close();
                } catch (IOException e) {
                    logException("Unable to close all file streams.", e);
                    return;
                }
            }

            mFile = currentFile;
            mFileWriter = currentFileWriter;
            Toast.makeText(mContext, "File opened: " + currentFilePath, Toast.LENGTH_SHORT).show();

            // To make sure that files do not fill up the external storage:
            // - Remove all empty files
            FileFilter filter = new FileToDeleteFilter(mFile);
            for (File existingFile : baseDirectory.listFiles(filter)) {
                existingFile.delete();
            }
            // - Trim the number of files with data
            File[] existingFiles = baseDirectory.listFiles();
            int filesToDeleteCount = existingFiles.length - MAX_FILES_STORED;
            if (filesToDeleteCount > 0) {
                Arrays.sort(existingFiles);
                for (int i = 0; i < filesToDeleteCount; ++i) {
                    existingFiles[i].delete();
                }
            }
        }
    }

    /**
     * Send the current log via email or other options selected from a pop menu shown to the user. A
     * new log is started when calling this function.
     */
    public void send() {
        if (mFile == null) {
            return;
        }
        if (mFileSub == null){
            return;
        }
        if(mFileAccAzi == null && SettingsFragment.ResearchMode){
                return;
        }
        try {
            mFileSubWriter.write("    </coordinates>\n  </LineString>\n</Placemark>\n</Document>\n</kml>\n");
            mFileSubWriter.newLine();
        }catch (IOException e){
            Toast.makeText(mContext, "ERROR_WRITINGFOTTER_FILE", Toast.LENGTH_SHORT).show();
            logException(ERROR_WRITING_FILE, e);
        }

        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        emailIntent.setType("*/*");
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "SensorLog");
        emailIntent.putExtra(Intent.EXTRA_TEXT, "");
        // attach the file
        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFile));
        getUiComponent().startActivity(Intent.createChooser(emailIntent, "Send RINEX.."));
        if(SettingsFragment.ResearchMode) {
            Intent emailIntentSub = new Intent(Intent.ACTION_SEND);
            emailIntentSub.setType("*/*");
            emailIntentSub.putExtra(Intent.EXTRA_SUBJECT, "SensorSubLog");
            emailIntentSub.putExtra(Intent.EXTRA_TEXT, "");
            // attach the file
            emailIntentSub.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFileSub));
            getUiComponent().startActivity(Intent.createChooser(emailIntentSub, "Send KML.."));
        }

        Intent emailIntentAccAzi = new Intent(Intent.ACTION_SEND);
        emailIntentAccAzi.setType("*/*");
        emailIntentAccAzi.putExtra(Intent.EXTRA_SUBJECT, "SensorAccAziLog");
        emailIntentAccAzi.putExtra(Intent.EXTRA_TEXT, "");
        // attach the file
        emailIntentAccAzi.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(mFileAccAzi));
        getUiComponent().startActivity(Intent.createChooser(emailIntentAccAzi, "Send SensorLog ..."));

        if (mFileWriter != null) {
            try {
                mFileWriter.close();
                mFileWriter = null;
            } catch (IOException e) {
                logException("Unable to close all file streams.", e);
                return;
            }
        }

        if(mFileSubWriter != null) {
            try {
                mFileSubWriter.close();
                mFileSubWriter = null;
            } catch (IOException e) {
                logException("Unable to close subobservation file streams.", e);
                return;
            }
        }
        if(SettingsFragment.ResearchMode) {
            if (mFileAccAzWriter != null) {
                try {
                    mFileAccAzWriter.close();
                    mFileAccAzWriter = null;
                } catch (IOException e) {
                    logException("Unable to close sensorlog file streams.", e);
                    return;
                }
            }
        }
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onProviderDisabled(String provider) {}

    @Override
    public void onLocationChanged(Location location) {
        if (location.getProvider().equals(LocationManager.GPS_PROVIDER)) {
            synchronized (mFileSubLock) {
                if (mFileSubWriter == null) {
                    return;
                }
                else{
                    try {
                        String locationStream =
                                String.format(
                                        Locale.US,
                                        "       %15.9f,%15.9f,%15.9f",
                                        location.getLongitude(),
                                        location.getLatitude(),
                                        location.getAltitude());
                        mFileSubWriter.write(locationStream);
                        mFileSubWriter.newLine();
                    }catch (IOException e){
                        Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                        logException(ERROR_WRITING_FILE, e);
                    }
                }
            }
        }
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {}

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            GnssClock gnssClock = event.getClock();
            for (GnssMeasurement measurement : event.getMeasurements()) {
                try {
                    if(firsttime == true){
                        double weekNumber = Math.floor( - (double)(gnssClock.getFullBiasNanos())*1e-9/604800);

                        long weekNumberNanos = (long)weekNumber*(long)(604800*1e9);

                        double tRxNanos = gnssClock.getTimeNanos() -gnssClock.getFullBiasNanos() - weekNumberNanos;
                        double tRxSeconds = 0;
                        if(gnssClock.hasBiasNanos() == false) {
                            tRxSeconds = (tRxNanos - measurement.getTimeOffsetNanos()) * 1e-9;
                        }
                        else{
                            tRxSeconds = (tRxNanos - measurement.getTimeOffsetNanos() - gnssClock.getBiasNanos()) * 1e-9;
                        }
                        double tTxSeconds  = (measurement.getReceivedSvTimeNanos())*1e-9;
                        //GPS週のロールオーバーチェック
                        double prSeconds  = tRxSeconds - tTxSeconds;
                        if(prSeconds > 604800/2) {
                            double delS = Math.round(prSeconds / 604800) * 604800;
                            double prS = prSeconds - delS;
                            double maxBiasSeconds = 10;
                            if(prS > maxBiasSeconds) {
                                Toast.makeText(mContext, "RollOverError", Toast.LENGTH_SHORT).show();
                            }
                            else{
                                tRxSeconds = tRxSeconds - delS;
                            }
                        }
                        GPSWStoGPST gpswStoGPST = new GPSWStoGPST();
                        ReturnValue value = gpswStoGPST.method(weekNumber , tRxSeconds);
                        String StartTimeOBS = String.format("%6d%6d%6d%6d%6d%13.7f     %3s         TIME OF FIRST OBS\n",value.Y,value.M,value.D,value.h,value.m,value.s,"GPS");
                        //END OF HEADER
                        String ENDOFHEADER = String.format("%73s","END OF HEADER");
                        mFileWriter.write(StartTimeOBS + ENDOFHEADER);
                        mFileWriter.newLine();
                        firsttime = false;
                    }
                    else{

                    }
                } catch (IOException e) {
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }
            }
            try {
                writeGnssMeasurementToFile(gnssClock,event);
            } catch (IOException e){
                logException(ERROR_WRITING_FILE, e);
            }
        }
        firstOBSforAcc = true;
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {}

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage navigationMessage) {
        synchronized (mFileLock) {
            if (mFileWriter == null) {
                return;
            }
            StringBuilder builder = new StringBuilder("Nav");
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getSvid());
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getType());
            builder.append(RECORD_DELIMITER);

            int status = navigationMessage.getStatus();
            builder.append(status);
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getMessageId());
            builder.append(RECORD_DELIMITER);
            builder.append(navigationMessage.getSubmessageId());
            byte[] data = navigationMessage.getData();
            for (byte word : data) {
                builder.append(RECORD_DELIMITER);
                builder.append(word);
            }
            try {
                mFileWriter.write(builder.toString());
                mFileWriter.newLine();
            } catch (IOException e) {
                logException(ERROR_WRITING_FILE, e);
            }
        }
    }
    public void onSensorListener(String listener,float azimuth,float accZ,float altitude){
        synchronized (mFileAccAzLock) {
            if (mFileAccAzWriter == null) {
                return;
            }
            else{
                try {
                    String SensorStream =
                            String.format("%f,%f,%f", (float)(accZ * Math.sin(azimuth)),(float)(accZ * Math.cos(azimuth)),altitude);
                    mFileAccAzWriter.write(SensorStream);
                    mFileAccAzWriter.newLine();
                }catch (IOException e){
                    Toast.makeText(mContext, "ERROR_WRITING_FILE", Toast.LENGTH_SHORT).show();
                    logException(ERROR_WRITING_FILE, e);
                }
            }
        }
    }
    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
    }

    @Override
    public void  onGnssStatusChanged(GnssStatus gnssStatus) {
        try {
            writeUseInFixArray(gnssStatus);
        }catch (IOException e){
            Toast.makeText(mContext, "FATAL_ERROR_FOR_WRITING_ARRAY", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {}

    @Override
    public void onListenerRegistration(String listener, boolean result) {}

    private void writeUseInFixArray(GnssStatus gnssStatus) throws IOException{
        for (int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            if (gnssStatus.usedInFix(i) && gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS) {
                if (UsedInFixList.indexOf(gnssStatus.getSvid(i)) == -1) {
                    UsedInFixList.add(gnssStatus.getSvid(i));
                }
            } else if(gnssStatus.getConstellationType(i) == GnssStatus.CONSTELLATION_GPS) {
                int index = UsedInFixList.indexOf(gnssStatus.getSvid(i));
                if (index != -1) {
                    UsedInFixList.remove(index);
                }
            }
        }
    }

    private boolean ReadUseInFixArray(int Svid) throws IOException{
        return UsedInFixList.indexOf(Svid) != -1;
    }

    private void writeGnssMeasurementToFile(GnssClock clock, GnssMeasurementsEvent event) throws IOException {
        StringBuilder Time = new StringBuilder();
        StringBuilder Prn = new StringBuilder();
        StringBuilder Measurements = new StringBuilder();
        String SensorStream = "";
        boolean firstOBS = true;
        int satnumber = 0;
        for ( GnssMeasurement measurement : event.getMeasurements()) {
            if(measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {

                /*double weekNumber = Math.floor(-(double) (clock.getFullBiasNanos()) * 1e-9 / 604800);

                long weekNumberNanos = (long) weekNumber * (long) (604800 * 1e9);

                double tRxNanos = clock.getTimeNanos() - clock.getFullBiasNanos() - weekNumberNanos;
                double tRxSeconds = 0;
                if (clock.hasBiasNanos() == false) {
                    tRxSeconds = (tRxNanos - measurement.getTimeOffsetNanos()) * 1e-9;
                } else {
                    tRxSeconds = (tRxNanos - measurement.getTimeOffsetNanos() - clock.getBiasNanos()) * 1e-9;
                }
                */
                GnssClock gnssClock = event.getClock();
                double tRxSeconds;
                double weekNumber = Math.floor((double) (gnssClock.getTimeNanos()) * 1e-9 / 604800);
                if (gnssClock.hasBiasNanos() == false) {
                    tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos()) * 1e-9;
                } else {
                    tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - gnssClock.getBiasNanos()) * 1e-9;
                }
                double tTxSeconds = (measurement.getReceivedSvTimeNanos()) * 1e-9;
                //GPS週のロールオーバーチェック
               /* boolean iRollover = prSeconds > 604800 / 2;
                if (iRollover) {
                    double delS = Math.round(prSeconds / 604800) * 604800;
                    double prS = prSeconds - delS;
                    double maxBiasSeconds = 10;
                    if (prS > maxBiasSeconds) {
                        Log.e("RollOver", "Rollover Error");
                    } else {
                        tRxSeconds = tRxSeconds - delS;
                    }
                }*/

                //GPS週・週秒から年月日時分秒に変換
                GPSWStoGPST gpswStoGPST = new GPSWStoGPST();
                ReturnValue value = gpswStoGPST.method(weekNumber, tRxSeconds);
                /*急場の変更！！*/
                String DeviceName = Build.DEVICE;
                //Log.d("DEVICE",DeviceName);
                if(DeviceName.indexOf("shamu") != -1) {
                    String tRxStr = String.valueOf(-gnssClock.getFullBiasNanos());
                    String tTxStr = String.valueOf(measurement.getReceivedSvTimeNanos());
                    tTxSeconds = Float.parseFloat(tTxStr.substring(tTxStr.length() - 10));
                    tRxSeconds = Float.parseFloat(tRxStr.substring(tRxStr.length() - 10));
                }
                /*急場の変更！！*/
                double prSeconds = (tRxSeconds - tTxSeconds)*1e-9;
                double prm = prSeconds * 2.99792458e8;
                //コード擬似距離の計算
                //搬送波位相も計測
                double AccumulatedDeltaRange = 0.0;
                if(SettingsFragment.CarrierPhase == true) {
                    AccumulatedDeltaRange = measurement.getCarrierCycles() + measurement.getCarrierPhase();
                }

                if (firstOBS == true) {
                    String OBSTime = String.format(" %2d %2d %2d %2d %2d%11.7f  0", value.Y - 2000, value.M, value.D, value.h, value.m, value.s);
                    SensorStream =
                            String.format("%6d,%6d,%6d,%6d,%6d,%13.7f", value.Y, value.M, value.D, value.h, value.m, value.s);
                    Time.append(OBSTime);
                    firstOBS = false;
                }
                //GPSのPRN番号と時刻用String
                String prn = String.format("G%2d", measurement.getSvid());
                satnumber = satnumber + 1;
                Prn.append(prn);
                String PrmStrings = String.format("%14.3f%s%s", prm, " ", " ");
                String DeltaRangeStrings = String.format("%14.3f%s%s", 0.0, " ", " ");
                if(SettingsFragment.CarrierPhase == true) {
                    DeltaRangeStrings = String.format("%14.3f%s%s", AccumulatedDeltaRange, " ", " ");
                }
                //Fix用チェック
                if (ReadUseInFixArray(measurement.getSvid())) {
                    String DbHz = String.format("%14.3f%s%s", measurement.getCn0DbHz(), " ", " ");
                    Measurements.append(PrmStrings + DbHz + "\n");
                }
                //Google側でFixとして使われていない場合は信号強度を0に
                else {
                    String DbHz = String.format("%14.3f%s%s", 0.0, " ", " ");
                    if(SettingsFragment.CarrierPhase){
                        Measurements.append(DeltaRangeStrings + PrmStrings + DbHz + "\n");
                    }else {
                        Measurements.append(PrmStrings + DbHz + "\n");
                    }
                }
            }
        }
        Prn.insert(0,String.format("%3d",satnumber));
        mFileWriter.write(Time.toString() + Prn.toString() + "\n");
        mFileWriter.write(Measurements.toString());
        mFileAccAzWriter.write(SensorStream);
        mFileAccAzWriter.newLine();
    }

    private void logException(String errorMessage, Exception e) {
        Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    private void logError(String errorMessage) {
        Log.e(GnssContainer.TAG + TAG, errorMessage);
        Toast.makeText(mContext, errorMessage, Toast.LENGTH_LONG).show();
    }

    /**
     * Implements a {@link FileFilter} to delete files that are not in the
     * {@link FileToDeleteFilter#mRetainedFiles}.
     */
    private static class FileToDeleteFilter implements FileFilter {
        private final List<File> mRetainedFiles;

        public FileToDeleteFilter(File... retainedFiles) {
            this.mRetainedFiles = Arrays.asList(retainedFiles);
        }

        /**
         * Returns {@code true} to delete the file, and {@code false} to keep the file.
         *
         * <p>Files are deleted if they are not in the {@link FileToDeleteFilter#mRetainedFiles} list.
         */
        @Override
        public boolean accept(File pathname) {
            if (pathname == null || !pathname.exists()) {
                return false;
            }
            if (mRetainedFiles.contains(pathname)) {
                return false;
            }
            return pathname.length() < MINIMUM_USABLE_FILE_SIZE_BYTES;
        }
    }
    //GPS週秒からGPS時への変換
    public static class ReturnValue {
        public int Y;
        public int M;
        public int D;
        public int h;
        public int m;
        public double s;
    }

    public static class GPSWStoGPST {
        public ReturnValue method(double GPSW , double GPSWS) {
            ReturnValue value = new ReturnValue();
            //MJDおよびMDの計算
            double MD = (int)(GPSWS/86400);
            double MJD = 44244+GPSW*7+MD;
            //ユリウス日から年月日
            double JD = MJD + 2400000.5;
            double N = JD + 0.5;
            int Z = (int)N;
            double F = N - Z;
            double A;
            if(Z >= 2299161){
                int X = (int)((Z-1867216.25)/36524.25);
                A = Z + 1 + X - X/4;
            }
            else {
                A = Z;
            }
            double B = A + 1524;
            int C = (int)((B-122.1)/365.25);
            int K = (int)(365.25*C);
            int E = (int)((B-K)/30.6001);
            double D = B-K-(int)(30.6001*E)+F;
            int M;
            int Y;
            if(E < 13.5){
                M = E - 1;
            }
            else {
                M = E - 13;
            }
            if(M > 2.5){
                Y = C - 4716;
            }
            else{
                Y = C - 4715;
            }
            value.Y = Y;
            value.M = M;
            value.D = (int)D;

            //GPS週秒からGPS時刻へ
            double DS = GPSWS-MD*86400;
            int h = (int)(DS/3600);
            double hm = DS-h*3600;
            int m = (int)(hm/60);
            double s = hm - m * 60;

            value.h = h;
            value.m = m;
            value.s = s;

            return value;
            }
    }

}
