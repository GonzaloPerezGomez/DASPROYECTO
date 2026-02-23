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
import com.example.dasproyecto.MainActivity;

public class EliminarTareaDialog extends DialogFragment {

    public static final String RESULT_KEY = "tareaEliminada";
    private static final String ARG_ID = "id_tarea";
    private static final String ARG_TITULO = "titulo_tarea";

    public static EliminarTareaDialog newInstance(long id, String titulo) {
        EliminarTareaDialog frag = new EliminarTareaDialog();
        Bundle args = new Bundle();
        args.putLong(ARG_ID, id);
        args.putString(ARG_TITULO, titulo);
        frag.setArguments(args);
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        long idTarea = getArguments() != null ? getArguments().getLong(ARG_ID) : -1;
        String tituloTarea = getArguments() != null ? getArguments().getString(ARG_TITULO) : "";

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getString(R.string.dialog_eliminar_titulo));
        builder.setMessage(getString(R.string.dialog_eliminar_mensaje, tituloTarea));
        builder.setPositiveButton(getString(R.string.dialog_eliminar_confirmar), (dialog, i) -> {
            DBmanager dbManager = new DBmanager(getActivity());
            dbManager.open();
            dbManager.eliminar(idTarea);
            dbManager.close();

            Toast.makeText(getActivity(), getString(R.string.toast_tarea_eliminada, tituloTarea), Toast.LENGTH_SHORT)
                    .show();

            // Notificar a la actividad mediante FragmentResult
            getParentFragmentManager().setFragmentResult(RESULT_KEY, new Bundle());

            // Refrescar lista si estamos en MainActivity
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshTareas();
            }
        });
        builder.setNegativeButton(getString(R.string.dialog_eliminar_cancelar), (dialog, i) -> dialog.dismiss());
        return builder.create();
    }
}
