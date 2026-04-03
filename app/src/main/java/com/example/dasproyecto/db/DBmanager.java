package com.example.dasproyecto.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
 * Fachada que gestiona el acceso a datos de la aplicación.
 * 
 * Tras el Hito 10, este manager actúa como interfaz de alto nivel que:
 * - Usa el ContentProvider para operaciones CRUD de tareas (que internamente usa Room + Servidor).
 * - Usa WorkManager para operaciones de autenticación y perfil.
 * - Mantiene utilidades de formato de fecha.
 */
public class DBmanager {
    private static final String TAG = "DBmanager";

    /** Constante usada como clave en Intents para pasar el ID de la tarea */
    public static final String COL_ID = "id";

    private final Context context;

    /**
     * Constructor.
     *
     * @param context Contexto de la app.
     */
    public DBmanager(Context context) {
        this.context = context;
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

        boolean ocultar = prefs.getBoolean("ocultar_completadas", false);
        String selection = "usuario_id=" + userId;
        if (ocultar) {
            selection += " AND completada=0";
        }

        Cursor cursor = context.getContentResolver().query(
                android.net.Uri.parse("content://com.example.dasproyecto.provider/tareas"),
                null,
                selection,
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
