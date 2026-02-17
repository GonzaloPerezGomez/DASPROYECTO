package com.example.dasproyecto.Dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import com.example.dasproyecto.db.DBmanager;

public class EliminarTareaDialog extends DialogFragment {

    // Interfaz para comunicar el resultado al Adaptador/Activity
    public interface ConfirmacionListener {
        void onTareaEliminada();
    }

    private long idTarea;
    private String tituloTarea;
    private ConfirmacionListener listener;

    // Constructor estático para pasar datos (Buenas prácticas Android)
    public static EliminarTareaDialog newInstance(long id, String titulo, ConfirmacionListener listener) {
        EliminarTareaDialog frag = new EliminarTareaDialog();
        frag.idTarea = id;
        frag.tituloTarea = titulo;
        frag.listener = listener;
        return frag;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle("Eliminar tarea")
                .setMessage("¿Estás seguro de que quieres eliminar la tarea '" + tituloTarea + "'?")
                .setPositiveButton("Eliminar", (dialog, which) -> {
                    // Mantenemos tu lógica de base de datos
                    DBmanager dbManager = new DBmanager(getContext());
                    dbManager.open();
                    dbManager.eliminar(idTarea);
                    dbManager.close();

                    // Avisamos que se ha eliminado para refrescar la interfaz
                    if (listener != null) {
                        listener.onTareaEliminada();
                    }
                })
                .setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss())
                .create();
    }
}
