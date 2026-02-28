package com.example.dasproyecto.fragment;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dasproyecto.AddTareaActivity;
import com.example.dasproyecto.MainActivity;
import com.example.dasproyecto.R;
import com.example.dasproyecto.TareasAdapter;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.dialog.EliminarTareaDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListaTareasFragment extends Fragment {

    private static final String TAG = "ListaTareasFragment";
    private RecyclerView recyclerView;
    private DBmanager dbManager;
    private TareasAdapter adapter;

    public interface OnTareaSeleccionadaListener {
        void onTareaSeleccionada(long id);
    }

    private OnTareaSeleccionadaListener listener;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof OnTareaSeleccionadaListener) {
            listener = (OnTareaSeleccionadaListener) context;
        } else {
            throw new ClassCastException(
                    "La clase " + context.toString() + " debe implementar OnTareaSeleccionadaListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_tarea, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);

        // Configurar Drawer desde la Activity
        if (requireActivity() instanceof MainActivity) {
            ((MainActivity) requireActivity()).setupDrawerToggle(toolbar);
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        // FAB
        FloatingActionButton fab = view.findViewById(R.id.fabAddTarea);
        fab.setOnClickListener(v -> {
            Log.d(TAG, "Botón flotante presionado - Abriendo AddTareaActivity");
            Intent intent = new Intent(requireContext(), AddTareaActivity.class);
            startActivity(intent);
        });

        // RecyclerView
        recyclerView = view.findViewById(R.id.recyclerViewTareas);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbManager = new DBmanager(requireContext());
        dbManager.open();

        cargarTareas();
    }

    public void cargarTareas() {
        Cursor cursor = dbManager.getTareas();
        if (adapter == null) {
            adapter = new TareasAdapter(requireContext(), cursor, listener);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateCursor(cursor);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (dbManager != null && adapter != null) {
            cargarTareas();
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
