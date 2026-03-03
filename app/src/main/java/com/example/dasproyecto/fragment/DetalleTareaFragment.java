package com.example.dasproyecto.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.EditTareaActivity;
import com.example.dasproyecto.R;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.dialog.EliminarTareaDialog;

public class DetalleTareaFragment extends Fragment {

    private static final String TAG = "DetalleTareaFragment";
    private static final String ARG_TAREA_ID = "tarea_id";

    private TextView tvTitulo, tvDescripcion, tvFecha, tvPrioridad;
    private Button btnCompletar;
    private DBmanager dbManager;
    private long tareaId = -1;
    private int estadoCompletada = 0;

    private String[] prioridades;

    public interface OnTareaEliminadaListener {
        void onTareaEliminada();
    }

    private DetalleTareaFragment.OnTareaEliminadaListener listenerEliminada;

    public interface OnTareaCompletadaListener {
        void onTareaCompletada(long tareaId);
    }

    private DetalleTareaFragment.OnTareaCompletadaListener listenerCompletada;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof DetalleTareaFragment.OnTareaEliminadaListener) {
            listenerEliminada = (DetalleTareaFragment.OnTareaEliminadaListener) context;
        } else {
            throw new ClassCastException(
                    "La clase " + context.toString() + " debe implementar OnTareaEliminadaListener");
        }
        if (context instanceof DetalleTareaFragment.OnTareaCompletadaListener) {
            listenerCompletada = (DetalleTareaFragment.OnTareaCompletadaListener) context;
        } else {
            throw new ClassCastException(
                    "La clase " + context.toString() + " debe implementar OnTareaCompletadaListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_tarea, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        prioridades = getResources().getStringArray(R.array.prioridades_array);

        tvTitulo = view.findViewById(R.id.tvTitulo);
        tvDescripcion = view.findViewById(R.id.tvDescripcion);
        tvFecha = view.findViewById(R.id.tvFecha);
        tvPrioridad = view.findViewById(R.id.tvPrioridad);
        btnCompletar = view.findViewById(R.id.btnCompletar);

        if (getArguments() != null) {
            tareaId = getArguments().getLong(ARG_TAREA_ID, -1);
        }

        if (tareaId == -1) {
            Toast.makeText(requireContext(), R.string.error_tarea_no_encontrada, Toast.LENGTH_SHORT).show();
            requireActivity().onBackPressed();
            return;
        }

        dbManager = new DBmanager(requireContext());
        dbManager.open();

        cargarDatos();

        btnCompletar.setOnClickListener(v -> {
            estadoCompletada = (estadoCompletada == 0) ? 1 : 0;
            dbManager.actualizarEstado(tareaId, estadoCompletada);
            listenerCompletada.onTareaCompletada(tareaId);
            actualizarBotonCompletar();
        });
    }

    private void actualizarBotonCompletar() {
        btnCompletar.setText(estadoCompletada == 0
                ? R.string.btn_completada
                : R.string.btn_no_completada);
    }

    private void cargarDatos() {
        Cursor cursor = dbManager.getTarea(tareaId);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Log.d(TAG, "Tarea encontrada: " + cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
                tvTitulo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
                tvDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION)));
                String fechaBD = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE));
                tvFecha.setText(DBmanager.formatFechaToUI(fechaBD));

                estadoCompletada = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_COMPLETADA));

                int prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD));
                tvPrioridad.setText(prioridades[prioridad]);
            }
            cursor.close();
        }
        actualizarBotonCompletar();
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_editar) {
            Intent intent = new Intent(requireContext(), EditTareaActivity.class);
            intent.putExtra(DBmanager.COL_ID, tareaId);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_eliminar) {
            String titulo = tvTitulo.getText().toString();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean confirmarEliminar = prefs.getBoolean("confirmar_eliminar", true);
            if (confirmarEliminar) {
                EliminarTareaDialog dialogo = EliminarTareaDialog.newInstance(tareaId, titulo, listenerEliminada);
                dialogo.show(getParentFragmentManager(), "EliminarTareaDialog");
                return true;
            } else {
                dbManager.eliminar(tareaId);
                listenerEliminada.onTareaEliminada();
                Toast.makeText(getActivity(), getString(R.string.toast_tarea_eliminada, titulo), Toast.LENGTH_SHORT)
                        .show();
            }
        }

        return false;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbManager != null && tareaId != -1) {
            cargarDatos();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
