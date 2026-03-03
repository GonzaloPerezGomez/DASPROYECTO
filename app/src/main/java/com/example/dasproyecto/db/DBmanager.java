package com.example.dasproyecto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

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

    private static final String[] columnas = { COL_ID, COL_TITULO, COL_DESCRIPCION, COL_PRIORIDAD, COL_FECHALIMITE,
            COL_COMPLETADA };

    public Cursor getTareas(boolean ocultarCompletadas) {
        String seleccion = ocultarCompletadas ? COL_COMPLETADA + " = 0" : null;
        return db.query(TABLE_NAME, columnas, seleccion, null, null, null, COL_FECHALIMITE + " ASC");
    }

    public Cursor getTareasByPrioridad(boolean ocultarCompletadas) {
        String seleccion = ocultarCompletadas ? COL_COMPLETADA + " = 0" : null;
        return db.query(TABLE_NAME, columnas, seleccion, null, null, null, COL_PRIORIDAD + " DESC");
    }

    public Cursor getTareasFiltradas(String texto, boolean ocultarCompletadas) {
        String seleccion = "(" + COL_TITULO + " LIKE ? OR " + COL_DESCRIPCION + " LIKE ?)";
        if (ocultarCompletadas) {
            seleccion += " AND " + COL_COMPLETADA + " = 0";
        }
        String[] argumentos = { "%" + texto + "%", "%" + texto + "%" };
        return db.query(TABLE_NAME, columnas, seleccion, argumentos, null, null, COL_FECHALIMITE + " ASC");
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

    public void eliminarCompletadas() {
        db.delete(TABLE_NAME, COL_COMPLETADA + " = 1", null);
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

    public ArrayList<String> tareasPendientes(String fechaHoyDB) {
        ArrayList<String> tareasPendientes = new ArrayList<>();

        Cursor cursor = db.query(TABLE_NAME, columnas,
                COL_COMPLETADA + " = 0", null, null, null, null);

        SimpleDateFormat sdfDB = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        Date hoy = null;
        try {
            hoy = sdfDB.parse(fechaHoyDB);
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear la fecha de hoy: " + fechaHoyDB, e);
            return null;
        }

        while (cursor.moveToNext()) {
            String fechaTarea = cursor.getString(cursor.getColumnIndexOrThrow(COL_FECHALIMITE));
            if (fechaTarea != null && !fechaTarea.trim().isEmpty()) {
                try {
                    Date dateTarea = sdfDB.parse(fechaTarea);
                    if (dateTarea != null && (dateTarea.before(hoy) || dateTarea.equals(hoy))) {
                        tareasPendientes.add(cursor.getString(cursor.getColumnIndexOrThrow(COL_TITULO)));
                    }
                } catch (ParseException e) {
                    Log.w(TAG, "Fecha no válida en tarea: " + fechaTarea, e);
                }
            }
        }
        cursor.close();
        return tareasPendientes;
    }

    public static String formatFechaToDB(String fechaUI) {
        if (fechaUI == null || fechaUI.trim().isEmpty())
            return "";
        try {
            if (fechaUI.contains("/")) {
                String[] p = fechaUI.split("/");
                if (p.length == 3) {
                    return String.format(Locale.getDefault(), "%04d-%02d-%02d",
                            Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formateando fecha para DB: " + fechaUI, e);
        }
        return fechaUI;
    }

    public static String formatFechaToUI(String fechaDB) {
        if (fechaDB == null || fechaDB.trim().isEmpty())
            return "";
        try {
            if (fechaDB.contains("-")) {
                String[] p = fechaDB.split("-");
                if (p.length == 3) {
                    return String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(p[2]), Integer.parseInt(p[1]), Integer.parseInt(p[0]));
                }
            } else if (fechaDB.contains("/")) {
                String[] p = fechaDB.split("/");
                if (p.length == 3) {
                    return String.format(Locale.getDefault(), "%02d/%02d/%04d",
                            Integer.parseInt(p[0]), Integer.parseInt(p[1]), Integer.parseInt(p[2]));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error formateando fecha para UI: " + fechaDB, e);
        }
        return fechaDB;
    }

    public Cursor getTarea(long id) {
        return db.query(TABLE_NAME, columnas, COL_ID + " = ?",
                new String[] { String.valueOf(id) }, null, null, null);
    }
}
