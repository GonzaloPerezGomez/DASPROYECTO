package com.example.dasproyecto.fragment;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.preference.PreferenceFragmentCompat;

import com.example.dasproyecto.BaseActivity;
import com.example.dasproyecto.R;
import com.example.dasproyecto.notification.NotificacionReceiver;

/**
 * Fragmento que muestra las opciones de configuración de la app
 * (idioma, tema, color, notificaciones) cargadas desde el XML de ajustes.
 */
public class AjustesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    /**
     * Carga las preferencias desde el archivo XML.
     */
    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.ajustes);

        androidx.preference.Preference prefCerrarSesion = findPreference("cerrar_sesion");
        if (prefCerrarSesion != null) {
            prefCerrarSesion.setOnPreferenceClickListener(preference -> {
                cerrarSesion();
                return true;
            });
        }
    }

    private void cerrarSesion() {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle(R.string.config_cerrar_sesion)
                .setMessage(R.string.config_cerrar_sesion_confirmar)
                .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                    getPreferenceManager().getSharedPreferences().edit()
                            .remove("session_user_id")
                            .apply();
                    Intent i = new Intent(requireActivity(), com.example.dasproyecto.LoginActivity.class);
                    i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(i);
                })
                .setNegativeButton(android.R.string.no, null)
                .show();
    }

    /**
     * Al volver a primer plano, se registra para escuchar cambios en los ajustes.
     */
    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    /**
     * Al pausarse, deja de escuchar cambios para no gastar recursos.
     */
    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    /**
     * Cuando el usuario cambia un ajuste, reacciona según lo que sea:
     * idioma/tema/color → recrea la actividad; notificaciones → pide permisos o
     * cancela la alarma.
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("idioma".equals(key) || "tema".equals(key) || "color_secundario".equals(key)) {
            requireActivity().recreate();
        } else if ("notificaciones".equals(key)) {
            boolean estado = sharedPreferences.getBoolean(key, true);
            if (estado) {
                if (requireActivity() instanceof BaseActivity) {
                    ((BaseActivity) requireActivity()).solicitarPermisosNotificaciones();
                }
            } else {
                cancelarAlarma();
            }
        }
    }

    /**
     * Cancela la alarma diaria de notificaciones.
     */
    private void cancelarAlarma() {
        Context context = requireContext();
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, NotificacionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}
