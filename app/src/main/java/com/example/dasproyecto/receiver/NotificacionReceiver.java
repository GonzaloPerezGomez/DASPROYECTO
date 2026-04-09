package com.example.dasproyecto.receiver;

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
import com.example.dasproyecto.data.db.AppDatabase;
import com.example.dasproyecto.data.db.TareaDao;
import com.example.dasproyecto.data.db.TareaEntity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Receiver que se dispara con la alarma diaria.
 * Mira si hay tareas pendientes y, si las hay, muestra una notificación.
 * Corregido para realizar el acceso a la BD en un hilo secundario mediante goAsync().
 */
public class NotificacionReceiver extends BroadcastReceiver {

    private static final String TAG = "NotificacionReceiver";
    private static final int NOTIFICACION_TAREAS_ID = 1;
    private static final String CHANNEL_ID = "tareas_channel";

    // Executor para realizar el trabajo de BD fuera del hilo principal
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    /**
     * Se ejecuta cuando salta la alarma.
     * Usa goAsync para poder consultar la base de datos sin bloquear el hilo principal
     * y evitar el IllegalStateException de Room.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "onReceive ejecutado - Iniciando procesamiento asíncrono...");

        final PendingResult pendingResult = goAsync();

        executor.execute(() -> {
            try {
                procesarNotificacion(context);
            } catch (Exception e) {
                Log.e(TAG, "Error al procesar la notificación en segundo plano", e);
            } finally {
                // Es crítico llamar a finish() para que el sistema sepa que el receiver ha terminado
                pendingResult.finish();
                Log.d(TAG, "Procesamiento de notificación finalizado.");
            }
        });
    }

    private void procesarNotificacion(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        boolean notificacionesActivadas = prefs.getBoolean("notificaciones", true);
        if (!notificacionesActivadas) {
            Log.d(TAG, "Notificaciones desactivadas en Ajustes. Abortando.");
            return;
        }

        crearCanalSiNecesario(context);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String fechaHoyDB = sdf.format(Calendar.getInstance().getTime());

        SharedPreferences prefsUser = PreferenceManager.getDefaultSharedPreferences(context);
        int userId = prefsUser.getInt("session_user_id", -1);
        if (userId == -1) {
            Log.w(TAG, "No hay usuario logueado, abortando notificación");
            return;
        }

        // Acceso a Room (Ahora seguro porque estamos en el hilo del executor)
        TareaDao dao =
                AppDatabase.getInstance(context).tareaDao();
        java.util.List<TareaEntity> todasTareas = dao.getTareasPorFecha(userId);

        ArrayList<String> tareas = new ArrayList<>();
        for (TareaEntity t : todasTareas) {
            if (t.completada == 0 && t.fechaLimite != null && !t.fechaLimite.isEmpty()) {
                if (t.fechaLimite.compareTo(fechaHoyDB) <= 0) {
                    tareas.add(t.titulo);
                }
            }
        }

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager == null) return;

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
            Log.d(TAG, "No hay tareas pendientes para hoy");
        }
    }

    /**
     * Crea el canal de notificaciones si estamos en Android 8 (Oreo) o superior.
     */
    @SuppressLint("ObsoleteSdkInt")
    private void crearCanalSiNecesario(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.notif_canal_nombre),
                    NotificationManager.IMPORTANCE_DEFAULT);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
