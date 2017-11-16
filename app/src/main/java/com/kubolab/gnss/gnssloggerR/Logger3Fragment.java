package com.kubolab.gnss.gnssloggerR;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

public class Logger3Fragment extends Fragment {

    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private TextView mSensorLogView;
    private TextView mSensorRawAccView;
    private TextView mSensorRawPressView;
    private TextView mSensorRawMagView;
    private TextView mSensorRawGyroView;
    private TextView mSensorRawGyroUncalibratedView;

    private final Logger3Fragment.UIFragment3Component mUiComponent = new Logger3Fragment.UIFragment3Component();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }
    @Override
    public void onAttach(Context context){
        super.onAttach(context);
        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragment3Component(mUiComponent);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_log3, container, false /* attachToRoot */);
        FrameLayout frameLayout = (FrameLayout) newView.findViewById(R.id.fragment);
        mSensorLogView = (TextView) newView.findViewById(R.id.sensorview);
        mSensorRawAccView = (TextView) newView.findViewById(R.id.sensorAccView);
        mSensorRawPressView = (TextView) newView.findViewById(R.id.sensorPressView);
        mSensorRawMagView = (TextView) newView.findViewById(R.id.sensorMagView);
        mSensorRawGyroView = (TextView) newView.findViewById(R.id.sensorGyroView);
        mSensorRawGyroUncalibratedView = (TextView) newView.findViewById(R.id.sensorGyroUncalibratedView);
        //int ImageWidth = SkyplotBG.getDrawable().getBounds().width();
        //int ImageHeight = SkyplotBG.getDrawable().getBounds().height();
        //Matrix mtx = new Matrix();
        //mtx.postTranslate(-ImageWidth/2 , -ImageHeight/2);
        //mtx.postRotate(deviceAzimuth);
        //.d("Rotate",String.valueOf(deviceAzimuth));
        //mtx.postTranslate(ImageWidth/2 , ImageHeight/2);
        //SkyplotBG.setImageMatrix(mtx);
        return newView;
    }

    public class UIFragment3Component {

        private static final int MAX_LENGTH = 12000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

        public synchronized void log3TextFragment(final String SensorString) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mSensorLogView.setText(SensorString);
                        }
                    });
        }

        public synchronized void log3SensorRawFragment(final String SensorRawString[]) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mSensorRawAccView.setText(SensorRawString[0]);
                            mSensorRawGyroUncalibratedView .setText(SensorRawString[1]);
                            mSensorRawGyroView.setText(SensorRawString[2]);
                            mSensorRawMagView.setText(SensorRawString[4]);
                            mSensorRawPressView.setText(SensorRawString[5]);
                        }
                    });
        }

        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }

}
