package com.kubolab.gnss.gnssloggerR;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Intent;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.RadioButton;

import java.lang.reflect.InvocationTargetException;
import java.util.Date;

import static android.location.GnssMeasurementsEvent.Callback.STATUS_LOCATION_DISABLED;
import static android.location.GnssMeasurementsEvent.Callback.STATUS_NOT_SUPPORTED;
import static android.location.GnssMeasurementsEvent.Callback.STATUS_READY;

/**
 * The UI fragment showing a set of configurable settings for the client to request GPS data.
 */
public class SettingsFragment extends Fragment {

//    private UiLogger mUiLogger;
//private TextView mSensorSpecView;
private TextView mAccSpecView;
    private TextView mGyroSpecView;
    private TextView mMagSpecView;
    private TextView mPressSpecView;

    private TextView mAccAvView;
    private TextView mGyroAvView;
    private TextView mMagAvView;
    private TextView mPressAvView;

    public static final String TAG = ":SettingsFragment";
    public static String SAVE_LOCATION = "G_RitZ_Logger";
    public static String FILE_PREFIX = "/" + SAVE_LOCATION + "/RINEX";
    public static String FILE_PREFIXSUB = "/" + SAVE_LOCATION + "/KML";
    public static String FILE_PREFIXACCAZI = "/" + SAVE_LOCATION + "/CSV";
    public static String FILE_NAME = "AndroidOBS";
    public static boolean CarrierPhase = false;
    public static boolean useQZ = false;
    public static boolean useGL = false;
    public static boolean GNSSClockSync = false;
    public static boolean useDeviceSensor = false;
    public static boolean ResearchMode = false;
    public static boolean SendMode = false;
    public static int GNSSMeasurementReadyMode = 10;
    private GnssContainer mGpsContainer;
    private SensorContainer mSensorContainer;
    private FileLogger mFileLogger;
    private UiLogger mUiLogger;
    private GnssContainer mGnssContainer;
    private TextView EditSaveLocation;
    private TextView mRawDataIsOk;
    private TextView mLocationIsOk;
    private TextView FTPDirectory;
    public static boolean FIRST_CHECK = false;
    public static String FTP_SERVER_DIRECTORY = "";


    public void setGpsContainer(GnssContainer value) {
        mGpsContainer = value;
    }
    public void setSensorContainer(SensorContainer value){ mSensorContainer = value; }

    private final SettingsFragment.UIFragmentSettingComponent mUiSettingComponent = new SettingsFragment.UIFragmentSettingComponent();

    public void setUILogger(UiLogger value) {
        mUiLogger = value;
    }

    public void setFileLogger(FileLogger value) {
        mFileLogger = value;
    }

    public void setGnssContainer(GnssContainer value){
        mGnssContainer = value;
    }

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false /* attachToRoot */);

        mRawDataIsOk = (TextView) view.findViewById(R.id.rawDataIsOk);
        mLocationIsOk = (TextView) view.findViewById(R.id.locationIsOk);

        mAccSpecView = (TextView) view.findViewById(R.id.accSpecView);
        mGyroSpecView = (TextView) view.findViewById(R.id.gyroSpecView);
        mMagSpecView = (TextView) view.findViewById(R.id.magSpecView);
        mPressSpecView = (TextView) view.findViewById(R.id.pressSpecView);

        mAccAvView = (TextView) view.findViewById(R.id.accAvView);
        mGyroAvView = (TextView) view.findViewById(R.id.gyroAvView);
        mMagAvView = (TextView) view.findViewById(R.id.magAvView);
        mPressAvView = (TextView) view.findViewById(R.id.pressAvView);

        //ダミーラジオボタン、スイッチの初期設定
        RadioButton rb = (RadioButton) view.findViewById(R.id.radioButtonRinex3);
        rb.setEnabled(false);
        CheckBox cb = (CheckBox) view.findViewById(R.id.checkBoxPseudorange);
        cb.setEnabled(false);
        cb = (CheckBox) view.findViewById(R.id.useGPS);
        cb.setEnabled(false);
        cb = (CheckBox) view.findViewById(R.id.outputRINEX);
        cb.setEnabled(false);
        cb = (CheckBox) view.findViewById(R.id.outputKML);
        cb.setEnabled(false);
        cb = (CheckBox) view.findViewById(R.id.outputNmea);
        cb.setEnabled(false);
        cb = (CheckBox) view.findViewById(R.id.outputSensor);
        cb.setEnabled(false);


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
        Date now = new Date();
        int observation = now.getYear() - 100;
        //final TextView FileName = (TextView) view.findViewById(R.id.FileName);
        //FileName.setText(FILE_NAME);
        final TextView FileExtension = (TextView) view.findViewById(R.id.FileExtension);
        FileExtension.setText("$log/RINEX/\"prefix\"." + observation + "o");

//        FTPDirectory = (TextView) view.findViewById(R.id.FTPDirectory);

        EditSaveLocation = (TextView) view.findViewById(R.id.EditSaveLocation);
        EditSaveLocation.setText("(Current Time)");
        EditSaveLocation.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                if(s.toString() == ""){
                    EditSaveLocation.setText("(Current Time)");
                }else{
                    FILE_NAME = s.toString();
                }
            }
        });

//        final TextView testtextview = (TextView) view.findViewById(R.id.textView11);
//        testtextview.setText("tesettest");



        final Switch SendFileSwitch = (Switch) view.findViewById(R.id.FileSend);
        SendFileSwitch.setChecked(false);
        SendFileSwitch.setOnCheckedChangeListener(
                new OnCheckedChangeListener() {

                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                        if (isChecked) {
                            SendMode = true;
                        } else {
                            SendMode = false;
                        }
                    }
                });
        /*Button resetGNSSClock = (Button) view.findViewById(R.id.rstbutton);

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
        });*/

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
        UiLogger currentUiLogger = mUiLogger;
        if (currentUiLogger != null) {
            //Log.d("mUILogger","Pointer OK");
            currentUiLogger.setUISettingComponent(mUiSettingComponent);
        }
        GnssContainer currentGnssContainer = mGnssContainer;
        if(currentGnssContainer != null){
            currentGnssContainer.setUISettingComponent(mUiSettingComponent);
        }
        if(FIRST_CHECK == false) {
            CheckGNSSMeasurementsReady(GNSSMeasurementReadyMode);
            FIRST_CHECK = true;
        }

        return view;
    }

    private void CheckGNSSMeasurementsReady(int status){
        if(status == STATUS_NOT_SUPPORTED){
            new AlertDialog.Builder(getContext())
                    .setTitle("DEVICE NOT SUPPORTED")
                    .setMessage("This device is not suppored please check supported device list\nhttps://developer.android.com/guide/topics/sensors/gnss.html")
                    .setPositiveButton("OK", null)
                    .show();
            //MainActivity.getInstance().finishAndRemoveTask();
            mRawDataIsOk.setText("Unavailable");
        }
        if(status == STATUS_LOCATION_DISABLED){
            new AlertDialog.Builder(getContext())
                    .setTitle("LOCATION DISABLED")
                    .setMessage("Location is disabled. \nplease turn on your GPS Setting")
                    .setPositiveButton("OK", null)
                    .show();
            mLocationIsOk.setText("Unavailable");
        }
        if(status == STATUS_READY){
            //Log.d("GNSSStatus","GNSSMeasurements Status Ready");
            Toast.makeText(getContext(),"GNSS Measurements Ready",Toast.LENGTH_SHORT).show();
        }
    }

    private void logException(String errorMessage, Exception e) {
        //Log.e(GnssContainer.TAG + TAG, errorMessage, e);
        Toast.makeText(getContext(), errorMessage, Toast.LENGTH_LONG).show();
    }

    public class UIFragmentSettingComponent {

        private static final int MAX_LENGTH = 12000;
        private static final int LOWER_THRESHOLD = (int) (MAX_LENGTH * 0.5);
        public synchronized void SettingFTPDirectory(final String DirectoryName){
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                        FTPDirectory.setText(DirectoryName);
                        }
                    });
        }

        public synchronized void SettingTextFragment(final String FileName) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            if(EditSaveLocation.getText().toString().indexOf("Current Time") != -1){
                                FILE_NAME = FileName;
                            }
                        }
                    });
        }

        public synchronized void Lockout(final boolean status) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            EditSaveLocation.setEnabled(status);

                        }
                    });
        }

        public synchronized void SettingFragmentSensorSpec(final String SensorSpec[]) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccSpecView.setText(SensorSpec[0]);
                            mGyroSpecView.setText(SensorSpec[1]);
                            mMagSpecView.setText(SensorSpec[2]);
                            mPressSpecView.setText(SensorSpec[3]);
                        }
                    });
        }

        public synchronized void SettingFragmentSensorAvairable(final String SensorAvairable[]) {
            Activity activity = getActivity();
            if (activity == null) {
                return;
            }
            activity.runOnUiThread(
                    new Runnable() {
                        @Override
                        public void run() {
                            mAccAvView.setText(SensorAvairable[0]);
                            mGyroAvView.setText(SensorAvairable[1]);
                            mMagAvView.setText(SensorAvairable[2]);
                            mPressAvView.setText(SensorAvairable[3]);
                        }
                    });
        }

        public void startActivity(Intent intent) {
            getActivity().startActivity(intent);
        }
    }
}
//aaa