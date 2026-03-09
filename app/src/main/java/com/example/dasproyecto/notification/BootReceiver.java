package com.example.dasproyecto.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.dasproyecto.MainActivity;

/**
 * Receiver que se activa cuando el dispositivo se reinicia.
 * Si las notificaciones están activadas, vuelve a programar la alarma diaria.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    /**
     * Se ejecuta al arrancar el dispositivo.
     * Comprueba si las notificaciones están activadas y, si es así, reprograma la
     * alarma.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);

            if (notificacionesActivadas) {
                Log.d(TAG, "Dispositivo reiniciado — reprogramando alarma de notificaciones");
                MainActivity.programarAlarmaDiaria(context);
            } else {
                Log.d(TAG, "Dispositivo reiniciado — notificaciones desactivadas, ignorando reprogramación");
            }
        }
    }
}
