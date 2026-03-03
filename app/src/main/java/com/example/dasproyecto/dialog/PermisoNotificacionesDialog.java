package com.example.dasproyecto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.R;

public class PermisoNotificacionesDialog extends DialogFragment {

    public static final String TAG = "PermisoNotificacionesDialog";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle(R.string.dialog_permiso_titulo)
                .setMessage(R.string.dialog_permiso_mensaje)
                .setPositiveButton(R.string.dialog_permiso_ajustes, (dialog, id) -> {
                    abrirAjustesApp();
                })
                .setNegativeButton(R.string.dialog_permiso_cancelar, (dialog, id) -> {
                    desactivarSwitchPrefs();
                });
        return builder.create();
    }

    private void abrirAjustesApp() {
        Context context = requireContext();
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", context.getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private void desactivarSwitchPrefs() {
        Context context = requireContext();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        prefs.edit().putBoolean("notificaciones", false).apply();
    }
}
