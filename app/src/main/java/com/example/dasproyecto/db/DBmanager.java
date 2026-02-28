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
        return db.query(TABLE_NAME, columnas, null, null, null, null, COL_FECHALIMITE + " ASC");
    }

    public Cursor getTareasByPrioridad() {
        return db.query(TABLE_NAME, columnas, null, null, null, null, COL_PRIORIDAD + " DESC");
    }

    public Cursor getTareasFiltradas(String texto) {
        String seleccion = COL_TITULO + " LIKE ? OR " + COL_DESCRIPCION + " LIKE ?";
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

    public void deleteCompleted() {
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

    /**
     * Cuenta las tareas NO completadas cuya fechaLimite es ANTERIOR a la fecha
     * dada.
     * Parsea cada fechaLimite (formato "d/M/yyyy") para compararla con 'fechaHoy'.
     *
     * @param fechaHoy la fecha de hoy en formato "d/M/yyyy"
     * @return número de tareas atrasadas
     */
    public ArrayList<String> tareasPendientes(String fechaHoy) {
        // 1. Crear el formateador de fechas con el mismo patrón usado al guardar
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        ArrayList<String> tareasPendientes = new ArrayList<>();

        // 2. Parsear la fecha de hoy a un objeto Date para poder comparar
        Date hoy;
        try {
            hoy = sdf.parse(fechaHoy);
        } catch (ParseException e) {
            Log.e(TAG, "Error al parsear la fecha de hoy: " + fechaHoy, e);
            return null; // Si no se puede parsear, devolvemos 0
        }

        // 3. Consultar todas las tareas NO completadas (completada = 0)
        Cursor cursor = db.query(TABLE_NAME, columnas,
                COL_COMPLETADA + " = 0", null, null, null, null);

        // 4. Recorrer cada tarea y comparar su fecha
        while (cursor.moveToNext()) {
            String fechaTarea = cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_FECHALIMITE));
            if (fechaTarea != null && !fechaTarea.isEmpty()) {
                try {
                    Date dateTarea = sdf.parse(fechaTarea);
                    // before() devuelve true si dateTarea es anterior a hoy
                    if (dateTarea != null && (dateTarea.before(hoy) || dateTarea.equals(hoy))) {
                        String tituloTarea = cursor.getString(
                                cursor.getColumnIndexOrThrow(COL_TITULO));
                        tareasPendientes.add(tituloTarea);
                    }
                } catch (ParseException e) {
                    Log.w(TAG, "Fecha no válida en tarea: " + fechaTarea, e);
                }
            }
        }
        cursor.close();
        return tareasPendientes;
    }

    public Cursor getTarea(long id) {
        return db.query(TABLE_NAME, columnas, COL_ID + " = ?",
                new String[] { String.valueOf(id) }, null, null, null);
    }
}
