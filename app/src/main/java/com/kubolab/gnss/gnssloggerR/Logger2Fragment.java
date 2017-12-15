package com.kubolab.gnss.gnssloggerR;

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
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;

public class Logger2Fragment extends Fragment {

    private TextView mLogView;
    private TextView mSensorLogView;
    private ImageView SkyplotBG;
    private ScrollView mScrollView;
    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private int MaxCanvusWidth;
    private int MaxCanvusHeight;
    private float[][] SkyPlotPos = new float[50][2];
    private float[] NorthPos = new float[2];
    private String[] SkyPlotSvid = new String[50];
    private int msatNumber = 0;
    private float deviceAzimuth = 0;
    private Bitmap skyplotbg = null;

    private final Logger2Fragment.UIFragment2Component mUiComponent = new Logger2Fragment.UIFragment2Component();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_log2, container, false /* attachToRoot */);
    }

    public  void onViewCreated(View view, Bundle savedInstanceState){
        FrameLayout frameLayout = (FrameLayout) view.findViewById(R.id.fragment);
        frameLayout.addView(new TestView(this.getActivity()));
        SkyplotBG = (ImageView) view.findViewById(R.id.skyplotview);
        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            currentUiLogger.setUiFragment2Component(mUiComponent);
        }
    }

    public class TestView extends SurfaceView implements SurfaceHolder.Callback, Runnable{
        Paint paint = new Paint();
        private boolean isAttached;
        private Thread mLooper;
        private SurfaceHolder mHolder;
        private long mTime =0;
        Activity activity = getActivity();
        public TestView(final Context context) {
            super(context);
            initialize();
        }
        private void initialize() {
            getHolder().setFormat(PixelFormat.TRANSLUCENT);
            getHolder().addCallback(this);
            setZOrderOnTop(true);
        }
        public void surfaceDestroyed(SurfaceHolder surfaceholder) {
            mLooper = null;
        }

        public void surfaceCreated(final SurfaceHolder surfaceholder) {
            mHolder = surfaceholder;
            mLooper = new Thread(this);
        }



        public void surfaceChanged(SurfaceHolder surfaceholder, int i, int j, int k) {
            if(mLooper != null) {
                //mTime = System.currentTimeMillis();
                mLooper.start();
                //doDraw(surfaceholder);
            }
        }
        public void run() {
            while (mLooper != null) {
                Activity activity = getActivity();
                if (activity == null) {
                    try {
                        mLooper.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (IllegalStateException e){
                        e.printStackTrace();
                    }
                } else {
                    activity.runOnUiThread(
                            new Runnable() {
                                @Override
                                public void run() {
                                    doDraw(mHolder);
                                    SkyplotBG.setRotation(-deviceAzimuth);
                                }
                            });
                    try {
                        mLooper.sleep(1000);
                    } catch (InterruptedException e) {
                         e.printStackTrace();
                    } catch (IllegalStateException e){
                        e.printStackTrace();
                    }
                }
            }
        }


        private void doDraw(SurfaceHolder holder) {
            Canvas canvas = holder.lockCanvas();
            if (canvas == null){ return; }
            try
            {
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                draw(canvas);
            }
            finally
            {
                if ( canvas != null )
                {
                    holder.unlockCanvasAndPost(canvas);
                    canvas = null;
                }
            }
        }

        @Override
        public void draw(Canvas canvas){
            //canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
            //canvas.drawColor(Color.WHITE);
            MaxCanvusWidth = canvas.getWidth();
            MaxCanvusHeight = canvas.getHeight();
            paint.setColor(Color.BLACK);
            paint.setTextSize(50);
            paint.setStyle(Paint.Style.FILL);
            paint.setAntiAlias(true);
            for(int i = 0; i < msatNumber; i++){
                if(SkyPlotSvid[i] != null) {
                    if(SkyPlotSvid[i].indexOf("R") != -1) {
                        paint.setColor(Color.GREEN);
                        paint.setStyle(Paint.Style.FILL);
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.GREEN);
                        canvas.drawText(SkyPlotSvid[i], MaxCanvusWidth / 2 + SkyPlotPos[i][0] - 100.0f, MaxCanvusHeight / 2 + SkyPlotPos[i][1] + 50.0f, paint);
                    }
                    else if(SkyPlotSvid[i].indexOf("J") != -1){
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
                        paint.setColor(Color.parseColor("#58D3F7"));
                        paint.setStyle(Paint.Style.FILL) ;
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.parseColor("#58D3F7"));
                        canvas.drawText(SkyPlotSvid[i], MaxCanvusWidth / 2 + SkyPlotPos[i][0] - 100.0f, MaxCanvusHeight / 2 + SkyPlotPos[i][1] + 50.0f, paint);
                    }else if(SkyPlotSvid[i].indexOf("E") != -1){
                        paint.setColor(Color.parseColor("#0101DF"));
                        paint.setStyle(Paint.Style.FILL) ;
                        paint.setAntiAlias(true);
                        canvas.drawRect(MaxCanvusWidth/2 + SkyPlotPos[i][0] - 10,MaxCanvusHeight/2 + SkyPlotPos[i][1] - 10 , MaxCanvusWidth/2 + SkyPlotPos[i][0] + 10 ,MaxCanvusHeight/2 + SkyPlotPos[i][1] + 10, paint);
                        paint.setStyle(Paint.Style.FILL_AND_STROKE);
                        paint.setStrokeWidth(5);
                        paint.setTextSize(50);
                        paint.setColor(Color.parseColor("#0101DF"));
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
            final TestView testview = new TestView((Context)activity);
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            for(int i = 0;i < satnumber;i++){
                                //まずは仰角を変換
                                double Altitude = 1 - pos[i][1]/90;
                                Altitude = Altitude * (MaxCanvusWidth/2);
                                float azimuth = pos[i][0];
                                //Log.d("Azimuth",String.valueOf(azimuth));
                                azimuth = azimuth - 90;
                                if(azimuth < 0){
                                    azimuth = azimuth + 360;
                                }
                                if(SettingsFragment.useDeviceSensor) {
                                    float gnssAzimuth = azimuth;
                                    gnssAzimuth = azimuth - deviceAzimuth;
                                    if (gnssAzimuth < 0) {
                                        gnssAzimuth = gnssAzimuth + 360;
                                    }
                                    azimuth = gnssAzimuth;
                                }
                                SkyPlotPos[i][0] = (float) (0.888 * Altitude * Math.cos(Math.toRadians(azimuth)));
                                SkyPlotPos[i][1] = (float) (0.888 * Altitude * Math.sin(Math.toRadians(azimuth)));
                                SkyPlotSvid[i] = svid[i];
                            }
                            float DevAzimuth = -90;
                            if(SettingsFragment.useDeviceSensor) {
                                DevAzimuth = -deviceAzimuth;
                                DevAzimuth = DevAzimuth - 90;
                                if (DevAzimuth < -360) {
                                    DevAzimuth = DevAzimuth + 360;
                                }
                            }
                            msatNumber = satnumber;
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
