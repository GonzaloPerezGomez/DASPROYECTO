package com.example.dasproyecto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

public class DBmanager {
    private static final String TAG = "DBmanager";
    public static final String TABLE_NAME = "tareas";
    public static final String COL_ID = "id";
    public static final String COL_TITULO = "titulo";
    public static final String COL_DESCRIPCION = "descripcion";
    public static final String COL_PRIORIDAD = "prioridad";
    public static final String COL_FECHALIMITE = "fechaLimite";
    public static final String COL_COMPLETADA = "completada";

    public static final String CREATE_TABLE = "CREATE TABLE tareas (" +
            "id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "titulo TEXT NOT NULL," +
            "descripcion TEXT," +
            "prioridad INTEGER DEFAULT 0," +
            "fechaLimite TEXT," +
            "completada INTEGER DEFAULT 0" +
            ")";

    private static final String[] columnas = { COL_ID, COL_TITULO, COL_DESCRIPCION, COL_PRIORIDAD, COL_FECHALIMITE,
            COL_COMPLETADA };

    private final DBconexion conexion;
    private SQLiteDatabase db;

    public DBmanager(Context context) {
        conexion = new DBconexion(context);
    }

    public void open() throws SQLException {
        db = conexion.getWritableDatabase();
    }

    public void close() {
        conexion.close();
    }

    public Cursor getTareas() {
        return db.query(TABLE_NAME, columnas, null, null, null, null, COL_PRIORIDAD + " DESC");
    }

    public void actualizarEstado(long id, int estado) {
        ContentValues values = new ContentValues();
        values.put(COL_COMPLETADA, estado);
        db.update(TABLE_NAME, values, COL_ID + " = " + id, null);
    }

    public void insertar(String titulo, String desc, int prioridad, String fecha) {
        ContentValues values = new ContentValues();
        values.put(COL_TITULO, titulo);
        values.put(COL_DESCRIPCION, desc);
        values.put(COL_PRIORIDAD, prioridad);
        values.put(COL_FECHALIMITE, fecha);
        db.insert(TABLE_NAME, null, values);
        Log.i(TAG, "Tarea guardada: " + titulo);
    }

    public void eliminar(long id) {
        db.delete(TABLE_NAME, COL_ID + " = " + id, null);
    }

    public void actualizarTareaCompleta(long tareaId, String titulo, String descripcion, int prioridad, String fecha) {
        ContentValues values = new ContentValues();
        values.put(COL_TITULO, titulo);
        values.put(COL_DESCRIPCION, descripcion);
        values.put(COL_PRIORIDAD, prioridad);
        values.put(COL_FECHALIMITE, fecha);
        int actualizados = db.update(TABLE_NAME, values, COL_ID + " = " + tareaId, null);
        if (actualizados == 0) {
            Log.e(TAG, "No se encontró la tarea con ID: " + tareaId);
        } else {
            Log.i(TAG, "Tarea actualizada: " + titulo);
        }
    }

    public Cursor getTarea(long id) {
        return db.query(TABLE_NAME, columnas, COL_ID + " = ?",
                new String[] { String.valueOf(id) }, null, null, null);
    }
}
