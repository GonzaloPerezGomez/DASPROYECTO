package com.example.dasproyecto.services.background;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.dasproyecto.R;
import com.example.dasproyecto.data.db.DBmanager;
import com.example.dasproyecto.ui.activities.LoginActivity;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

/**
 * Servicio que maneja los mensajes FCM recibidos desde Firebase.
 *
 * Responsabilidades:
 * - Mostrar notificación cuando llega un mensaje con la app en foreground.
 * - Gestionar el nuevo token FCM si rota (onNewToken).
 */
public class MiFirebaseMessagingService extends FirebaseMessagingService {

    private static final String TAG = "FCMService";
    private static final String CHANNEL_ID = "fcm_nueva_version";
    private static final int NOTIFICACION_ID = 99;

    /**
     * Se ejecuta cuando llega un mensaje FCM mientras la app está en primer plano.
     * Si la app está en background, Android muestra la notificación automáticamente
     * usando los campos "notification" del payload.
     */
    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);
        Log.d(TAG, "Mensaje FCM recibido de: " + remoteMessage.getFrom());

        // Extraer título y cuerpo del payload (puede venir en "notification" o en "data")
        String titulo = "Nueva versión disponible";
        String cuerpo = "Actualiza la aplicación para disfrutar de las últimas mejoras.";

        if (remoteMessage.getNotification() != null) {
            if (remoteMessage.getNotification().getTitle() != null) {
                titulo = remoteMessage.getNotification().getTitle();
            }
            if (remoteMessage.getNotification().getBody() != null) {
                cuerpo = remoteMessage.getNotification().getBody();
            }
        }

        // Si hay datos adicionales en el payload "data"
        if (!remoteMessage.getData().isEmpty()) {
            Log.d(TAG, "Datos del mensaje: " + remoteMessage.getData());
            if (remoteMessage.getData().containsKey("titulo")) {
                titulo = remoteMessage.getData().get("titulo");
            }
            if (remoteMessage.getData().containsKey("cuerpo")) {
                cuerpo = remoteMessage.getData().get("cuerpo");
            }
        }

        mostrarNotificacion(titulo, cuerpo);
    }

    /**
     * Se ejecuta cuando Firebase genera o rota el token FCM del dispositivo.
     * En nuestra arquitectura usamos Topics (no tokens individuales),
     * por lo que no necesitamos reenviar el token al servidor, pero lo logueamos.
     */
    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        Log.d(TAG, "Nuevo token FCM recibido: " + token);

        // Si el usuario ya está logueado, sincronizamos el token con el servidor
        SharedPreferences prefs =
                androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("session_user_id", -1);

        if (userId != -1) {
            DBmanager db = new DBmanager(this);
            db.actualizarFCMTokenRemoto(userId, token);
        }
    }

    /**
     * Muestra la notificación en la barra de estado del sistema.
     */
    private void mostrarNotificacion(String titulo, String cuerpo) {
        crearCanalSiNecesario();

        // Al pulsar la notificación → abre LoginActivity (que decide si ir a Main)
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(titulo)
                .setContentText(cuerpo)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(cuerpo))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManager manager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(NOTIFICACION_ID, builder.build());
        }
    }

    /**
     * Crea el canal de notificaciones (obligatorio en Android 8+).
     */
    private void crearCanalSiNecesario() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nueva versión",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Avisos de nuevas versiones de la aplicación");
            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }
}
