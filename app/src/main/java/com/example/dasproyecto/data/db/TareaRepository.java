package com.example.dasproyecto.data.db;

import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import com.example.dasproyecto.widget.TareasWidgetProvider;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Repository que implementa el patrón Single Source of Truth.
 * - Lecturas: siempre desde Room (caché local, instantáneo).
 * - Escrituras: primero al servidor remoto, luego actualiza Room si tiene éxito.
 * - Sincronización: descarga todas las tareas del servidor y reemplaza la caché local.
 */
public class TareaRepository {

    private static final String TAG = "TareaRepository";
    private static final String SERVER_URL = "http://34.68.1.253:81/tareas.php";

    private final TareaDao tareaDao;
    private final Context context;

    public TareaRepository(Context context) {
        this.context = context.getApplicationContext();
        this.tareaDao = AppDatabase.getInstance(this.context).tareaDao();
    }

    // =========================================================================
    // LECTURAS (desde Room — instantáneas)
    // =========================================================================

    /**
     * Obtiene todas las tareas de un usuario desde la caché local.
     * Debe llamarse desde un hilo secundario.
     */
    public List<TareaEntity> getTareasLocal(int usuarioId, String orden) {
        if ("prioridad".equals(orden)) {
            return tareaDao.getTareasPorPrioridad(usuarioId);
        }
        return tareaDao.getTareasPorFecha(usuarioId);
    }

    /**
     * Obtiene una tarea por su ID desde la caché local.
     */
    public TareaEntity getTareaLocal(int tareaId) {
        return tareaDao.getTarea(tareaId);
    }

    /**
     * Devuelve un Cursor con las tareas ordenadas y filtradas (para ContentProvider).
     */
    public Cursor getCursorTareas(int usuarioId, String sortOrder, boolean ocultarCompletadas) {
        StringBuilder queryString = new StringBuilder("SELECT * FROM tareas WHERE usuario_id = " + usuarioId);
        
        if (ocultarCompletadas) {
            queryString.append(" AND completada = 0");
        }
        
        if (sortOrder != null && sortOrder.equals("prioridad")) {
            queryString.append(" ORDER BY prioridad DESC");
        } else {
            // Por defecto, o si es "fechaLimite"
            queryString.append(" ORDER BY fechaLimite ASC");
        }
        
        return tareaDao.getCursorTareasDinamico(new androidx.sqlite.db.SimpleSQLiteQuery(queryString.toString()));
    }

    /**
     * Devuelve un Cursor con una tarea específica (para el ContentProvider).
     */
    public Cursor getCursorTarea(int tareaId) {
        return tareaDao.getCursorTarea(tareaId);
    }

    // =========================================================================
    // SINCRONIZACIÓN (Servidor → Room)
    // =========================================================================

    /**
     * Descarga todas las tareas del servidor y reemplaza la caché local.
     * Debe llamarse desde un hilo secundario.
     *
     * @return true si la sincronización fue exitosa.
     */
    public boolean sincronizar(int usuarioId) {
        try {
            android.net.Uri.Builder builder = new android.net.Uri.Builder();
            builder.appendQueryParameter("accion", "getTareas");
            builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));
            builder.appendQueryParameter("ocultar_completadas", "false");
            builder.appendQueryParameter("orden", "fechaLimite");

            String respuesta = realizarPeticionHttp(builder.build().getEncodedQuery());
            if (respuesta == null) return false;

            JSONObject json = new JSONObject(respuesta);
            if (!json.optBoolean("exito", false)) return false;

            JSONArray tareasArray = json.getJSONArray("tareas");
            List<TareaEntity> tareas = new ArrayList<>();

            for (int i = 0; i < tareasArray.length(); i++) {
                JSONObject t = tareasArray.getJSONObject(i);
                TareaEntity entity = new TareaEntity(
                        t.getInt("id"),
                        usuarioId,
                        t.optString("titulo", ""),
                        t.optString("descripcion", ""),
                        t.optInt("prioridad", 0),
                        t.optString("fechaLimite", ""),
                        t.optInt("completada", 0),
                        t.optString("latitud", ""),
                        t.optString("longitud", ""),
                        t.optString("direccion", "")
                );
                tareas.add(entity);
            }

            // Reemplazar toda la caché local de este usuario
            tareaDao.deleteAll(usuarioId);
            tareaDao.insertAll(tareas);

            Log.d(TAG, "Sincronización completada: " + tareas.size() + " tareas descargadas");

            // Notificar al widget para que se actualice inmediatamente
            android.content.Intent intent = new android.content.Intent(context, TareasWidgetProvider.class);
            intent.setAction(android.appwidget.AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            int[] ids = android.appwidget.AppWidgetManager.getInstance(context)
                    .getAppWidgetIds(new android.content.ComponentName(context, TareasWidgetProvider.class));
            intent.putExtra(android.appwidget.AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(intent);

            return true;

        } catch (Exception e) {
            Log.e(TAG, "Error durante la sincronización", e);
            return false;
        }
    }

    // =========================================================================
    // ESCRITURAS (Servidor remoto + actualizar Room)
    // =========================================================================

    /**
     * Inserta una tarea en el servidor. Si tiene éxito, sincroniza Room.
     */
    public boolean insertarRemoto(int usuarioId, String titulo, String desc, int prioridad,
                                  String fecha, String latitud, String longitud, String direccion) {
        android.net.Uri.Builder builder = new android.net.Uri.Builder();
        builder.appendQueryParameter("accion", "insertTarea");
        builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));
        builder.appendQueryParameter("titulo", titulo);
        builder.appendQueryParameter("descripcion", desc);
        builder.appendQueryParameter("prioridad", String.valueOf(prioridad));
        builder.appendQueryParameter("fechaLimite", fecha != null ? fecha : "");
        builder.appendQueryParameter("latitud", latitud != null ? latitud : "");
        builder.appendQueryParameter("longitud", longitud != null ? longitud : "");
        builder.appendQueryParameter("direccion", direccion != null ? direccion : "");

        String resp = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (resp != null) {
            try {
                JSONObject json = new JSONObject(resp);
                if (json.optBoolean("exito", false)) {
                    // Tras insertar en el servidor, sincronizamos Room
                    sincronizar(usuarioId);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando respuesta insert", e);
            }
        }
        return false;
    }

    /**
     * Actualiza una tarea en el servidor. Si tiene éxito, sincroniza Room.
     */
    public boolean actualizarRemoto(int usuarioId, int tareaId, String titulo, String desc,
                                    int prioridad, String fecha, String latitud, String longitud, String direccion) {
        android.net.Uri.Builder builder = new android.net.Uri.Builder();
        builder.appendQueryParameter("accion", "updateTarea");
        builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));
        builder.appendQueryParameter("tarea_id", String.valueOf(tareaId));
        builder.appendQueryParameter("titulo", titulo);
        builder.appendQueryParameter("descripcion", desc);
        builder.appendQueryParameter("prioridad", String.valueOf(prioridad));
        builder.appendQueryParameter("fechaLimite", fecha != null ? fecha : "");
        builder.appendQueryParameter("latitud", latitud != null ? latitud : "");
        builder.appendQueryParameter("longitud", longitud != null ? longitud : "");
        builder.appendQueryParameter("direccion", direccion != null ? direccion : "");

        String resp = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (resp != null) {
            try {
                JSONObject json = new JSONObject(resp);
                if (json.optBoolean("exito", false)) {
                    sincronizar(usuarioId);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando respuesta update", e);
            }
        }
        return false;
    }

    /**
     * Actualiza solo el estado (completada) de una tarea.
     */
    public boolean actualizarEstadoRemoto(int usuarioId, int tareaId, int estado) {
        android.net.Uri.Builder builder = new android.net.Uri.Builder();
        builder.appendQueryParameter("accion", "updateTarea");
        builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));
        builder.appendQueryParameter("tarea_id", String.valueOf(tareaId));
        builder.appendQueryParameter("completada", String.valueOf(estado));

        String resp = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (resp != null) {
            try {
                JSONObject json = new JSONObject(resp);
                if (json.optBoolean("exito", false)) {
                    sincronizar(usuarioId);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando respuesta updateEstado", e);
            }
        }
        return false;
    }

    /**
     * Elimina una tarea del servidor y sincroniza Room.
     */
    public boolean eliminarRemoto(int usuarioId, int tareaId) {
        android.net.Uri.Builder builder = new android.net.Uri.Builder();
        builder.appendQueryParameter("accion", "deleteTarea");
        builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));
        builder.appendQueryParameter("tarea_id", String.valueOf(tareaId));

        String resp = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (resp != null) {
            try {
                JSONObject json = new JSONObject(resp);
                if (json.optBoolean("exito", false)) {
                    sincronizar(usuarioId);
                    return true;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando respuesta delete", e);
            }
        }
        return false;
    }

    /**
     * Elimina las tareas completadas del servidor y sincroniza Room.
     */
    public int eliminarCompletadasRemoto(int usuarioId) {
        android.net.Uri.Builder builder = new android.net.Uri.Builder();
        builder.appendQueryParameter("accion", "deleteCompletadas");
        builder.appendQueryParameter("usuario_id", String.valueOf(usuarioId));

        String resp = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (resp != null) {
            try {
                JSONObject json = new JSONObject(resp);
                if (json.optBoolean("exito", false)) {
                    sincronizar(usuarioId);
                    return 1;
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando respuesta deleteCompletadas", e);
            }
        }
        return 0;
    }

    // =========================================================================
    // HTTP (Comunicación directa con el servidor)
    // =========================================================================

    /**
     * Realiza una petición HTTP POST síncrona al backend PHP.
     */
    private String realizarPeticionHttp(String parametros) {
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(SERVER_URL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setConnectTimeout(5000);
            urlConnection.setReadTimeout(5000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

            if (parametros != null && !parametros.isEmpty()) {
                PrintWriter out = new PrintWriter(urlConnection.getOutputStream());
                out.print(parametros);
                out.close();
            }

            int statusCode = urlConnection.getResponseCode();
            if (statusCode == 200) {
                BufferedInputStream inputStream = new BufferedInputStream(urlConnection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
                StringBuilder result = new StringBuilder();
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    result.append(line);
                }
                inputStream.close();

                // Limpiar posibles warnings de PHP
                String rawData = result.toString();
                int start = rawData.indexOf("{");
                int end = rawData.lastIndexOf("}");
                if (start != -1 && end != -1 && end >= start) {
                    return rawData.substring(start, end + 1);
                }
                return rawData;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en petición HTTP: " + e.getMessage());
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return null;
    }
}
