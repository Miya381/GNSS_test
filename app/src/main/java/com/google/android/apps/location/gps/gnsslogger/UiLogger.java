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
import android.os.Bundle;
import android.renderscript.Sampler;
import android.util.Log;
import com.google.android.apps.location.gps.gnsslogger.LoggerFragment.UIFragmentComponent;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

/**
 * A class representing a UI logger for the application. Its responsibility is show information in
 * the UI.
 */
public class UiLogger implements GnssListener {

    private static final long EARTH_RADIUS_METERS = 6371000;
    private static final int USED_COLOR = Color.rgb(0x4a, 0x5f, 0x70);
    private double trueAzimuth;
    private double Declination;

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
        double TrueAzimuth = azimuth + Declination;
        if(TrueAzimuth >= 360){
            TrueAzimuth = 360 - TrueAzimuth;
        }
        logText("Sensor", listener + "\n Declination : " + Declination + "\n TrueAzimuth : " + Math.abs(TrueAzimuth), USED_COLOR);
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
                component.SensorlogTextFragment(text,color);
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
                component.SensorlogTextFragment(text,color);
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
        if(gnssClock.getHardwareClockDiscontinuityCount() == 0){
            ClockStr = "WARING!! HARDWARE Clock may broken";
        }else{
            double tRxSeconds;
            double TimeNanos = gnssClock.getTimeNanos();
            double FullBiasNanos = gnssClock.getFullBiasNanos();
            double BiasNanos = gnssClock.getBiasNanos();
            double weekNumber = Math.floor((double) (gnssClock.getTimeNanos()) * 1e-9 / 604800);
            if (gnssClock.hasBiasNanos() == false) {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos()) * 1e-9;
            } else {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - gnssClock.getBiasNanos()) * 1e-9;
            }
            FileLogger.GPSWStoGPST gpswStoGPST = new FileLogger.GPSWStoGPST();
            FileLogger.ReturnValue value = gpswStoGPST.method(weekNumber,tRxSeconds);
            ClockStr = String.format("GPST = %d / %d / %d / %d : %d : %f \n TimeNanos: %f, FullBiasNanos: %f, BiasNanos: %f", value.Y,value.M,value.D,value.h,value.m,value.s,TimeNanos,FullBiasNanos,BiasNanos);
        }
        return ClockStr;
    }

    private String[][] gnssMessageToString(GnssMeasurementsEvent event, GnssClock gnssClock){
        String[][] array = new String[12][4];
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
        int arrayRow = 0;
        for (GnssMeasurement measurement : event.getMeasurements()) {
        if(measurement.getConstellationType() == GnssStatus.CONSTELLATION_GPS) {
            double tRxSeconds;
            //double weekNumber = Math.floor((double) (gnssClock.getTimeNanos()) * 1e-9 / 604800);
            if (gnssClock.hasBiasNanos() == false) {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos());
            } else {
                tRxSeconds = (gnssClock.getTimeNanos() - gnssClock.getFullBiasNanos() - gnssClock.getBiasNanos());
            }
            double tTxSeconds = measurement.getReceivedSvTimeNanos();
            /*急場の変更！！*/
            String tRxStr = String.valueOf(-gnssClock.getFullBiasNanos());
            String tTxStr = String.valueOf(measurement.getReceivedSvTimeNanos());
            tTxSeconds = Float.parseFloat(tTxStr.substring(tTxStr.length() - 10));
            tRxSeconds = Float.parseFloat(tRxStr.substring(tRxStr.length() - 10));
            /*急場の変更！！*/
            //GPS週のロールオーバーチェック
            Log.d("tRxSeconds",tRxStr);
            Log.d("tTxSeconds",tTxStr);
            //Log.d("Prm",String.valueOf(-gnssClock.getFullBiasNanos() - measurement.getReceivedSvTimeNanos()));
            double prSeconds = (tRxSeconds - tTxSeconds)*1e-9;
            double prm = prSeconds * 2.99792458e8;
            array[arrayRow][0] = "G" + String.valueOf(measurement.getSvid());
            array[arrayRow][1] = String.format("%14.3f",prm);
            /*builder.append("GNSSClock = ").append(event.getClock().getTimeNanos()).append("\n");
            builder.append("Svid = ").append(measurement.getSvid()).append(", ");
            builder.append("Cn0DbHz = ").append(measurement.getCn0DbHz()).append(", ");
            builder.append("PseudoRange = ").append(prm).append("\n");
            builder.append("tRxSeconds = ").append(tRxSeconds).append("\n");
            builder.append("tTxSeconds = ").append(tTxSeconds).append("\n");*/
            //builder.append("FullCarrierCycles = ").append(measurement.getCarrierCycles() + measurement.getCarrierPhase()).append("\n");
            if(SettingsFragment.CarrierPhase == true) {
                if(measurement.getAccumulatedDeltaRangeState() == GnssMeasurement.ADR_STATE_CYCLE_SLIP){
                    array[arrayRow][2] = "-1";
                }else {
                    array[arrayRow][2] = String.format("%14.3f", measurement.getCarrierCycles() + measurement.getCarrierPhase());
                }
            }else{
                array[arrayRow][2] = "0";
            }
            array[arrayRow][3] = String.valueOf(measurement.getCn0DbHz());
            arrayRow++;
        }
    }
        return array;
}

    private void logLocationEvent(String event) {
        logEvent("Location", event, USED_COLOR);
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
