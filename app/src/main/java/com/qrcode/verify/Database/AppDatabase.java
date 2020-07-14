package com.qrcode.verify.Database;


import com.raizlabs.android.dbflow.annotation.Database;

/**
 * Created by Hongliang Luo on 2019/3/15.
 **/
@Database(name=AppDatabase.NAME,version = AppDatabase.VERSION)
public class AppDatabase {
    final static public String NAME="AppDatabase";
    final static public int VERSION=1;
}
