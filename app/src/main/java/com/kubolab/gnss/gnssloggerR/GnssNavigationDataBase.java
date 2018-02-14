package com.kubolab.gnss.gnssloggerR;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by KuboLab on 2018/02/12.
 */

public class GnssNavigationDataBase {
    public Context mContext;
    public GnssNavigationDataBase(final Context context){
        mContext = context;
    }

    public String getIonosphericDataStr(){
        SQLiteDatabase NavDB;
        SQLiteManager hlpr = new SQLiteManager(mContext);
        NavDB = hlpr.getWritableDatabase();
        StringBuilder FourthSubframe = new StringBuilder();
        if(hlpr.existColumn(NavDB,"IONOSPHERIC", "GPSA0")) {
            double a0 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSA0");
            double a1 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSA1");
            double a2 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSA2");
            double a3 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSA3");

            double b0 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSB0");
            double b1 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSB1");
            double b2 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSB2");
            double b3 = hlpr.searchIndex(NavDB, "IONOSPHERIC", "GPSB3");
            FourthSubframe.append(String.format("GPS_a0 %1.4E\nGPS_a1 %1.4E\nGPS_a2 %1.4E\nGPS_a3 %1.4E\n",a0,a1,a2,a3));
            FourthSubframe.append(String.format("GPS_b0 %1.4E\nGPS_b1 %1.4E\nGPS_b2 %1.4E\nGPS_b3 %1.4E\n",b0,b1,b2,b3));
        }else {
            FourthSubframe.append("NOTFOUND DATA");
        }
        NavDB.close();
        return FourthSubframe.toString();
    }
}
