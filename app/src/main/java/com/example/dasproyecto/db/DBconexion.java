package com.example.dasproyecto.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Helper de SQLite que se encarga de crear y actualizar la base de datos.
 */
public class DBconexion extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "tareas.db";
    private static final int DATABASE_VERSION = 4;

    /**
     * Constructor. Le dice a Android el nombre y la versión de la BD.
     *
     * @param context Contexto de la app.
     */
    public DBconexion(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Se llama la primera vez que se crea la BD.
     * Ejecuta el SQL para crear la tabla de tareas.
     *
     * @param db Instancia de la base de datos.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(DBmanager.CREATE_TABLE);
    }

    /**
     * Se llama cuando sube la versión de la BD.
     * Borra la tabla vieja y la vuelve a crear.
     *
     * @param db         La base de datos.
     * @param oldVersion Versión anterior.
     * @param newVersion Versión nueva.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + DBmanager.TABLE_NAME);
        onCreate(db);
    }
}
