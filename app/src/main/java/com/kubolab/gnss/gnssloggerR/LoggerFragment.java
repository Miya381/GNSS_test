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

package com.kubolab.gnss.gnssloggerR;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**  The UI fragment that hosts a logging view. */
public class LoggerFragment extends Fragment {

    private TextView mLogView;
    private TextView mGNSSClockView;
    private TextView mSensorLogView;
    private ScrollView mScrollView;
    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private ViewGroup mTable;
    private Button startLog;
    private Button sendFile;
    private boolean FileLogging;

    //表用ID
    int Rid[][]={{R.id.textView1_1,R.id.textView1_2,R.id.textView1_3,R.id.textView1_4},{R.id.textView2_1,R.id.textView2_2,R.id.textView2_3,R.id.textView2_4},
            {R.id.textView3_1,R.id.textView3_2,R.id.textView3_3,R.id.textView3_4},{R.id.textView4_1,R.id.textView4_2,R.id.textView4_3,R.id.textView4_4},
            {R.id.textView5_1,R.id.textView5_2,R.id.textView5_3,R.id.textView5_4},{R.id.textView6_1,R.id.textView6_2,R.id.textView6_3,R.id.textView6_4},
            {R.id.textView7_1,R.id.textView7_2,R.id.textView7_3,R.id.textView7_4},{R.id.textView8_1,R.id.textView8_2,R.id.textView8_3,R.id.textView8_4},
            {R.id.textView9_1,R.id.textView9_2,R.id.textView9_3,R.id.textView9_4},{R.id.textView10_1,R.id.textView10_2,R.id.textView10_3,R.id.textView10_4},
            {R.id.textView11_1,R.id.textView11_2,R.id.textView11_3,R.id.textView11_4},{R.id.textView12_1,R.id.textView12_2,R.id.textView12_3,R.id.textView12_4},
            {R.id.textView13_1,R.id.textView13_2,R.id.textView13_3,R.id.textView13_4},{R.id.textView14_1,R.id.textView14_2,R.id.textView14_3,R.id.textView14_4},
            {R.id.textView15_1,R.id.textView15_2,R.id.textView15_3,R.id.textView15_4},{R.id.textView16_1,R.id.textView16_2,R.id.textView16_3,R.id.textView16_4},
            {R.id.textView17_1,R.id.textView17_2,R.id.textView17_3,R.id.textView17_4},{R.id.textView18_1,R.id.textView18_2,R.id.textView18_3,R.id.textView18_4},
            {R.id.textView19_1,R.id.textView19_2,R.id.textView19_3,R.id.textView19_4}};

    TextView mTextView[][] = new TextView[12][4];
    private final UIFragmentComponent mUiComponent = new UIFragmentComponent();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_log, container, false /* attachToRoot */);
        //mLogView = (TextView) newView.findViewById(R.id.lo);
        mSensorLogView = (TextView) newView.findViewById(R.id.sensor_log_view);
        //mScrollView = (ScrollView) newView.findViewById(R.id.log_scroll);
        mTable = (ViewGroup) newView.findViewById(R.id.TableLayout);
        //表の初期化
        for(int i = 0;i < 12;i++){
            for(int j = 0;j < 4;j++){
                Log.d("Array", i + "," + j);
                mTextView[i][j]=(TextView) newView.findViewById(Rid[i][j]);
                //Log.d("Array", i + "," + j);
            }
        }

        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragmentComponent(mUiComponent);
        }
        FileLogger currentFileLogger = mFileLogger;
        if (currentFileLogger != null) {
            currentFileLogger.setUiComponent(mUiComponent);
        }

        startLog = (Button) newView.findViewById(R.id.start_logs);
        sendFile = (Button) newView.findViewById(R.id.send_file);

        startLog.setText("ClockSync...");
        startLog.setEnabled(false);
        sendFile.setEnabled(false);

        mGNSSClockView = (TextView) newView.findViewById(R.id.GNSSClockView);

        FileLogging = false;

        startLog.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startLog.setEnabled(false);
                        sendFile.setEnabled(true);
                        Toast.makeText(getContext(), "Starting log...", Toast.LENGTH_LONG).show();
                        mFileLogger.startNewLog();
                        FileLogging = true;
                    }
                });

        sendFile.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        startLog.setEnabled(true);
                        sendFile.setEnabled(false);
                        Toast.makeText(getContext(), "Sending file...", Toast.LENGTH_LONG).show();
                        mFileLogger.send();
                        FileLogging = false;
                    }
                });
        return newView;
    }

    /**
     * A facade for UI and Activity related operations that are required for {@link GnssListener}s.
     */
    public class UIFragmentComponent {

        private static final int MAX_LENGTH = 12000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

        public synchronized void logTextFragment(final String tag, final String text, final String[][] array) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(FileLogging == false) {
                                if (SettingsFragment.GNSSClockSync == true) {
                                    startLog.setEnabled(true);
                                    startLog.setText("START LOG");
                                } else {
                                    startLog.setEnabled(false);
                                    startLog.setText("ClockSync...");
                                }
                            }
                            for(int i = 0;i < 12;i++){
                                for(int j = 0;j < 4;j++){
                                    if(j == 2){
                                        if(array[i][j] == "0") {
                                            mTextView[i][j].setTextColor(Color.parseColor("#FF9900"));
                                            mTextView[i][j].setText("Carrier Phase OFF");
                                        }else if(array[i][j] == "ADR_STATE_CYCLE_SLIP"){
                                            mTextView[i][j].setTextColor(Color.RED);
                                            mTextView[i][j].setText("ADR_STATE_CYCLE_SLIP");
                                        }else {
                                            mTextView[i][j].setTextColor(Color.BLACK);
                                            mTextView[i][j].setText(array[i][j]);
                                        }
                                    }else if(j == 3){
                                        if(array[i][j] != null) {
                                            if (Float.parseFloat(array[i][j]) < 15.0) {
                                                mTextView[i][j].setTextColor(Color.RED);
                                                mTextView[i][j].setText(array[i][j]);
                                            } else if (Float.parseFloat(array[i][j]) < 25.0) {
                                                mTextView[i][j].setTextColor(Color.parseColor("#FF9900"));
                                                mTextView[i][j].setText(array[i][j]);
                                            } else {
                                                mTextView[i][j].setTextColor(Color.GREEN);
                                                mTextView[i][j].setText(array[i][j]);
                                            }
                                        }else{
                                            mTextView[i][j].setText(array[i][j]);
                                        }
                                    }else {
                                        mTextView[i][j].setText(array[i][j]);
                                    }
                                }
                            }
                            /*Editable editable = mLogView.getEditableText();
                            int length = editable.length();
                            if (length > MAX_LENGTH) {
                                editable.delete(0, length - LOWER_THRESHOLD);
                            }*/
                        }
                    });
        }

        public synchronized void GNSSClockLog(final String String) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(String).append("\n");

            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mGNSSClockView.setText(builder);
                            if(SettingsFragment.GNSSClockSync == false) {
                                builder.append("GNSS Clock Syncronizing... Please Standby");
                                builder.append("\n");
                            }
                            /*Editable editable = mLogView.getEditableText();
                            int length = editable.length();
                            if (length > MAX_LENGTH) {
                                editable.delete(0, length - LOWER_THRESHOLD);
                            }*/
                        }
                    });
        }

        public synchronized void LocationTextFragment(final String text, int color) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mSensorLogView.setText("Location provided by google\n" + text);
                        }
                    });
        }

        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }
}
