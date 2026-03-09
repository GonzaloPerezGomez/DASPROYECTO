package com.example.dasproyecto.notification;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.R;
import com.example.dasproyecto.db.DBmanager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * Receiver que se dispara con la alarma diaria.
 * Mira si hay tareas pendientes y, si las hay, muestra una notificación.
 */
public class NotificacionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificacionReceiver";
    private static final int NOTIFICACION_TAREAS_ID = 1;
    private static final String CHANNEL_ID = "tareas_channel";

    /**
     * Se ejecuta cuando salta la alarma.
     * Comprueba los permisos, busca tareas pendientes y lanza la notificación.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive ejecutado - Comprobando tareas pendientes...");

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);
        if (!notificacionesActivadas) {
            Log.d(TAG, "Notificaciones desactivadas en Ajustes. Abortando.");
            return;
        }

        crearCanalSiNecesario(context);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaHoyDB = sdf.format(Calendar.getInstance().getTime());
        Log.d(TAG, "Fecha de hoy: " + fechaHoyDB);

        DBmanager dbManager = new DBmanager(context);
        dbManager.open();
        ArrayList<String> tareas = dbManager.tareasPendientes(fechaHoyDB);
        dbManager.close();

        if (tareas == null) {
            Log.e(TAG, "Error al obtener las tareas pendientes");
            return;
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (!tareas.isEmpty()) {
            StringBuilder mensaje = new StringBuilder(context.getString(R.string.notif_mensaje, tareas.size()));
            for (String tarea : tareas) {
                mensaje.append("\n• ").append(tarea);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(context.getString(R.string.notif_titulo))
                    .setContentText(mensaje.toString())
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje.toString()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true);

            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                manager.notify(NOTIFICACION_TAREAS_ID, builder.build());
                Log.d(TAG, "Notificación enviada: " + mensaje);
            } else {
                Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido");
            }
        } else {
            manager.cancel(NOTIFICACION_TAREAS_ID);
            Log.d(TAG, "No hay tareas pendientes, notificación cancelada");
        }
    }

    /**
     * Crea el canal de notificaciones si estamos en Android 8 (Oreo) o superior.
     * Sin esto, las notificaciones no se muestran.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void crearCanalSiNecesario(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_canal_nombre),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
