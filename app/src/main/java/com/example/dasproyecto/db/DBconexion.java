package com.example.dasproyecto.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DBconexion extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tareas.db";
    private static final int DATABASE_VERSION = 1;

    public DBconexion(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBmanager.CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBmanager.TABLE_NAME);
    }
}
