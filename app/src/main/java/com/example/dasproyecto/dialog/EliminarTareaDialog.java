package com.example.dasproyecto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.example.dasproyecto.R;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.DetalleTareaFragment;
import androidx.fragment.app.FragmentActivity;

/**
 * Diálogo de confirmación para borrar una tarea.
 * Pregunta al usuario si está seguro antes de eliminarla.
 */
public class EliminarTareaDialog extends DialogFragment {

    private static final String ARG_ID = "id_tarea";
    private static final String ARG_TITULO = "titulo_tarea";
    private DetalleTareaFragment.OnTareaEliminadaListener listener;

    /**
     * Crea una instancia del diálogo con el ID y título de la tarea a borrar.
     */
    public static EliminarTareaDialog newInstance(long id, String titulo,
            DetalleTareaFragment.OnTareaEliminadaListener listener) {
        EliminarTareaDialog frag = new EliminarTareaDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        args.putString(ARG_TITULO, titulo);
        frag.setArguments(args);
        frag.listener = listener;
        return frag;
    }

    /**
     * Monta el AlertDialog.
     * Si el usuario pulsa "Sí", borra la tarea de la BD y avisa al listener.
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        long idTarea = getArguments() != null ? getArguments().getLong(ARG_ID) : -1;
        String tituloTarea = getArguments() != null ? getArguments().getString(ARG_TITULO) : "";

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_eliminar_titulo));
        builder.setMessage(getString(R.string.dialog_eliminar_mensaje, tituloTarea));
        builder.setPositiveButton(getString(R.string.dialog_eliminar_confirmar), (dialog, i) -> {
            androidx.fragment.app.FragmentActivity activityActivity = getActivity();
            if (activityActivity == null) return;
            
            DBmanager dbManager = new DBmanager(activityActivity);
            dbManager.open();
            
            new Thread(() -> {
                boolean exito = dbManager.eliminarProvider(idTarea);
                if (activityActivity != null) {
                    activityActivity.runOnUiThread(() -> {
                        dbManager.close();
                        if (exito) {
                            if (listener != null) listener.onTareaEliminada();
                            android.widget.Toast.makeText(activityActivity, activityActivity.getString(R.string.toast_tarea_eliminada, tituloTarea), android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(activityActivity, "Error al borrar con ContentProvider", android.widget.Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }).start();
        });
        builder.setNegativeButton(getString(R.string.dialog_eliminar_cancelar), (dialog, i) -> dialog.dismiss());
        return builder.create();
    }
}
