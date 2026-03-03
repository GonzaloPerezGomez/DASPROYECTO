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

public class BaseActivity extends AppCompatActivity {

    private String idiomaActual;
    private String colorActual;
    private String temaActual;
    private static final int NOTIFICACION_CODE = 0;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Idioma.wrap(newBase));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        idiomaActual = prefs.getString("idioma", "es");
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        temaActual = prefs.getString("tema", "claro");
        if ("oscuro".equals(temaActual)) {
            androidx.appcompat.app.AppCompatDelegate
                    .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate
                    .setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        colorActual = prefs.getString("color_secundario", "azul");
        switch (colorActual) {
            case "rojo":
                setTheme(R.style.Theme_DASPROYECTO_Rojo);
                break;
            case "verde":
                setTheme(R.style.Theme_DASPROYECTO_Verde);
                break;
            case "naranja":
                setTheme(R.style.Theme_DASPROYECTO_Naranja);
                break;
            case "morado":
                setTheme(R.style.Theme_DASPROYECTO_Morado);
                break;
            case "azul":
            default:
                setTheme(R.style.Theme_DASPROYECTO);
                break;
        }

        super.onCreate(savedInstanceState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String idiomaPrefs = prefs.getString("idioma", "es");
        String colorPrefs = prefs.getString("color_secundario", "azul");
        String temaPrefs = prefs.getString("tema", "claro");

        boolean needsRecreate = false;
        if (!idiomaPrefs.equals(idiomaActual)) {
            idiomaActual = idiomaPrefs;
            needsRecreate = true;
        }
        if (!colorPrefs.equals(colorActual)) {
            colorActual = colorPrefs;
            needsRecreate = true;
        }
        if (!temaPrefs.equals(temaActual)) {
            temaActual = temaPrefs;
            needsRecreate = true;
        }

        if (needsRecreate) {
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
