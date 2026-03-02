package com.example.dasproyecto;

import static android.content.ContentValues.TAG;
import static com.example.dasproyecto.MainActivity.programarAlarmaDiaria;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.dialog.PermisoNotificacionesDialog;

/**
 * Activity base que aplica el locale de las preferencias.
 * Todas las Activities de la app deben extender esta clase.
 *
 * - attachBaseContext: aplica el locale al crear la Activity.
 * - onResume: si el usuario cambió el idioma en Ajustes, recrea la Activity.
 */
public class BaseActivity extends AppCompatActivity {

    private String idiomaActual;
    private static final int NOTIFICACION_CODE = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Idioma.wrap(newBase));
        // Guardar el idioma con el que se creó esta Activity
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        idiomaActual = prefs.getString("idioma", "es");
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si el idioma cambió mientras estábamos en segundo plano, recrear
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idiomaPrefs = prefs.getString("idioma", "es");
        if (!idiomaPrefs.equals(idiomaActual)) {
            idiomaActual = idiomaPrefs;
            recreate();
        }
    }

    public void solicitarPermisosNotificaciones() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.POST_NOTIFICATIONS }, NOTIFICACION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case NOTIFICACION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso concedido");

                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                "tareas_channel",
                                getString(R.string.notif_canal_nombre),
                                NotificationManager.IMPORTANCE_DEFAULT);
                        manager.createNotificationChannel(channel);
                    }
                    programarAlarmaDiaria(this);

                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putBoolean("notificaciones", true).apply();

                } else if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_DENIED) {
                    Log.d(TAG, "Permiso denegado");
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    prefs.edit().putBoolean("notificaciones", false).apply();

                    // Si el usuario deniega y ya no se le debe mostrar el diálogo nativo (porque lo
                    // rechazó previamente)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            !shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                        Log.d(TAG, "Permiso bloqueado permanentemente. Mostrando diálogo.");
                        new PermisoNotificacionesDialog().show(getSupportFragmentManager(),
                                PermisoNotificacionesDialog.TAG);
                    }
                }
            }
        }
    }
}
