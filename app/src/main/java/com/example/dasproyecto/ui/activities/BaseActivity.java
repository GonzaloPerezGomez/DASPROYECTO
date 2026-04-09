package com.example.dasproyecto.ui.activities;

import static android.content.ContentValues.TAG;
import static com.example.dasproyecto.ui.activities.MainActivity.programarAlarmaDiaria;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.view.MenuItem;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.widget.Toast;

import com.example.dasproyecto.utils.Idioma;
import com.example.dasproyecto.R;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

/**
 * Clase base de la que heredan todas las actividades.
 * Se encarga de aplicar el idioma, el tema (claro/oscuro),
 * el color secundario y de pedir permiso de notificaciones.
 */
public class BaseActivity extends AppCompatActivity {

    private String idiomaActual;
    private String colorActual;
    private String temaActual;
    private static final int NOTIFICACION_CODE = 0;
    private static final int CALENDARIO_CODE = 101;

    /**
     * Envuelve el contexto con el idioma que haya elegido el usuario.
     *
     * @param newBase Contexto original.
     */
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(Idioma.wrap(newBase));
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(newBase);
        idiomaActual = prefs.getString("idioma", "es");
    }

    /**
     * Se ejecuta al crear la actividad.
     * Aplica el tema y el color secundario según las preferencias del usuario.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
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

    /**
     * Al volver a la pantalla, comprueba si cambió el idioma, color o tema.
     * Si algo cambió, recrea la actividad para que se vean los nuevos ajustes.
     */
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

    /**
     * Pide al usuario permiso para enviar notificaciones.
     * Necesario a partir de Android 13 (TIRAMISU).
     */
    public void solicitarPermisosNotificaciones() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.POST_NOTIFICATIONS }, NOTIFICACION_CODE);
        }
    }

    /**
     * Pide al usuario permiso para acceder al calendario.
     */
    public void solicitarPermisosCalendario() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR }, CALENDARIO_CODE);
        }
    }

    /**
     * Comprueba que los servicios de Google Play estén instalados y actualizados.
     * Si no, levanta un diálogo permitiendo al usuario solucionarlo.
     */
    protected boolean comprobarPlayServices() {
        GoogleApiAvailability api = GoogleApiAvailability.getInstance();
        int code = api.isGooglePlayServicesAvailable(this);
        if (code == ConnectionResult.SUCCESS) {
            return true;
        } else {
            if (api.isUserResolvableError(code)) {
                api.getErrorDialog(this, code, 58).show();
            } else {
                Toast.makeText(this, "Este dispositivo no soporta Google Play Services", Toast.LENGTH_SHORT).show();
            }
            return false;
        }
    }

    /**
     * Recibe la respuesta del usuario al pedir permisos.
     * Si acepta, crea el canal de notificaciones y programa la alarma diaria.
     * Si deniega, desactiva las notificaciones en los ajustes.
     *
     * @param requestCode  Código de la petición.
     * @param permissions  Permisos solicitados.
     * @param grantResults Resultado de cada permiso (concedido o denegado).
     */
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

                }
                break;
            }
            case CALENDARIO_CODE: {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso de calendario concedido");
                    prefs.edit().putBoolean("sync_google_calendar", true).apply();
                } else {
                    Log.d(TAG, "Permiso de calendario denegado");
                    prefs.edit().putBoolean("sync_google_calendar", false).apply();
                    Toast.makeText(this, R.string.permiso_calendario_denegado, Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }
    }

    /**
     * Gestiona los clics en la Toolbar.
     * Si se pulsa el botón de "atrás", vuelve a la pantalla anterior.
     *
     * @param item Elemento del menú pulsado.
     * @return true si se procesó el evento, false si no.
     */
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
