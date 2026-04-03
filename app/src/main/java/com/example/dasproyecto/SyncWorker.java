package com.example.dasproyecto;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.dasproyecto.db.TareaRepository;

/**
 * Worker que sincroniza la base de datos local (Room) con el servidor remoto.
 * Se ejecuta periódicamente en segundo plano para mantener la caché actualizada.
 */
public class SyncWorker extends Worker {

    private static final String TAG = "SyncWorker";

    public SyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "Iniciando sincronización periódica...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int userId = prefs.getInt("session_user_id", -1);

        if (userId == -1) {
            Log.w(TAG, "No hay usuario logueado, saltando sincronización");
            return Result.success();
        }

        TareaRepository repo = new TareaRepository(getApplicationContext());
        boolean exito = repo.sincronizar(userId);

        if (exito) {
            Log.d(TAG, "Sincronización periódica completada con éxito");
            return Result.success();
        } else {
            Log.w(TAG, "Sincronización falló, se reintentará");
            return Result.retry();
        }
    }
}
