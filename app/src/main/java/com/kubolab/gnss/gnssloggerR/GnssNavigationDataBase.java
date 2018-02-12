package com.kubolab.gnss.gnssloggerR;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

/**
 * Created by KuboLab on 2018/02/12.
 */

public class GnssNavigationDataBase {
    public Context mContext;
    SQLiteDatabase NavDB;
    SQLiteManager hlpr = new SQLiteManager(mContext);
    NavDB = hlpr.getWritableDatabase();
    public GnssNavigationDataBase(final Context context){
        mContext = context;
    }
}
