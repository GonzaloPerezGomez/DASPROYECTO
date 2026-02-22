package com.example.dasproyecto;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.example.dasproyecto.db.DBmanager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

/**
 * BroadcastReceiver que se ejecuta cuando salta la alarma programada (cada día
 * a las 8:00).
 *
 * ¿Qué es un BroadcastReceiver?
 * - Es un componente de Android que "escucha" eventos (broadcasts).
 * - El sistema lo ejecuta automáticamente cuando llega el evento (en este caso,
 * la alarma).
 * - NO necesita que la app esté abierta: Android lo ejecuta incluso si la app
 * está cerrada.
 * - Su método onReceive() se ejecuta en el hilo principal, así que debe ser
 * rápido.
 *
 * Flujo:
 * 1. AlarmManager dispara la alarma a las 8:00 AM.
 * 2. Android ejecuta onReceive() de esta clase.
 * 3. onReceive() abre la BD, consulta tareas pendientes, y envía una
 * notificación.
 */
public class NotificacionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificacionReceiver";

    // ID de la notificación: al usar siempre el mismo, se reemplaza la anterior
    private static final int NOTIFICACION_TAREAS_ID = 1;

    // ID del canal de notificaciones (debe coincidir con el creado en MainActivity)
    private static final String CHANNEL_ID = "tareas_channel";

    /**
     * Método que Android ejecuta automáticamente cuando se recibe la alarma.
     *
     * @param context Contexto de la aplicación (permite acceder a servicios del
     *                sistema, BD, etc.)
     * @param intent  Intent que contiene información sobre el broadcast (no lo
     *                usamos aquí)
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "Alarma recibida — comprobando tareas pendientes");

        // --- Paso 1: Asegurar que el canal de notificaciones existe ---
        // Esto es necesario porque el BroadcastReceiver puede ejecutarse sin que
        // MainActivity se haya abierto nunca (ej: tras un reinicio del teléfono).
        crearCanalSiNecesario(context);

        // --- Paso 2: Obtener la fecha de hoy ---
        // Usamos el mismo formato "d/M/yyyy" que la app usa al guardar tareas.
        SimpleDateFormat sdf = new SimpleDateFormat("d/M/yyyy", Locale.getDefault());
        String fechaHoy = sdf.format(Calendar.getInstance().getTime());
        Log.d(TAG, "Fecha de hoy: " + fechaHoy);

        // --- Paso 3: Abrir la base de datos y consultar tareas pendientes ---
        // Creamos una instancia nueva de DBmanager porque el BroadcastReceiver
        // no tiene acceso al dbManager de MainActivity (puede que ni exista).
        DBmanager dbManager = new DBmanager(context);
        dbManager.open();
        ArrayList<String> tareas = dbManager.tareasPendientes(fechaHoy);
        dbManager.close(); // Cerramos inmediatamente para no dejar conexiones abiertas

        // --- Paso 4: Si hubo un error al parsear la fecha, salimos ---
        if (tareas == null) {
            Log.e(TAG, "Error al obtener las tareas pendientes");
            return;
        }

        // --- Paso 5: Obtener el NotificationManager del sistema ---
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // --- Paso 6: Si hay tareas pendientes, enviar notificación ---
        if (!tareas.isEmpty()) {
            // Construir el mensaje con la lista de tareas
            StringBuilder mensaje = new StringBuilder("Tienes ");
            mensaje.append(tareas.size()).append(" tarea(s) pendiente(s)");
            for (String tarea : tareas) {
                mensaje.append("\n• ").append(tarea);
            }

            // Construir la notificación
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle("Tareas pendientes")
                    .setContentText(mensaje.toString())
                    // BigTextStyle permite expandir el texto para ver todas las tareas
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(mensaje.toString()))
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    // La notificación desaparece al pulsarla
                    .setAutoCancel(true);

            // Verificar que tenemos permiso (obligatorio en Android 13+)
            if (ActivityCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
                manager.notify(NOTIFICACION_TAREAS_ID, builder.build());
                Log.d(TAG, "Notificación enviada: " + mensaje);
            } else {
                Log.w(TAG, "Permiso POST_NOTIFICATIONS no concedido");
            }
        } else {
            // Si no hay tareas, cancelar cualquier notificación previa
            manager.cancel(NOTIFICACION_TAREAS_ID);
            Log.d(TAG, "No hay tareas pendientes, notificación cancelada");
        }
    }

    /**
     * Crea el canal de notificaciones si el dispositivo es Android 8 (Oreo) o
     * superior.
     * Si el canal ya existe, Android ignora la llamada (no se duplica).
     */
    @SuppressLint("ObsoleteSdkInt")
    private void crearCanalSiNecesario(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Tareas",
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }
    }
}
