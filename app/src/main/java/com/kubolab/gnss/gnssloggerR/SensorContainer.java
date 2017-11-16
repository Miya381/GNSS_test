package com.kubolab.gnss.gnssloggerR;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.List;

/**
 * 回転角度取得クラス
 *
 * @author
 *
 */
public class SensorContainer {
    //センサ生データ用
    private String sensorRaw[] = new String[6];
    /** デバッグ用 */
    private static final boolean DEBUG = true;
    private static final String TAG = "OrientationListener";
    /** 行列数 */
    private static final int MATRIX_SIZE = 16;
    /** 三次元(XYZ) */
    private static final int DIMENSION = 3;
    /** センサー管理クラス */
    private SensorManager mManager;
    /** 地磁気行列 */
    private float[] mMagneticValues;
    private float MagX,MagY,MagZ;
    /** 加速度行列 */
    private float[] mAccelerometerValues;
    private float RawX,RawY,RawZ;
    private int AccAzi;
    // ジャイロ
    private float[] mGyroValues;
    private float GyroX, GyroY, GyroZ;
    // ジャイロuncalibratred
    private float[] mGyroUncalibratedValues;
    private float GyroUncalibratedX, GyroUncalibratedY, GyroUncalibratedZ;
    private float GyroDriftX, GyroDriftY, GyroDriftZ;
    /** 気圧 **/
    private float[] mPressureValues;
    private float Altitude;
    private float Pressure;
    private float LastAltitude = 0.0f;
    /** X軸の回転角度 */
    private double mPitchX;
    /** Y軸の回転角度 */
    private double mRollY;
    /** Z軸の回転角度(方位角) */
    private double mAzimuthZ;
    private final Context mContext;
    private final UiLogger mLogger;
    private final FileLogger mFileLogger;

    // ローパスフィルタ用変数
    private float currentOrientationZValues = 0.0f;
    private float currentAccelerationZValues = 0.0f;
    private float currentAccelerationXValues = 0.0f;
    private float currentAccelerationYValues = 0.0f;
    private float currentOrientationXValues = 0.0f;
    private float currentOrientationYValues = 0.0f;
    private double x,y,z = 0;
    private double mx,my,mz = 0;

    // 歩数カウンタ関連
    private boolean passcounter = true;
    private int counter = 0;

    /**
     * センサーイベント取得開始
     *
     * @param context
     *            コンテキスト
     */
    public SensorContainer(Context context, UiLogger Logger, FileLogger FileLogger) {
        this.mContext = context;
        this.mLogger = Logger;
        this.mFileLogger = FileLogger;
    }

    public void registerSensor(){
        if (mManager == null) {
            // 初回実行時
            mManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        }
        mManager.registerListener(listener, mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), 100000);
        // 加速度センサー登録
        mManager.registerListener(listener, mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), 100000);
        //気圧センサー登録
        mManager.registerListener(listener,mManager.getDefaultSensor(Sensor.TYPE_PRESSURE), 100000);
        //ジャイロセンサー登録
        mManager.registerListener(listener,mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE), 100000);
        //ジャイロセンサーuncalibrated登録
        mManager.registerListener(listener,mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE_UNCALIBRATED), 100000);

        Sensor sensor;
        String[] strTmp = new String[4];
        //String strTmp="";
        sensor = mManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (sensor == null) {
            strTmp[0] = "利用不可";
        }else {
            strTmp[0] = sensor.getName();
        }
        sensor = mManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        if (sensor == null) {
            strTmp[1] = "利用不可";
        }else {
            strTmp[1] = sensor.getName();
        }
        sensor = mManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (sensor == null) {
            strTmp[2] = "利用不可";
        }else {
            strTmp[2] = sensor.getName();
        }
        sensor = mManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
        if (sensor == null) {
            strTmp[3] = "利用不可";
        }else {
            strTmp[3] = sensor.getName();
        }
        mLogger.SensorSpec(strTmp);
    }

    public void unregisterSensor(){
        mManager.unregisterListener(listener);
    }

    /**
     * X軸の回転角度を取得する
     *
     * @return X軸の回転角度
     */
    public synchronized double getPitch() {
        return mPitchX;
    }

    /**
     * Y軸の回転角度を取得する
     *
     * @return Y軸の回転角度
     */
    public synchronized double getRoll() {
        return mRollY;
    }

    /**
     * Z軸の回転角度(方位角)を取得する
     *
     * @return Z軸の回転角度
     */
    public synchronized double getAzimuth() {
        return mAzimuthZ;
    }

    /**
     * ラジアンを角度に変換する
     *
     * @param angrad
     *            ラジアン
     * @return 角度
     */
    private int radianToDegrees(float angrad) {
        return (int) Math.floor(angrad >= 0 ? Math.toDegrees(angrad) : 360 + Math.toDegrees(angrad));
    }

   SensorEventListener listener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            // センサーイベント
            switch (event.sensor.getType()) {
                case Sensor.TYPE_MAGNETIC_FIELD:
                    // 地磁気センサー
                    mMagneticValues = event.values.clone();
                    MagX = mMagneticValues[0];
                    MagY = mMagneticValues[1];
                    MagZ = mMagneticValues[2];
                    break;
                case Sensor.TYPE_ACCELEROMETER:
                    // 加速度センサー
                    mAccelerometerValues = event.values.clone();
                    RawX = mAccelerometerValues[0];
                    RawY = mAccelerometerValues[1];
                    RawZ = mAccelerometerValues[2];
                    break;
                case Sensor.TYPE_PRESSURE:
                    //気圧センサー
                    mPressureValues = event.values.clone();
                    Pressure  = mPressureValues[0];
                case Sensor.TYPE_GYROSCOPE:
                    //ジャイロ
                    mGyroValues = event.values.clone();
                    GyroX  = mGyroValues[0];
                    GyroY  = mGyroValues[1];
                    GyroZ  = mGyroValues[2];
                case Sensor.TYPE_GYROSCOPE_UNCALIBRATED:
                    //ジャイロ
                    mGyroUncalibratedValues = event.values.clone();
                    GyroUncalibratedX  = mGyroUncalibratedValues[0];
                    GyroUncalibratedY  = mGyroUncalibratedValues[1];
                    GyroUncalibratedZ  = mGyroUncalibratedValues[2];
//                    GyroDriftX  = mGyroUncalibratedValues[3];
//                    GyroDriftY  = mGyroUncalibratedValues[4];
//                    GyroDriftZ  = mGyroUncalibratedValues[5];
                default:
                    // それ以外は無視
                    return;
            }
            if (mMagneticValues != null && mAccelerometerValues != null) {
                float[] rotationMatrix = new float[MATRIX_SIZE];
                float[] inclinationMatrix = new float[MATRIX_SIZE];
                float[] remapedMatrix = new float[MATRIX_SIZE];
                float[] orientationValues = new float[DIMENSION];
                // 加速度センサーと地磁気センサーから回転行列を取得
                SensorManager.getRotationMatrix(rotationMatrix, inclinationMatrix, mAccelerometerValues, mMagneticValues);
                SensorManager.remapCoordinateSystem(rotationMatrix, SensorManager.AXIS_MINUS_X, SensorManager.AXIS_Y, remapedMatrix);
                SensorManager.getOrientation(remapedMatrix, orientationValues);
                //ローパsフィルタ
                x = (x * 0.9 + RawX * 0.1);
                y = (y * 0.9 + RawY * 0.1);
                z = (z * 0.9 + RawZ * 0.1);
                mx = (mx * 0.9 + MagX * 0.1);
                my = (my * 0.9 + MagY * 0.1);
                mz = (mz * 0.9 + MagZ * 0.1);
                // ラジアン値を変換し、それぞれの回転角度を取得する
                if(!SettingsFragment.ResearchMode) {
                    //Androidオリジナルシステム
                    //https://www.nxp.com/docs/en/application-note/AN4248.pdf の軸変換 Gxは-AccY GyはAccX
                    mAzimuthZ = radianToDegrees(orientationValues[0]);
                    mPitchX = radianToDegrees((orientationValues[1]));
                    mRollY = radianToDegrees(orientationValues[2]);
                }else {
                    //研究用システム
                    double Gx = -y;
                    double Gy = x;
                    double Gz = z;
                    mRollY = Math.atan2(Gy,Gz);
                    mPitchX  = Math.atan( -Gx / (Gy * Math.sin(mRollY) + Gz * Math.cos(mRollY)));
                    /*double tmp = mRollY;
                    mRollY = mPitchX;
                    mPitchX = tmp;*/
                    //地磁気センサーオフセット
                    double Bx = mx;
                    double By = -my;
                    double Bz = -mz;
                    double GxOff = 0;
                    double GyOff = 0;
                    double GzOff = 0;
                    mAzimuthZ = Math.atan2((((Bz - GzOff)*Math.sin(mRollY)) - ((By - GyOff)*Math.cos(mRollY))) , ((Bx - GxOff)*Math.cos(mPitchX) + (By - GyOff)*Math.sin(mPitchX)*Math.sin(mRollY) + (Bz - GzOff)*Math.sin(mPitchX)*Math.cos(mRollY)));
                    //mAzimuthZ = -mAzimuthZ;
                    //加速度センサーのWGS84系での下向きの加速度を求める
                    double az = - RawX * Math.sin(mRollY) + RawY * Math.sin((mPitchX)) + RawZ * Math.cos((mPitchX)) * Math.cos(mRollY);
                    double bx = RawX * Math.cos(mRollY) + RawZ * Math.sin(mRollY);
                    double by = RawX * Math.sin(mPitchX) * Math.sin(mRollY) + RawY * Math.cos(mPitchX) - RawZ * Math.sin(mPitchX) * Math.cos(mRollY);
                    double ax = bx * Math.cos(mAzimuthZ) - by * Math.sin(mAzimuthZ);
                    double ay = bx * Math.sin(mAzimuthZ) + by * Math.cos(mAzimuthZ);

                    currentOrientationZValues = (float)az * 0.1f + currentOrientationZValues * (1.0f - 0.1f);
                    currentAccelerationZValues = (float)az - currentOrientationZValues;
                    currentOrientationXValues = (float)ax * 0.1f + currentOrientationXValues * (1.0f - 0.1f);
                    currentAccelerationXValues = (float)ax - currentOrientationXValues;
                    currentOrientationYValues = (float)ay * 0.1f + currentOrientationYValues * (1.0f - 0.1f);
                    currentAccelerationYValues = (float)ay - currentOrientationYValues;
                    if(passcounter == true) {
                        if (currentAccelerationZValues <= -1.5) {
                            counter++;
                            passcounter = false;
                            mFileLogger.onSensorListener("", (float) mAzimuthZ,(float)0.72,Altitude);
                        }
                    }else{
                        if (currentAccelerationZValues >= 1.0) {
                            passcounter = true;
                        }
                    }
                    if(Math.abs(currentAccelerationYValues) >= 0.00000000001 || Math.abs(currentAccelerationXValues) >= 0.0000000000001) {
                        double AccAziRad = Math.atan(currentAccelerationYValues / currentAccelerationXValues);
                        AccAzi = radianToDegrees((float) AccAziRad);
                    }
                }
                //気圧から高度を算出
                if(mPressureValues != null){
                    Altitude = (float) -(((Math.pow((mPressureValues[0]/1023.0),(1/5.257)) - 1)*(6.6 + 273.15)) / 0.0065);
                    sensorRaw[5] = String.format("Ambient Pressure = %f", Pressure);
                }

                if(mMagneticValues != null){
                    sensorRaw[4] = String.format("X = %f, Y = %f, Z = %f", MagX, MagY, MagZ);
                }
                if(mGyroValues != null){
                    sensorRaw[2] = String.format("X = %f, Y = %f, Z = %f", GyroX, GyroY, GyroZ);
                }
                if(mGyroUncalibratedValues != null){
//                    sensorRaw[1] = String.format("X = %f, Y = %f, Z = %f\n dX = %f, dY = %f, dZ = %f (drift estimates)", GyroUncalibratedX, GyroUncalibratedY, GyroUncalibratedZ, GyroDriftX, GyroDriftY, GyroDriftZ);
                    sensorRaw[1] = String.format("X = %f, Y = %f, Z = %f\n dX = %f, dY = %f, dZ = %f (drift estimates)", GyroUncalibratedX, GyroUncalibratedY, GyroUncalibratedZ, 0.001, 0.001, 0.001);
                }

                sensorRaw[0] = String.format("X = %f, Y = %f, Z = %f", RawX, RawY, RawZ);

                mLogger.onSensorRawListener(sensorRaw);


                if(SettingsFragment.ResearchMode) {
                    mLogger.onSensorListener(String.format("Pitch = %f , Roll = %f , Azimuth = %f \n Altitude = %f \n WalkCounter = %d \n AccAzi = %d", Math.toDegrees(mPitchX), Math.toDegrees(mRollY), Math.toDegrees(mAzimuthZ) + 180, LastAltitude - Altitude, counter, AccAzi), Math.toDegrees(mAzimuthZ) + 180, currentAccelerationZValues, LastAltitude - Altitude);
                }else{
                    mLogger.onSensorListener(String.format("Pitchh = %2.1f\nRoll = %2.1f\nAzimuth = %3.1f\nAltitude = %3.1f", mPitchX, mRollY, mAzimuthZ, Altitude), mAzimuthZ, currentAccelerationZValues, LastAltitude - Altitude);
                    //mLogger.onSensorListener(String.format("MagX = %f \n MagY = %f \n MagZ = %f",mMagneticValues[0],mMagneticValues[1],mMagneticValues[2]),mAzimuthZ,currentAccelerationZValues,LastAltitude - Altitude);
                }
                //mFileLogger.onSensorListener("",mAzimuthZ,currentAccelerationZValues);//aaaaa
                LastAltitude = Altitude;
            }
        }
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            //処理なし
        }
    };
}

