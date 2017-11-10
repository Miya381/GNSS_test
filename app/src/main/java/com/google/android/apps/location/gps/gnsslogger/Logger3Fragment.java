package com.google.android.apps.location.gps.gnsslogger;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class Logger3Fragment extends Fragment {

    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private TextView mSensorLogView;

    private final Logger3Fragment.UIFragment3Component mUiComponent = new Logger3Fragment.UIFragment3Component();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_log3, container, false /* attachToRoot */);
        FrameLayout frameLayout = (FrameLayout) newView.findViewById(R.id.fragment);
        mSensorLogView = (TextView) newView.findViewById(R.id.sensorview);
        //int ImageWidth = SkyplotBG.getDrawable().getBounds().width();
        //int ImageHeight = SkyplotBG.getDrawable().getBounds().height();
        //Matrix mtx = new Matrix();
        //mtx.postTranslate(-ImageWidth/2 , -ImageHeight/2);
        //mtx.postRotate(deviceAzimuth);
        //.d("Rotate",String.valueOf(deviceAzimuth));
        //mtx.postTranslate(ImageWidth/2 , ImageHeight/2);
        //SkyplotBG.setImageMatrix(mtx);
        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragment3Component(mUiComponent);
        }
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
        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }

}
