package com.example.dasproyecto;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.example.dasproyecto.db.DBmanager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;

public class TareasContentProvider extends ContentProvider {

    private static final String TAG = "TareasContentProvider";

    public static final String AUTHORITY = "com.example.dasproyecto.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tareas");

    private static final int TAREAS = 1;
    private static final int TAREA_ID = 2;

    private static final UriMatcher uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        uriMatcher.addURI(AUTHORITY, "tareas", TAREAS);
        uriMatcher.addURI(AUTHORITY, "tareas/#", TAREA_ID);
    }

    private static final String SERVER_URL = "http://34.28.161.49:81/tareas.php";

    @Override
    public boolean onCreate() {
        Log.d(TAG, "ContentProvider creado");
        return true;
    }

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

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int match = uriMatcher.match(uri);
        String ordenSQL = sortOrder != null ? sortOrder : "fechaLimite";

        // Asumimos que podemos rescatar el usuario asociado (o le pasamos el id en un bundle,
        // pero ContentResolver.query args no permiten ints nativos fácilmente. Pasaremos usuario_id como selection).
        // Por simplificar, si el selection contiene "usuario_id=", lo extraemos.
        int userId = -1;
        if (selection != null && selection.startsWith("usuario_id=")) {
            try {
                userId = Integer.parseInt(selection.split("=")[1]);
            } catch (Exception e) {}
        }

        Uri.Builder builder = new Uri.Builder();
        if (match == TAREAS) {
            builder.appendQueryParameter("accion", "getTareas");
            builder.appendQueryParameter("usuario_id", String.valueOf(userId));
            builder.appendQueryParameter("ocultar_completadas", "false"); // o extraer de args
            builder.appendQueryParameter("orden", ordenSQL);
        } else if (match == TAREA_ID) {
            builder.appendQueryParameter("accion", "getTarea");
            builder.appendQueryParameter("usuario_id", String.valueOf(userId));
            builder.appendQueryParameter("tarea_id", uri.getLastPathSegment());
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        String respuestaJson = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (respuestaJson == null) return null;

        MatrixCursor matrixCursor = new MatrixCursor(new String[]{
                DBmanager.COL_ID, DBmanager.COL_TITULO, DBmanager.COL_DESCRIPCION,
                DBmanager.COL_PRIORIDAD, DBmanager.COL_FECHALIMITE, DBmanager.COL_COMPLETADA,
                "latitud", "longitud", "direccion"
        });

        try {
            JSONObject json = new JSONObject(respuestaJson);
            if (json.getBoolean("exito")) {
                if (match == TAREAS) {
                    JSONArray tareasArray = json.getJSONArray("tareas");
                    for (int i = 0; i < tareasArray.length(); i++) {
                        JSONObject t = tareasArray.getJSONObject(i);
                        matrixCursor.addRow(new Object[]{
                                t.optInt("id", 0),
                                t.optString("titulo", ""),
                                t.optString("descripcion", ""),
                                t.optInt("prioridad", 0),
                                t.optString("fechaLimite", ""),
                                t.optInt("completada", 0),
                                t.optString("latitud", ""),
                                t.optString("longitud", ""),
                                t.optString("direccion", "")
                        });
                    }
                } else if (match == TAREA_ID) {
                    JSONObject t = json.getJSONObject("tarea");
                    matrixCursor.addRow(new Object[]{
                            t.optInt("id", 0),
                            t.optString("titulo", ""),
                            t.optString("descripcion", ""),
                            t.optInt("prioridad", 0),
                            t.optString("fechaLimite", ""),
                            t.optInt("completada", 0),
                            t.optString("latitud", ""),
                            t.optString("longitud", ""),
                            t.optString("direccion", "")
                    });
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error parseando JSON en query(): " + e.getMessage());
        }

        return matrixCursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (uriMatcher.match(uri)) {
            case TAREAS:
                return "vnd.android.cursor.dir/vnd.com.example.dasproyecto.provider.tareas";
            case TAREA_ID:
                return "vnd.android.cursor.item/vnd.com.example.dasproyecto.provider.tareas";
            default:
                throw new IllegalArgumentException("Unsupported URI: " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != TAREAS) {
            throw new IllegalArgumentException("Unknown URI for insert: " + uri);
        }
        
        if (values == null) return null;

        Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter("accion", "insertTarea");
        for (String key : values.keySet()) {
            builder.appendQueryParameter(key, values.getAsString(key));
        }

        String respuestaJson = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (respuestaJson != null) {
            try {
                JSONObject json = new JSONObject(respuestaJson);
                if (json.getBoolean("exito")) {
                    // asumiremos el exito. En algunos backends devuelve el 'tarea_id'
                    long newId = json.optLong("tarea_id", -1);
                    return Uri.withAppendedPath(CONTENT_URI, String.valueOf(newId));
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando inserción: " + e.getMessage());
            }
        }
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Uri.Builder builder = new Uri.Builder();

        int match = uriMatcher.match(uri);
        if (match == TAREA_ID) {
            builder.appendQueryParameter("accion", "deleteTarea");
            builder.appendQueryParameter("tarea_id", uri.getLastPathSegment());
            if (selection != null && selection.startsWith("usuario_id=")) {
                builder.appendQueryParameter("usuario_id", selection.split("=")[1]);
            }
        } else if (match == TAREAS && "deleteCompletadas".equals(selection)) {
            builder.appendQueryParameter("accion", "deleteCompletadas");
            if (selectionArgs != null && selectionArgs.length > 0) {
                builder.appendQueryParameter("usuario_id", selectionArgs[0]);
            }
        } else {
            throw new IllegalArgumentException("Unsupported URI for delete: " + uri);
        }

        String respuestaJson = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (respuestaJson != null) {
            try {
                JSONObject json = new JSONObject(respuestaJson);
                if (json.getBoolean("exito")) {
                    return 1; // 1 row deleted
                }
            } catch (Exception e) {}
        }
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (uriMatcher.match(uri) != TAREA_ID) {
            throw new IllegalArgumentException("Unsupported URI for update: " + uri);
        }
        if (values == null) return 0;

        Uri.Builder builder = new Uri.Builder();
        builder.appendQueryParameter("accion", "updateTarea");
        builder.appendQueryParameter("tarea_id", uri.getLastPathSegment());

        for (String key : values.keySet()) {
            builder.appendQueryParameter(key, values.getAsString(key));
        }
        
        if (selection != null && selection.startsWith("usuario_id=")) {
            builder.appendQueryParameter("usuario_id", selection.split("=")[1]);
        }

        String respuestaJson = realizarPeticionHttp(builder.build().getEncodedQuery());
        if (respuestaJson != null) {
            try {
                JSONObject json = new JSONObject(respuestaJson);
                if (json.getBoolean("exito")) {
                    return 1; // 1 row updated
                }
            } catch (Exception e) {}
        }
        return 0;
    }
}
