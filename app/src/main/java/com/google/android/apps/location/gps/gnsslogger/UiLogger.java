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

import android.graphics.Color;
import android.location.GnssClock;
import android.location.GnssMeasurement;
import android.location.GnssMeasurementsEvent;
import android.location.GnssNavigationMessage;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Build;
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.location.gps.gnsslogger.LoggerFragment.UIFragmentComponent;
import com.google.android.apps.location.gps.gnsslogger.Logger2Fragment.UIFragment2Component;
import com.google.android.apps.location.gps.gnsslogger.Logger3Fragment.UIFragment3Component;
import com.google.android.apps.location.gps.gnsslogger.SettingsFragment.UIFragmentSettingComponent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.Double.NaN;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class UiLogger implements GnssListener {

    private static final long EARTH_RADIUS_METERS = 6371000;
    private static final double GPS_L1_FREQ = 154.0 * 10.23e6;
    private static final double SPEED_OF_LIGHT = 299792458.0; //[m/s]
    private static final double GPS_L1_WAVELENGTH = SPEED_OF_LIGHT/GPS_L1_FREQ;
    private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);
    private double trueAzimuth;
    private double Declination;

    private boolean gnssStatusReady = false;

    String array[][] = new String[12][4];


    public UiLogger() {
    }

    private UIFragmentComponent mUiFragmentComponent;

    public synchronized UIFragmentComponent getUiFragmentComponent() {
        return mUiFragmentComponent;
    }

    public synchronized void setUiFragmentComponent(UIFragmentComponent value) {
        mUiFragmentComponent = value;
    }

    private UIFragment2Component mUiFragment2Component;

    public synchronized UIFragment2Component getUiFragment2Component() {
        return mUiFragment2Component;
    }

    public synchronized void setUiFragment2Component(UIFragment2Component value) {
        mUiFragment2Component = value;
    }

    private UIFragment3Component mUiFragment3Component;

    public synchronized UIFragment3Component getUiFragment3Component() {
        return mUiFragment3Component;
    }

    public synchronized void setUiFragment3Component(UIFragment3Component value) {
        mUiFragment3Component = value;
    }

    private UIFragmentSettingComponent mUISettingComponent;

    public synchronized UIFragmentSettingComponent getUISettingComponent() {
        return mUISettingComponent;
    }

    public synchronized void setUISettingComponent(UIFragmentSettingComponent value) {
        mUISettingComponent = value;
    }


    @Override
    public void onProviderEnabled(String provider) {
        logLocationEvent("onProviderEnabled: " + provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        logLocationEvent("onProviderDisabled: " + provider);
    }

    @Override
    public void onLocationChanged(Location location) {
        trueAzimuth = location.getBearing();
        //磁気偏角を計算
        double Longitude = location.getLongitude();
        double Latitude = location.getLatitude();
        double deltaPhi = Latitude - 37;
        double deltaLamda = Longitude - 138;
        Declination = 757.201 + 18.750*deltaPhi - 6.761*deltaLamda - 0.059*Math.pow(deltaPhi,2) - 0.014 * deltaPhi * deltaLamda - 0.579 * Math.pow(deltaLamda,2);
        Declination = Declination / 100;
        BigDecimal x = new BigDecimal(Declination);
        x = x.setScale(1,BigDecimal.ROUND_HALF_UP);
        Declination = x.doubleValue();

        location.getTime();
        UIFragmentComponent component = getUiFragmentComponent();
        component.LocationTextFragment(String.format("LON:%f\nLAT:%f\nALT:%f",location.getLongitude(),location.getLatitude(),location.getAltitude()),0);

        //logLocationEvent("onLocationChanged: " + location);
    }

    @Override
    public void onLocationStatusChanged(String provider, int status, Bundle extras) {
        /*String message =
                String.format(
                        "onStatusChanged: provider=%s, status=%s, extras=%s",
                        provider, locationStatusToString(status), extras);
        logLocationEvent(message);*/
    }

    @Override
    public void onGnssMeasurementsReceived(GnssMeasurementsEvent event) {
        UIFragmentComponent component = getUiFragmentComponent();
        array = gnssMessageToString(event,event.getClock());
        component.logTextFragment("", "", array);
        String GNSSStr = gnssClockToString(event.getClock());
        component.GNSSClockLog(GNSSStr);
        //logMeasurementEvent("onGnsssMeasurementsReceived: " + measurements);
    }

    @Override
    public void onGnssMeasurementsStatusChanged(int status) {
        if(gnssMeasurementsStatusToString(status) != "READY"){
            gnssStatusReady = false;
        }else {
            gnssStatusReady = true;
        }
        //UIFragmentSettingComponent component = getUISettingComponent();
        //component.SettingErrorFragment(status);
        SettingsFragment.GNSSMeasurementReadyMode = status;
        //logMeasurementEvent("onStatusChanged: " + gnssMeasurementsStatusToString(status));
    }

    @Override
    public void onGnssNavigationMessageReceived(GnssNavigationMessage event) {
        //logNavigationMessageEvent("onGnssNavigationMessageReceived: " + event);
    }

    @Override
    public void onGnssNavigationMessageStatusChanged(int status) {
        //logNavigationMessageEvent("onStatusChanged: " + getGnssNavigationMessageStatus(status));
    }

    @Override
    public void onGnssStatusChanged(GnssStatus gnssStatus) {
        UIFragment2Component component2 = getUiFragment2Component();
        if(gnssStatusReady == false){
            return;
        }
        String[] SVID = new String[30];
        float[][] pos = new float[30][2];
        int maxSat = gnssStatus.getSatelliteCount();
        for(int i = 0;i < maxSat;i++){
            if(gnssStatus.getConstellationType(i + 1) == GnssStatus.CONSTELLATION_GPS) {
                SVID[i] ="G" + String.valueOf(gnssStatus.getSvid(i + 1));
                pos[i][0] = gnssStatus.getAzimuthDegrees(i + 1);
                pos[i][1] = gnssStatus.getElevationDegrees(i + 1);
            }else if(gnssStatus.getConstellationType(i + 1) == GnssStatus.CONSTELLATION_QZSS && SettingsFragment.useQZ){
                SVID[i] ="Q" + String.valueOf(gnssStatus.getSvid(i + 1));
                pos[i][0] = gnssStatus.getAzimuthDegrees(i + 1);
                pos[i][1] = gnssStatus.getElevationDegrees(i + 1);
            }else if(gnssStatus.getConstellationType(i + 1) == GnssStatus.CONSTELLATION_GLONASS && SettingsFragment.useGL){
                SVID[i] ="R" + String.valueOf(gnssStatus.getSvid(i + 1));
                pos[i][0] = gnssStatus.getAzimuthDegrees(i + 1);
                pos[i][1] = gnssStatus.getElevationDegrees(i + 1);
            }
        }
        int satNumber = maxSat;
        component2.log2TextFragment(SVID,pos,satNumber);
        //logStatusEvent("onGnssStatusChanged: " + gnssStatusToString(gnssStatus));
    }

    @Override
    public void onNmeaReceived(long timestamp, String s) {
        /*logNmeaEvent(String.format("onNmeaReceived: timestamp=%d, %s", timestamp, s));*/
    }

    @Override
    public void onListenerRegistration(String listener, boolean result) {
        logEvent("Registration", String.format("add%sListener: %b", listener, result), USED_COLOR);
    }

    public void onSensorListener(String listener,double azimuth,float accZ , float altitude){
        UIFragment2Component component2 = getUiFragment2Component();
        UIFragment3Component component3 = getUiFragment3Component();

        double TrueAzimuth = azimuth + Declination;
        if(TrueAzimuth >= 360){
            TrueAzimuth = 360 - TrueAzimuth;
        }
        component2.log2SensorFragment(azimuth);
        if(component3 != null) {
            component3.log3TextFragment(listener);
        }
        if(SettingsFragment.ResearchMode) {
            logText("Sensor", listener + "\n Declination : " + Declination + "\n TrueAzimuth : " + Math.abs(TrueAzimuth), USED_COLOR);
            //Log.d("Device Sensor",listener);
        }else {
            logText("Sensor", listener, USED_COLOR);
            //Log.d("Device Sensor",listener);
        }
    }

    private void logMeasurementEvent(String event) {
        logEvent("Measurement", event, USED_COLOR);
    }

    private void logNavigationMessageEvent(String event) {
        logEvent("NavigationMsg", event, USED_COLOR);
    }

    private void logStatusEvent(String event) {
        logEvent("Status", event, USED_COLOR);
    }

    private void logNmeaEvent(String event) {
        logEvent("Nmea", event, USED_COLOR);
    }

    private void logEvent(String tag, String message, int color) {
        String composedTag = GnssContainer.TAG + tag;
        Log.d(composedTag, message);
        logText(tag, message, color);
    }

    private void logText(String tag, String text, int color) {
        UIFragmentComponent component = getUiFragmentComponent();
        if (component != null) {
            if(tag == "Sensor"){
                //component.SensorlogTextFragment(text,color);
            }
            else{
                //component.logTextFragment(tag, text, color);
            }
        }
    }

    private void SublogText(String tag, String text, int color) {
        UIFragmentComponent component = getUiFragmentComponent();
        if (component != null) {
            if(tag == "Sensor"){
                //component.SensorlogTextFragment(text,color);
            }
            else{
                //component.logTextFragment(tag, text, color);
            }
        }
    }

    private String locationStatusToString(int status) {
        switch (status) {
            case LocationProvider.AVAILABLE:
                return "AVAILABLE";
            case LocationProvider.OUT_OF_SERVICE:
                return "OUT_OF_SERVICE";
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                return "TEMPORARILY_UNAVAILABLE";
            default:
                return "<Unknown>";
        }
    }

    private String gnssMeasurementsStatusToString(int status) {
        switch (status) {
            case GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED:
                return "NOT_SUPPORTED";
            case GnssMeasurementsEvent.Callback.STATUS_READY:
                return "READY";
            case GnssMeasurementsEvent.Callback.STATUS_LOCATION_DISABLED:
                return "GNSS_LOCATION_DISABLED";
            default:
                return "<Unknown>";
        }
    }

    private String getGnssNavigationMessageStatus(int status) {
        switch (status) {
            case GnssNavigationMessage.STATUS_UNKNOWN:
                return "Status Unknown";
            case GnssNavigationMessage.STATUS_PARITY_PASSED:
                return "READY";
            case GnssNavigationMessage.STATUS_PARITY_REBUILT:
                return "Status Parity Rebuilt";
            default:
                return "<Unknown>";
        }
    }

    private String gnssStatusToString(GnssStatus gnssStatus) {

        StringBuilder builder = new StringBuilder("SATELLITE_STATUS | [Satellites:\n");
        for(int i = 0; i < gnssStatus.getSatelliteCount(); i++) {
            builder
                    .append("Type = ")
                    .append(getConstellationName(gnssStatus.getConstellationType(i)))
                    .append(", ");
            builder.append("Svid = ").append(gnssStatus.getSvid(i)).append(", ");
            //builder.append("Elevation = ").append(gnssStatus.getElevationDegrees(i)).append(", ");
            //builder.append("Azimuth = ").append(gnssStatus.getAzimuthDegrees(i)).append(", ");
            //builder.append("hasEphemeris = ").append(gnssStatus.hasEphemerisData(i)).append(", ");
            builder.append("usedInFix = ").append(gnssStatus.usedInFix(i)).append("\n");
        }
        builder.append("]");
        return builder.toString();
    }

    private String gnssClockToString(GnssClock gnssClock){
        String ClockStr = "";
        if(gnssStatusReady == false){
            return "GNSS Measurements NOT READY or SUPPORTED";
        }
        if(gnssClock.getHardwareClockDiscontinuityCount() == -1){
            ClockStr = "WARING!! HARDWARE Clock may broken";
        }else{
            double tRxSeconds;
            double TimeNanos = gnssClock.getTimeNanos();
            double FullBiasNanos = gnssClock.getFullBiasNanos();
            double BiasNanos = gnssClock.getBiasNanos();
            double weekNumber = Math.floor(- (gnssClock.getFullBiasNanos() * 1e-9 / 604800));
            double weekNumberNanos = weekNumber * 604800 * 1e9;
            if (gnssClock.hasBiasNanos() == false) {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos) * 1e-9;
            } else {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - gnssClock.getBiasNanos() - weekNumberNanos) * 1e-9;
            }
            String DeviceName = Build.DEVICE;
            FileLogger.GPSWStoGPST gpswStoGPST = new FileLogger.GPSWStoGPST();
            FileLogger.ReturnValue value = gpswStoGPST.method(weekNumber,tRxSeconds);
            ClockStr = String.format("DEVICE NAME: %s\nGPST = %d / %d / %d / %d : %d : %f \n", Build.DEVICE,value.Y,value.M,value.D,value.h,value.m,value.s);
            UIFragmentSettingComponent component = getUISettingComponent();
            component.SettingTextFragment(String.format("%d_%d_%d_%d_%d",value.Y,value.M,value.D,value.h,value.m));
            final Calendar calendar = Calendar.getInstance();
            if(String.valueOf(value.Y).indexOf(String.valueOf(calendar.YEAR)) != -1){
                SettingsFragment.GNSSClockSync = true;
            }else{
                SettingsFragment.GNSSClockSync = false;
            }
            Log.d("GNSSClock",gnssClock.toString());
        }
        return ClockStr;
    }

    private String[][] gnssMessageToString(GnssMeasurementsEvent event, GnssClock gnssClock){
        String[][] array = new String[20][4];
        //builder.append("GNSSClock = ").append(event.getClock().toString()).append("\n");
        //double GPSWeek = Math.floor((double) (gnssClock.getTimeNanos()) * 1e-9 / 604800);
        //long GPSWeekNanos = (long) GPSWeek * (long) (604800 * 1e9);
        //double tRxNanos = 0;
        //if (gnssClock.hasBiasNanos() == false) {
            //tRxNanos = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos());
        //} else {
            //tRxNanos = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - gnssClock.getBiasNanos());
        //}
        //double tRxSeconds = tRxNanos * 1e-9;
        //雷神は値がひっくり返っているため補正する必要あり
        //builder.append("GPSWeek = ").append(GPSWeek).append("\n");
        //builder.append("GPSWeekSec = ").append(tRxSeconds).append("\n");
        //builder.append("GPSWeek = ").append(Math.floor(gnssClock.getFullBiasNanos())).append("\n");
        //builder.append("GPSWeekNanos = ").append(tRxNanos * 1e-9).append("\n");
        //builder.append("FullBiasSeconds = ").append((double)(gnssClock.getFullBiasNanos() * 1e-9)).append("\n");
        //builder.append("TimeSeconds = ").append((double)(gnssClock.getTimeNanos()* 1e-9)).append("\n");
        //builder.append("BiasSeconds = ").appen
        // kfleavfesthoszdeoxdgilojfgytd((double)(gnssClock.getBiasNanos()* 1e-9)).append("\n");
        if(gnssStatusReady == false){
            return array;
        }
        int arrayRow = 0;
        for (GnssMeasurement measurement : event.getMeasurements()) {
        if((measurement.getConstellationType() == GnssStatus.CONSTELLATION_QZSS && SettingsFragment.useQZ) || (measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) || ((measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS) && (SettingsFragment.useGL == true))) {
            double weekNumber = Math.floor(- (gnssClock.getFullBiasNanos() * 1e-9 / 604800));
            //Log.d("WeekNumber",String.valueOf(weekNumber));
            double weekNumberNanos = weekNumber * 604800 * 1e9;
            //Log.d("WeekNumberNanos",String.valueOf(weekNumberNanos));
            //double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;
            double tRxNanos = gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - weekNumberNanos;
            if (gnssClock.hasBiasNanos()) {
                tRxNanos = tRxNanos - gnssClock.getBiasNanos();
            }
            if (measurement.getTimeOffsetNanos() != 0){
                tRxNanos = tRxNanos - measurement.getTimeOffsetNanos();
            }
            double tRxSeconds = tRxNanos*1e-9;
            double tTxSeconds = measurement.getReceivedSvTimeNanos()*1e-9;
            Log.d("tRxSeconds",String.valueOf(tRxSeconds));
            Log.d("tTxSeconds",String.valueOf(tTxSeconds));
            /*急場の変更！！*/
            String DeviceName = Build.DEVICE;
            //Log.d("DEVICE",DeviceName);
            /*急場の変更！！*/
            //GPS週のロールオーバーチェック
            double prSeconds = tRxSeconds - tTxSeconds;
            boolean iRollover = prSeconds > 604800 / 2;
            if (iRollover) {
                double delS = Math.round(prSeconds / 604800) * 604800;
                double prS = prSeconds - delS;
                double maxBiasSeconds = 10;
                if (prS > maxBiasSeconds) {
                    Log.e("RollOver", "Rollover Error");
                    iRollover = true;
                } else {
                    tRxSeconds = tRxSeconds - delS;
                    prSeconds = tRxSeconds - tTxSeconds;
                    iRollover = false;
                }
            }
            //Log.d("tRxSeconds",tRxStr);
            //Log.d("tTxSeconds",tTxStr);
            //Log.d("Prm",String.valueOf(-gnssClock.getFullBiasNanos() - measurement.getReceivedSvTimeNanos()));
            //Log.d("tRxSeconds",String.valueOf(tRxSeconds));
            //Log.d("tTxSeconds",String.valueOf(tTxSeconds));
            double prm = prSeconds * 2.99792458e8;
            if(measurement.getConstellationType() == GnssStatus.CONSTELLATION_QZSS){
                //Log.d("QZSS","QZSS Detected");
                array[arrayRow][0] = "Q" + String.valueOf(measurement.getSvid());
            }else if(measurement.getConstellationType() == GnssStatus.CONSTELLATION_GLONASS){
                array[arrayRow][0] = "R" + String.valueOf(measurement.getSvid());
            }else {
                array[arrayRow][0] = "G" + String.valueOf(measurement.getSvid());
            }
            //Log.d("STATE",String.valueOf(measurement.getState());
            if(iRollover){
                array[arrayRow][1] = "ROLLOVER_ERROR";
            }else if(prSeconds < 0 || prSeconds > 1){
                array[arrayRow][1] = "INVALID_VALUE";
            }
            else if(getStateName(measurement.getState()) == "1") {
                array[arrayRow][1] = String.format("%14.3f", prm);
            }else {
                array[arrayRow][1] = getStateName(measurement.getState());
            }
            /*builder.append("GNSSClock = ").append(event.getClock().getTimeNanos()).append("\n");
            builder.append("Svid = ").append(measurement.getSvid()).append(", ");
            builder.append("Cn0DbHz = ").append(measurement.getCn0DbHz()).append(", ");
            builder.append("PseudoRange = ").append(prm).append("\n");
            builder.append("tRxSeconds = ").append(tRxSeconds).append("\n");
            builder.append("tTxSeconds = ").append(tTxSeconds).append("\n");*/
            //builder.append("FullCarrierCycles = ").append(measurement.getCarrierCycles() + measurement.getCarrierPhase()).append("\n");
            if(SettingsFragment.CarrierPhase == true) {
                if(measurement.getAccumulatedDeltaRangeState() == GnssMeasurement.ADR_STATE_CYCLE_SLIP){
                    array[arrayRow][2] = "ADR_STATE_CYCLE_SLIP";
                }else if(measurement.getAccumulatedDeltaRangeState() == GnssMeasurement.ADR_STATE_RESET) {
                    array[arrayRow][2] = "ADR_STATE_RESET";
                }else if(measurement.getAccumulatedDeltaRangeState() == GnssMeasurement.ADR_STATE_UNKNOWN) {
                    array[arrayRow][2] = "ADR_STATE_UNKNOWN";
                }else{
                    if(measurement.hasCarrierPhase() && measurement.hasCarrierCycles()) {
                        array[arrayRow][2] = String.format("%14.3f", measurement.getCarrierCycles() + measurement.getCarrierPhase());
                    }else {
                        array[arrayRow][2] = String.format("%14.3f", measurement.getAccumulatedDeltaRangeMeters() / GPS_L1_WAVELENGTH);
                    }
                }
            }else{
                array[arrayRow][2] = "0";
            }
            array[arrayRow][3] = String.format("%2.1f",measurement.getCn0DbHz());
            arrayRow++;
        }
    }
        return array;
}

    private void logLocationEvent(String event) {
        logEvent("Location", event, USED_COLOR);
    }

    private String getStateName(int id){
        switch (id){
            case GnssMeasurement.STATE_BIT_SYNC:
                return "STATE_BIT_SYNC";
            case GnssMeasurement.STATE_SUBFRAME_SYNC:
                return "STATE_SUBFRAME_SYNC";
            case GnssMeasurement.STATE_SYMBOL_SYNC:
                return "STATE_SYMBOL_SYNC";
            case GnssMeasurement.STATE_MSEC_AMBIGUOUS:
                return "STATE_MSEC_AMBIGUOUS";
            case GnssMeasurement.STATE_CODE_LOCK:
                return "STATE_CODE_LOCK";
            case GnssMeasurement.STATE_UNKNOWN:
                return "STATE_UNKNOWN";
            case GnssMeasurement.STATE_TOW_DECODED:
                return "STATE_TOW_DECODED";
            case GnssMeasurement.STATE_BDS_D2_BIT_SYNC:
                return "STATE_BDS_D2_BIT_SYNC";
            case GnssMeasurement.STATE_GAL_E1B_PAGE_SYNC:
                return "STATE_GAL_E1B_PAGE_SYNC";
            case GnssMeasurement.STATE_BDS_D2_SUBFRAME_SYNC:
                return "STATE_BDS_D2_SUBFRAME_SYNC";
            case GnssMeasurement.STATE_GAL_E1BC_CODE_LOCK:
                return "STATE_GAL_E1BC_CODE_LOCK";
            case GnssMeasurement.STATE_GAL_E1C_2ND_CODE_LOCK:
                return "STATE_GAL_E1C_2ND_CODE_LOCK";
            case GnssMeasurement.STATE_GLO_STRING_SYNC:
                return "STATE_GLO_STRING_SYNC";
            case GnssMeasurement.STATE_GLO_TOD_DECODED:
                return "STATE_GLO_TOD_DECODED";
            case GnssMeasurement.STATE_SBAS_SYNC:
                return "STATE_SBAS_SYNC";
            default:
                return "1";
        }
    }

    private String getConstellationName(int id) {
        switch (id) {
            case 1:
                return "GPS";
            case 2:
                return "SBAS";
            case 3:
                return "GLONASS";
            case 4:
                return "QZSS";
            case 5:
                return "BEIDOU";
            case 6:
                return "GALILEO";
            default:
                return "UNKNOWN";
        }
    }
}
