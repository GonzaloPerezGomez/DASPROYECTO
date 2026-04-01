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

import org.json.JSONObject;

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
    // MÉTODOS CONTENT PROVIDER (Síncronos para Tareas)
    // =========================================================================

    public int eliminarCompletadasProvider() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        return context.getContentResolver().delete(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas"),
                "deleteCompletadas",
                new String[]{String.valueOf(userId)}
        );
    }

    public JSONObject getTareasProvider(String ordenUI) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);
        String ordenSQL = "prioridad".equals(ordenUI) ? "prioridad" : "fechaLimite";

        Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas"),
                null,
                "usuario_id=" + userId,
                null,
                ordenSQL
        );

        JSONObject result = new JSONObject();
        try {
            if (cursor != null) {
                result.put("exito", true);
                org.json.JSONArray tareasArray = new org.json.JSONArray();
                while (cursor.moveToNext()) {
                    JSONObject t = new JSONObject();
                    t.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                    t.put("titulo", cursor.getString(cursor.getColumnIndexOrThrow("titulo")));
                    t.put("descripcion", cursor.getString(cursor.getColumnIndexOrThrow("descripcion")));
                    t.put("prioridad", cursor.getInt(cursor.getColumnIndexOrThrow("prioridad")));
                    t.put("fechaLimite", cursor.getString(cursor.getColumnIndexOrThrow("fechaLimite")));
                    t.put("completada", cursor.getInt(cursor.getColumnIndexOrThrow("completada")));
                    t.put("latitud", cursor.getString(cursor.getColumnIndexOrThrow("latitud")));
                    t.put("longitud", cursor.getString(cursor.getColumnIndexOrThrow("longitud")));
                    t.put("direccion", cursor.getString(cursor.getColumnIndexOrThrow("direccion")));
                    tareasArray.put(t);
                }
                result.put("tareas", tareasArray);
            } else {
                result.put("exito", false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getTareasProvider", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public JSONObject getTareaProvider(long tareaId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas/" + tareaId),
                null,
                "usuario_id=" + userId,
                null, null
        );

        JSONObject result = new JSONObject();
        try {
            if (cursor != null && cursor.moveToFirst()) {
                result.put("exito", true);
                JSONObject t = new JSONObject();
                t.put("id", cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                t.put("titulo", cursor.getString(cursor.getColumnIndexOrThrow("titulo")));
                t.put("descripcion", cursor.getString(cursor.getColumnIndexOrThrow("descripcion")));
                t.put("prioridad", cursor.getInt(cursor.getColumnIndexOrThrow("prioridad")));
                t.put("fechaLimite", cursor.getString(cursor.getColumnIndexOrThrow("fechaLimite")));
                t.put("completada", cursor.getInt(cursor.getColumnIndexOrThrow("completada")));
                t.put("latitud", cursor.getString(cursor.getColumnIndexOrThrow("latitud")));
                t.put("longitud", cursor.getString(cursor.getColumnIndexOrThrow("longitud")));
                t.put("direccion", cursor.getString(cursor.getColumnIndexOrThrow("direccion")));
                result.put("tarea", t);
            } else {
                result.put("exito", false);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getTareaProvider", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return result;
    }

    public boolean insertarProvider(String titulo, String desc, int prioridad, String fecha, Double latitud, Double longitud, String direccion) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        ContentValues values = new ContentValues();
        values.put("usuario_id", String.valueOf(userId)); 
        values.put("titulo", titulo);
        values.put("descripcion", desc);
        values.put("prioridad", String.valueOf(prioridad));
        values.put("fechaLimite", fecha != null ? fecha : "");
        values.put("latitud", latitud != null ? String.valueOf(latitud) : "");
        values.put("longitud", longitud != null ? String.valueOf(longitud) : "");
        values.put("direccion", direccion != null ? direccion : "");

        android.net.Uri result = context.getContentResolver().insert(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas"),
                values
        );
        return result != null;
    }

    public boolean actualizarTareaCompletaProvider(long tareaId, String titulo, String desc, int prioridad, String fecha, Double latitud, Double longitud, String direccion) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        ContentValues values = new ContentValues();
        values.put("titulo", titulo);
        values.put("descripcion", desc);
        values.put("prioridad", String.valueOf(prioridad));
        values.put("fechaLimite", fecha != null ? fecha : "");
        values.put("latitud", latitud != null ? String.valueOf(latitud) : "");
        values.put("longitud", longitud != null ? String.valueOf(longitud) : "");
        values.put("direccion", direccion != null ? direccion : "");

        int updated = context.getContentResolver().update(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas/" + tareaId),
                values,
                "usuario_id=" + userId,
                null
        );
        return updated > 0;
    }

    public boolean actualizarEstadoProvider(long tareaId, int estado) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        ContentValues values = new ContentValues();
        values.put("completada", String.valueOf(estado));

        int updated = context.getContentResolver().update(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas/" + tareaId),
                values,
                "usuario_id=" + userId,
                null
        );
        return updated > 0;
    }

    public boolean eliminarProvider(long tareaId) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefs.getInt("session_user_id", -1);

        int deleted = context.getContentResolver().delete(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas/" + tareaId),
                "usuario_id=" + userId,
                null
        );
        return deleted > 0;
    }

    // =========================================================================
    // MÉTODOS WORKMANAGER (Asíncronos para Perfil/Auth)
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

    public LiveData<WorkInfo> actualizarFotoPerfilRemoto(int usuarioId, String fotoPath) {
        Data datos = new Data.Builder()
                .putString("accion", "perfil")
                .putInt("usuario_id", usuarioId)
                .putString("foto_path", fotoPath)
                .build();

        OneTimeWorkRequest req = new OneTimeWorkRequest.Builder(ConexionWorker.class)
                .setInputData(datos)
                .build();

        WorkManager.getInstance(context).enqueue(req);
        return WorkManager.getInstance(context).getWorkInfoByIdLiveData(req.getId());
    }
}
