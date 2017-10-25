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

import android.app.Fragment;
import android.location.GnssClock;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.apps.location.gps.gnsslogger.GnssContainer;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import android.widget.Button;

/**
 * The UI fragment showing a set of configurable settings for the client to request GPS data.
 */
public class SettingsFragment extends Fragment {

    public static final String TAG = ":SettingsFragment";
    public static boolean CarrierPhase = false;
    public static boolean useQZ = false;
    public static boolean useGL = false;
    public static boolean GNSSClockSync = false;
    public static boolean useDeviceSensor = false;
    public static boolean ResearchMode = false;
    private GnssContainer mGpsContainer;
    private SensorContainer mSensorContainer;
    private HelpDialog helpDialog;


    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }
    public void setSensorContainer(SensorContainer value){ mSensorContainer = value; }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false /* attachToRoot */);

        final Switch registerLocation = (Switch) view.findViewById(R.id.register_location);
        final TextView registerLocationLabel =
                (TextView) view.findViewById(R.id.register_location_label);
        //set the switch to OFF
        registerLocation.setChecked(false);
        registerLocationLabel.setText("GNSS Register OFF");
        registerLocation.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mGpsContainer.registerLocation();
                            mGpsContainer.registerMeasurements();
                            mGpsContainer.registerNavigation();
                            mGpsContainer.registerGnssStatus();
                            //mGpsContainer.registerNmea();
                            registerLocationLabel.setText("GNSS Register ON");
                        } else {
                            mGpsContainer.unregisterLocation();
                            mGpsContainer.unregisterMeasurements();
                            mGpsContainer.unregisterNavigation();
                            mGpsContainer.unregisterGpsStatus();
                            //mGpsContainer.unregisterNmea();
                            registerLocationLabel.setText("GNSS Register OFF");
                        }
                    }
                });

        final CheckBox CarrierPhaseChkBox = (CheckBox) view.findViewById(R.id.checkBox);
        final CheckBox useQZSS = (CheckBox) view.findViewById(R.id.useQZS);
        final CheckBox useGLO = (CheckBox) view.findViewById(R.id.useGLO);
        CarrierPhaseChkBox.setChecked(false);
        CarrierPhaseChkBox.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                CarrierPhase = CarrierPhaseChkBox.isChecked();
            }

        });

        useQZSS.setChecked(false);
        useQZSS.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                useQZ = useQZSS.isChecked();
            }

        });

        useGLO.setChecked(false);
        useGLO.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                useGL = useGLO.isChecked();
            }

        });

        final Switch ResearchModeSwitch = (Switch) view.findViewById(R.id.ResearchMode);

        ResearchModeSwitch.setChecked(false);
        ResearchModeSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    ResearchMode = true;
                } else {
                    ResearchMode = false;
                }
            }

        });

        final Switch registerSensor = (Switch) view.findViewById(R.id.register_sensor);
        registerSensor.setChecked(false);
        registerSensor.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            mSensorContainer.registerSensor();
                            useDeviceSensor = true;
                        } else {
                            mSensorContainer.unregisterSensor();
                            useDeviceSensor = false;
                        }
                    }
                });

        Button resetGNSSClock = (Button) view.findViewById(R.id.rstbutton);

        resetGNSSClock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view){
                GnssClock Clock = null;
                try {
                    Clock = GnssClock.class.newInstance();
                } catch (java.lang.InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
                Method method_clock = null;
                try {
                    method_clock = Clock.getClass().getMethod("reset");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                if(Clock != null) {
                    try {
                        Object reset = method_clock.invoke(Clock);
                        Log.d("Invoke Complete", "GNSS Clock Reset Complete");
                    } catch (IllegalArgumentException e) {
                        //呼び出し：引数が異なる
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        //呼び出し：アクセス違反、保護されている
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        //ターゲットとなるメソッド自身の例外処理
                        e.printStackTrace();
                    }

                }
                else {
                    Log.e("Invoke Error", "GNSSClock Instance is null");
                }
            }
        });

        Button exit = (Button) view.findViewById(R.id.exit);
        exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getActivity().finishAffinity();
            }
        });

        TextView swInfo = (TextView) view.findViewById(R.id.sw_info);

        java.lang.reflect.Method method;
        LocationManager locationManager = mGpsContainer.getLocationManager();
        try {
            method = locationManager.getClass().getMethod("getGnssYearOfHardware");
            int hwYear = (int) method.invoke(locationManager);
            if (hwYear == 0) {
                swInfo.append("HW Year: " + "2015 or older \n");
            } else {
                swInfo.append("HW Year: " + hwYear + "\n");
            }

        } catch (NoSuchMethodException e) {
            logException("No such method exception: ", e);
            return null;
        } catch (IllegalAccessException e) {
            logException("Illegal Access exception: ", e);
            return null;
        } catch (InvocationTargetException e) {
            logException("Invocation Target Exception: ", e);
            return null;
        }

        String platfromVersionString = Build.VERSION.RELEASE;
        swInfo.append("Platform: " + platfromVersionString + "\n");
        int apiLivelInt = Build.VERSION.SDK_INT;
        swInfo.append("Api Level: " + apiLivelInt);

        return view;
    }

    private void logException(String errorMessage, Exception e) {
        //Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }
}
