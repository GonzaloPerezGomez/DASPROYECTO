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

public class EliminarTareasCompletadasDialog extends DialogFragment {

    private static final String ARG_ID = "id_tarea";
    private static final String ARG_TITULO = "titulo_tarea";
    private DetalleTareaFragment.OnTareaEliminadaListener listener;

    public static EliminarTareasCompletadasDialog newInstance(
            DetalleTareaFragment.OnTareaEliminadaListener listener) {
        EliminarTareasCompletadasDialog frag = new EliminarTareasCompletadasDialog();
        frag.listener = listener;
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_eliminar_completadas_titulo));
        builder.setMessage(getString(R.string.dialog_eliminar_completadas_mensaje));
        builder.setPositiveButton(getString(R.string.dialog_eliminar_confirmar), (dialog, i) -> {
            DBmanager dbManager = new DBmanager(getActivity());
            dbManager.open();
            dbManager.eliminarCompletadas();
            dbManager.close();
            listener.onTareaEliminada();
            Toast.makeText(getActivity(), getString(R.string.toast_tareas_completadas_eliminadas), Toast.LENGTH_SHORT)
                    .show();
        });
        builder.setNegativeButton(getString(R.string.dialog_eliminar_cancelar), (dialog, i) -> dialog.dismiss());
        return builder.create();
    }
}
