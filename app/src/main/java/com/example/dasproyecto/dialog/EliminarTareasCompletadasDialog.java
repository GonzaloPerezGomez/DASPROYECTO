package com.example.dasproyecto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import com.example.dasproyecto.R;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.DetalleTareaFragment;

/**
 * Diálogo que pide confirmación antes de borrar todas las tareas completadas.
 */
public class EliminarTareasCompletadasDialog extends DialogFragment {

    private static final String ARG_ID = "id_tarea";
    private static final String ARG_TITULO = "titulo_tarea";
    private DetalleTareaFragment.OnTareaEliminadaListener listener;

    /**
     * Crea una instancia del diálogo con el listener para avisar cuando se borren.
     */
    public static EliminarTareasCompletadasDialog newInstance(
            DetalleTareaFragment.OnTareaEliminadaListener listener) {
        EliminarTareasCompletadasDialog frag = new EliminarTareasCompletadasDialog();
        frag.listener = listener;
        return frag;
    }

    /**
     * Monta el AlertDialog.
     * Si el usuario confirma, borra las completadas de la BD y muestra un Toast.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_eliminar_completadas_titulo));
        builder.setMessage(getString(R.string.dialog_eliminar_completadas_mensaje));
        builder.setPositiveButton(getString(R.string.dialog_eliminar_confirmar), (dialog, i) -> {
            androidx.fragment.app.FragmentActivity activityActivity = getActivity();
            if (activityActivity == null) return;
            
            DBmanager dbManager = new DBmanager(activityActivity);
            dbManager.open();
            
            new Thread(() -> {
                int eliminadas = dbManager.eliminarCompletadasProvider();
                if (activityActivity != null) {
                    activityActivity.runOnUiThread(() -> {
                        dbManager.close();
                        if (eliminadas >= 0) {
                            if (listener != null) listener.onTareaEliminada();
                            android.widget.Toast.makeText(activityActivity, activityActivity.getString(R.string.toast_tareas_completadas_eliminadas), android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(activityActivity, "Error al borrar tareas completadas", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });
        builder.setNegativeButton(getString(R.string.dialog_eliminar_cancelar), (dialog, i) -> dialog.dismiss());
        return builder.create();
    }
}
