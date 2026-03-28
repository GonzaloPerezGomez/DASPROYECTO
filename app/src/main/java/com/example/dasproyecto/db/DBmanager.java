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

import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.preference.PreferenceManager;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.example.dasproyecto.ConexionWorker;

/**
 * Clase que gestiona todas las operaciones con la base de datos de tareas:
 * crear, leer, actualizar y borrar (CRUD). También tiene métodos para formatear
 * fechas.
 */
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
    private final Context context;

    /**
     * Constructor. Crea la conexión a la BD.
     *
     * @param context Contexto de la app.
     */
    public DBmanager(Context context) {
        this.context = context;
        conexion = new DBconexion(context);
    }

    /**
     * Abre la base de datos en modo escritura.
     *
     * @throws SQLException Si hay algún problema al abrir.
     */
    public void open() throws SQLException {
        db = conexion.getWritableDatabase();
    }

    /**
     * Cierra la conexión con la BD.
     */
    public void close() {
        conexion.close();
    }

    private static final String[] columnas = { COL_ID, COL_TITULO, COL_DESCRIPCION, COL_PRIORIDAD, COL_FECHALIMITE,
            COL_COMPLETADA };



    /**
     * Busca las tareas pendientes cuya fecha límite ya haya pasado o sea hoy.
     * Devuelve sus títulos para mostrarlos en la notificación.
     */
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
        Log.i(TAG, "Tareas pendientes: " + tareasPendientes.toString());
        return tareasPendientes;
    }

    /**
     * Convierte una fecha de formato visual (dd/MM/yyyy) a formato de BD
     * (yyyy-MM-dd).
     */
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

    /**
     * Convierte una fecha de formato BD (yyyy-MM-dd) a formato visual (dd/MM/yyyy).
     */
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



    // =========================================================================
    // MÉTODOS REMOTOS (WorkManager Wrapper)
    // =========================================================================

    public LiveData<WorkInfo> loginRemoto(String email, String password) {
        Data datos = new Data.Builder()
                .putString("accion", "login")
                .putString("email", email)
                .putString("password", password)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> registroRemoto(String nombre, String email, String password) {
        Data datos = new Data.Builder()
                .putString("accion", "registro")
                .putString("nombre", nombre)
                .putString("email", email)
                .putString("password", password)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> getTareasRemoto(String ordenUI) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);
        if (userId == -1) {
            Log.e(TAG, "No hay usuario logueado");
            return null;
        }

        boolean ocultarCompletadas = prefs.getBoolean("ocultar_completadas", false);
        
        String ordenSQL = "fechaLimite";
        if ("prioridad".equals(ordenUI)) {
            ordenSQL = "prioridad";
        }

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "getTareas")
                .putInt("usuario_id", userId)
                .putBoolean("ocultar_completadas", ocultarCompletadas)
                .putString("orden", ordenSQL)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(datos)
                .build();

        Log.d(TAG, "Lanzando WorkManager para getTareas. Usuario: " + userId + " | Ocultar Completadas: "
                + ocultarCompletadas + " | Orden: " + ordenSQL);
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> insertarRemoto(String titulo, String desc, int prioridad, String fecha, Double latitud, Double longitud, String direccion) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "insertTarea")
                .putInt("usuario_id", userId)
                .putString("titulo", titulo)
                .putString("descripcion", desc)
                .putInt("prioridad", prioridad)
                // Enviamos "" si la fecha es null para no romper el PHP, o no ponemos la key
                .putString("fechaLimite", fecha != null ? fecha : "")
                .putString("latitud", latitud != null ? String.valueOf(latitud) : "")
                .putString("longitud", longitud != null ? String.valueOf(longitud) : "")
                .putString("direccion", direccion != null ? direccion : "")
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(datos)
                .build();

        Log.d(TAG, "Lanzando WorkManager para insertarTarea: " + titulo);
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> getTareaRemoto(long tareaId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "getTarea")
                .putInt("usuario_id", userId)
                .putInt("tarea_id", (int) tareaId)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class).setInputData(datos).build();
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> actualizarTareaCompletaRemoto(long tareaId, String titulo, String desc, int prioridad,
            String fecha, Double latitud, Double longitud, String direccion) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "updateTarea")
                .putInt("usuario_id", userId)
                .putInt("tarea_id", (int) tareaId)
                .putString("titulo", titulo)
                .putString("descripcion", desc)
                .putInt("prioridad", prioridad)
                .putString("fechaLimite", fecha != null ? fecha : "")
                .putString("latitud", latitud != null ? String.valueOf(latitud) : "")
                .putString("longitud", longitud != null ? String.valueOf(longitud) : "")
                .putString("direccion", direccion != null ? direccion : "")
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class).setInputData(datos).build();
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> actualizarEstadoRemoto(long tareaId, int estado) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "updateTarea")
                .putInt("usuario_id", userId)
                .putInt("tarea_id", (int) tareaId)
                .putInt("completada", estado)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class).setInputData(datos).build();
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> eliminarRemoto(long tareaId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "deleteTarea")
                .putInt("usuario_id", userId)
                .putInt("tarea_id", (int) tareaId)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class).setInputData(datos).build();
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }

    public LiveData<WorkInfo> eliminarCompletadasRemoto() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Data datos = new Data.Builder()
                .putString("accion", "tareas")
                .putString("tarea_accion", "deleteCompletadas")
                .putInt("usuario_id", userId)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class).setInputData(datos).build();
        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }
}
