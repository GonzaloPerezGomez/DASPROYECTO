package com.example.dasproyecto;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.db.TareaRepository;

/**
 * ContentProvider que expone las tareas de la app a otras aplicaciones.
 *
 * Refactorizado en el Hito 10 para usar Room como fuente de datos local
 * en lugar de hacer peticiones HTTP directas en cada operación.
 *
 * - query(): Lee de Room (instantáneo, sin red).
 * - insert/update/delete(): Delegan en TareaRepository (HTTP + sync Room).
 */
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

    private TareaRepository repository;

    @Override
    public boolean onCreate() {
        Log.d(TAG, "ContentProvider creado");
        // No inicializamos el repository aquí porque getContext() puede no estar listo.
        return true;
    }

    /**
     * Obtiene el repository de forma lazy (se inicializa la primera vez que se necesita).
     */
    private TareaRepository getRepository() {
        if (repository == null && getContext() != null) {
            repository = new TareaRepository(getContext());
        }
        return repository;
    }

    /**
     * Extrae el usuario_id del parámetro selection.
     * Convenio: selection = "usuario_id=X"
     */
    private int extraerUserId(String selection) {
        if (selection != null && selection.startsWith("usuario_id=")) {
            try {
                return Integer.parseInt(selection.split("=")[1]);
            } catch (Exception e) {
                Log.e(TAG, "Error extrayendo userId de selection: " + selection);
            }
        }
        // Fallback: leer de SharedPreferences
        if (getContext() != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            return prefs.getInt("session_user_id", -1);
        }
        return -1;
    }

    // =========================================================================
    // QUERY — Lee desde Room (instantáneo)
    // =========================================================================

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        TareaRepository repo = getRepository();
        if (repo == null) return null;

        int match = uriMatcher.match(uri);
        int userId = extraerUserId(selection);
        boolean ocultarCompletadas = selection != null && selection.contains("completada=0");

        if (match == TAREAS) {
            return repo.getCursorTareas(userId, sortOrder, ocultarCompletadas);
        } else if (match == TAREA_ID) {
            int tareaId = Integer.parseInt(uri.getLastPathSegment());
            return repo.getCursorTarea(tareaId);
        } else {
            throw new IllegalArgumentException("Unknown URI: " + uri);
        }
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

    // =========================================================================
    // INSERT — Envía al servidor y sincroniza Room
    // =========================================================================

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        if (uriMatcher.match(uri) != TAREAS) {
            throw new IllegalArgumentException("Unknown URI for insert: " + uri);
        }
        if (values == null) return null;

        TareaRepository repo = getRepository();
        if (repo == null) return null;

        int userId = values.containsKey("usuario_id")
                ? Integer.parseInt(values.getAsString("usuario_id"))
                : extraerUserId(null);

        boolean exito = repo.insertarRemoto(
                userId,
                values.getAsString("titulo"),
                values.getAsString("descripcion"),
                Integer.parseInt(values.getAsString("prioridad")),
                values.getAsString("fechaLimite"),
                values.getAsString("latitud"),
                values.getAsString("longitud"),
                values.getAsString("direccion")
        );

        if (exito) {
            if (getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
            return Uri.withAppendedPath(CONTENT_URI, "0");
        }
        return null;
    }

    // =========================================================================
    // DELETE — Envía al servidor y sincroniza Room
    // =========================================================================

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        TareaRepository repo = getRepository();
        if (repo == null) return 0;

        int match = uriMatcher.match(uri);

        if (match == TAREA_ID) {
            int userId = extraerUserId(selection);
            int tareaId = Integer.parseInt(uri.getLastPathSegment());
            boolean exito = repo.eliminarRemoto(userId, tareaId);
            if (exito && getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
            return exito ? 1 : 0;

        } else if (match == TAREAS && "deleteCompletadas".equals(selection)) {
            int userId = (selectionArgs != null && selectionArgs.length > 0)
                    ? Integer.parseInt(selectionArgs[0]) : extraerUserId(null);
            int result = repo.eliminarCompletadasRemoto(userId);
            if (result > 0 && getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
            return result;

        } else {
            throw new IllegalArgumentException("Unsupported URI for delete: " + uri);
        }
    }

    // =========================================================================
    // UPDATE — Envía al servidor y sincroniza Room
    // =========================================================================

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (uriMatcher.match(uri) != TAREA_ID) {
            throw new IllegalArgumentException("Unsupported URI for update: " + uri);
        }
        if (values == null) return 0;

        TareaRepository repo = getRepository();
        if (repo == null) return 0;

        int userId = extraerUserId(selection);
        int tareaId = Integer.parseInt(uri.getLastPathSegment());

        boolean exito;

        // Si solo se actualiza el estado (completada), usamos el método específico
        if (values.size() == 1 && values.containsKey("completada")) {
            exito = repo.actualizarEstadoRemoto(userId, tareaId,
                    Integer.parseInt(values.getAsString("completada")));
        } else {
            exito = repo.actualizarRemoto(
                    userId, tareaId,
                    values.getAsString("titulo"),
                    values.getAsString("descripcion"),
                    Integer.parseInt(values.getAsString("prioridad")),
                    values.getAsString("fechaLimite"),
                    values.getAsString("latitud"),
                    values.getAsString("longitud"),
                    values.getAsString("direccion")
            );
        }

        if (exito && getContext() != null) getContext().getContentResolver().notifyChange(uri, null);
        return exito ? 1 : 0;
    }
}
