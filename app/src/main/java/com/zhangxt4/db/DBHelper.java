package com.zhangxt4.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 数据库帮助类
 * 用来创建包含线程信息(ThreadInfo)的数据库thread_info
 * 表中的一条信息代表着一个线程信息
 * Created by zhangxt4 on 2015/12/15.
 */
public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "download.db";
    private static DBHelper sHelper;
    private static final int VERSION = 1;
    private static final String SQL_CREATE = "create table thread_info(_id integer primary key autoincrement," +
            "thread_id integer,url text,start integer,end integer,finished integer)";
    private static final String SQL_DROP = "drop table if exists thread_info";


    //单例模式，将构造函数私有化
    private DBHelper(Context context) {
        super(context, DB_NAME, null, VERSION);
    }

    //单例模式，只有在实例未创建时才会new一个，new过之后会直接返回它
    public static DBHelper geInstance(Context context){
        if (sHelper == null){
            //sHelper是static，所有它只会有一个（单例）
            sHelper = new DBHelper(context);
        }
        return sHelper;
    }

    /*
     * onCreate()中完成创建数据库的操作
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(SQL_DROP);
        db.execSQL(SQL_CREATE);
    }
}
