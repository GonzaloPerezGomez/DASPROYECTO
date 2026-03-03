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

public class AjustesFragment extends PreferenceFragmentCompat
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        addPreferencesFromResource(R.xml.ajustes);
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, @Nullable String key) {
        if ("idioma".equals(key) || "tema".equals(key) || "color_secundario".equals(key)) {
            // recreate() hace que attachBaseContext re-aplique configuraciones
            requireActivity().recreate();
        } else if ("notificaciones".equals(key)) {
            boolean estado = sharedPreferences.getBoolean(key, true);
            if (estado) {
                // habilitar notificaciones
                if (requireActivity() instanceof BaseActivity) {
                    ((BaseActivity) requireActivity()).solicitarPermisosNotificaciones();
                }
            } else {
                // deshabilitar notificaciones: cancelar alarma existente
                cancelarAlarma();
            }
        }
    }

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
