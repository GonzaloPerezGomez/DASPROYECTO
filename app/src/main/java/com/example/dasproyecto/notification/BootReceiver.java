package com.example.dasproyecto.notification;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.dasproyecto.MainActivity;

/**
 * BroadcastReceiver que se ejecuta automáticamente cuando el dispositivo se
 * reinicia.
 *
 * ¿Por qué es necesario?
 * - Las alarmas programadas con AlarmManager se PIERDEN cuando el teléfono se
 * apaga o reinicia.
 * - Este receiver escucha el evento BOOT_COMPLETED (el teléfono acaba de
 * arrancar).
 * - Cuando lo recibe, vuelve a programar la alarma diaria de las 8:00 AM.
 *
 * Requisitos:
 * - Permiso RECEIVE_BOOT_COMPLETED en el AndroidManifest.
 * - Registrar este receiver en el AndroidManifest con el intent-filter
 * BOOT_COMPLETED.
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        // Verificar que el evento es realmente un BOOT_COMPLETED
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
