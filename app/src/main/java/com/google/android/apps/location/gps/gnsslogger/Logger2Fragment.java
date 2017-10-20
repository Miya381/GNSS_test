package com.google.android.apps.location.gps.gnsslogger;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class Logger2Fragment extends Fragment {

    private TextView mLogView;
    private TextView mSensorLogView;
    private ScrollView mScrollView;
    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private int MaxCanvusWidth;
    private int MaxCanvusHeight;
    private float[][] SkyPlotPos = new float[30][2];
    private float[] NorthPos = new float[2];
    private String[] SkyPlotSvid = new String[30];
    private int satNumber = 0;
    private float deviceAzimuth = 0;

    private final Logger2Fragment.UIFragment2Component mUiComponent = new Logger2Fragment.UIFragment2Component();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View newView = inflater.inflate(R.layout.fragment_log2, container, false /* attachToRoot */);
        FrameLayout frameLayout = (FrameLayout) newView.findViewById(R.id.fragment);
        frameLayout.addView(new TestView(this.getActivity()));
        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragment2Component(mUiComponent);
        }
        return newView;
    }

    public class SampleHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            final TestView testview = new TestView((Context) activity);
            testview.invalidate();
        }

        public void sleep(long delayMills) {
            //使用済みメッセージの削除
            removeMessages(0);
            sendMessageDelayed(obtainMessage(0), delayMills);
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        SampleHandler sampleHandler = new SampleHandler();
        sampleHandler.sleep(0);
    }

    public class TestView extends View{
        Paint paint = new Paint();
        private boolean isAttached;
        public TestView(Context context) {
            super(context);
        }
        protected void onAttachedToWindow(){
            Handler handler = new Handler(){
                public void handleMessage(Message msg){
                    if(isAttached) {
                        invalidate();
                        sendEmptyMessageDelayed(0, 1000);
                    }
                }
            };
            isAttached = true;
            handler.sendEmptyMessageDelayed(0,1000);
            super.onAttachedToWindow();
        }

        protected void onDraw(Canvas canvas){
            MaxCanvusWidth = canvas.getWidth();
            MaxCanvusHeight = canvas.getHeight();
            paint.setColor(Color.BLACK);
            //Rect rect = new Rect(100, 200, 300, 400);
            paint.setStyle(Paint.Style.STROKE);
            paint.setStrokeWidth(1);
            canvas.drawCircle(MaxCanvusWidth/2,MaxCanvusHeight/2,MaxCanvusWidth/2, paint);
            canvas.drawCircle(MaxCanvusWidth/2,MaxCanvusHeight/2,MaxCanvusWidth/2 * (float)Math.cos(4/Math.PI), paint);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            canvas.drawCircle(MaxCanvusWidth/2,MaxCanvusHeight/2,10.0f, paint);
            canvas.drawLine(MaxCanvusWidth,MaxCanvusHeight/2,0,MaxCanvusHeight/2,paint);
            canvas.drawLine(MaxCanvusWidth/2,MaxCanvusHeight/2 - MaxCanvusWidth/2,MaxCanvusWidth/2,MaxCanvusHeight/2 + MaxCanvusWidth/2,paint);
            //北方向を表示
            paint.setTextSize(100);
            canvas.drawText("N", MaxCanvusWidth/2 + NorthPos[0] - 30, MaxCanvusHeight/2 + NorthPos[1] - 30, paint);
            for(int i = 0;i < satNumber;i++){
                if(SkyPlotSvid[i] != null) {
                    if(SkyPlotSvid[i].indexOf("R") != -1) {
                        paint.setColor(Color.BLUE);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.BLUE);
                        canvas.drawText(SkyPlotSvid[i], MaxCanvusWidth / 2 + SkyPlotPos[i][0] - 100.0f, MaxCanvusHeight / 2 + SkyPlotPos[i][1] + 50.0f, paint);
                    }
                    else if(SkyPlotSvid[i].indexOf("Q") != -1){
                        paint.setColor(Color.MAGENTA);
                        paint.setStyle(Paint.Style.FILL) ;
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.MAGENTA);
                        canvas.drawText(SkyPlotSvid[i], MaxCanvusWidth / 2 + SkyPlotPos[i][0] - 100.0f, MaxCanvusHeight / 2 + SkyPlotPos[i][1] + 50.0f, paint);
                    }else if(SkyPlotSvid[i].indexOf("G") != -1){
                        paint.setColor(Color.GREEN);
                        paint.setStyle(Paint.Style.FILL) ;
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.GREEN);
                        canvas.drawText(SkyPlotSvid[i], MaxCanvusWidth / 2 + SkyPlotPos[i][0] - 100.0f, MaxCanvusHeight / 2 + SkyPlotPos[i][1] + 50.0f, paint);
                    }
                }
            }

        }
    }

    public class UIFragment2Component {

        private static final int MAX_LENGTH = 12000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);

        public synchronized void log2TextFragment(final String[] svid, final float[][] pos, final int satnumber) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            //final TestView testview = new TestView((Context)activity);
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0;i < satnumber;i++){
                                //まずは仰角を変換
                                double Altitude = Math.cos(pos[i][1]);
                                //Log.d("Altitude",String.valueOf(Altitude));
                                Altitude = Altitude * (MaxCanvusWidth/2);
                                float azimuth = (float) Math.toDegrees(pos[i][0]);
                                azimuth = azimuth + 90;
                                if(azimuth > 360){
                                    azimuth = azimuth - 360;
                                }
                                if(SettingsFragment.useDeviceSensor) {
                                    float gnssAzimuth = azimuth;
                                    gnssAzimuth = azimuth - deviceAzimuth;
                                    if (gnssAzimuth < 0) {
                                        gnssAzimuth = gnssAzimuth + 360;
                                    }
                                    azimuth = gnssAzimuth;
                                }
                                SkyPlotPos[i][0] = (float) (Altitude * Math.cos(Math.toRadians(azimuth)));
                                SkyPlotPos[i][1] = (float) (Altitude * Math.sin(Math.toRadians(azimuth)));
                                //Log.d("SkyPlotPos",SkyPlotPos[i][0] + "," + SkyPlotPos[i][1]);
                                SkyPlotSvid[i] = svid[i];
                                //SkyPlotPos[i][0] = (float) (SkyPlotPos[i][0] * Math.cos(Math.toRadians(deviceAzimuth)) - SkyPlotPos[i][1] * Math.sin(Math.toRadians(deviceAzimuth)));
                                //SkyPlotPos[i][1] = (float) (SkyPlotPos[i][0] * Math.sin(Math.toRadians(deviceAzimuth)) + SkyPlotPos[i][1] * Math.cos(Math.toRadians(deviceAzimuth)));
                            }
                            float DevAzimuth = -90;
                            if(SettingsFragment.useDeviceSensor) {
                                DevAzimuth = -deviceAzimuth;
                                DevAzimuth = DevAzimuth - 90;
                                if (DevAzimuth < -360) {
                                    DevAzimuth = DevAzimuth + 360;
                                }
                            }
                            NorthPos[0] = (float) ((MaxCanvusWidth / 2 - 10) * Math.cos(Math.toRadians(DevAzimuth)));
                            NorthPos[1] = (float) ((MaxCanvusWidth / 2 - 10) * Math.sin(Math.toRadians(DevAzimuth)));
                            satNumber = satnumber;
                        }
                    });
        }

        public synchronized void log2SensorFragment(final double azimuth) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            //final TestView testview = new TestView((Context)activity);
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            deviceAzimuth = (float) azimuth;
                        }
                    });
        }
        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }

}
