package com.example.dasproyecto.dialog;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.MainActivity;

public class EliminarTareaDialog extends DialogFragment {

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
        builder.setTitle("Eliminar tarea");
        builder.setMessage("¿Estás seguro de que quieres eliminar la tarea '" + tituloTarea + "'?");
        builder.setPositiveButton("Sí", (dialog, i) -> {
            DBmanager dbManager = new DBmanager(getActivity());
            dbManager.open();
            dbManager.eliminar(idTarea);
            dbManager.close();
            
            Toast.makeText(getActivity(), "Tarea '" + tituloTarea + "' eliminada correctamente", Toast.LENGTH_SHORT).show();
            
            // Notificar a la actividad para refrescar la lista
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).refreshTareas();
            }
        });
        builder.setNegativeButton("No", (dialog, i) -> dialog.dismiss());
        return builder.create();
    }
}
