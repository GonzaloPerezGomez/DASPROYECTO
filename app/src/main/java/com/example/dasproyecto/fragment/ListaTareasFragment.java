package com.example.dasproyecto.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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
    private boolean primeraSeleccionRealizada = false;

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
        setHasOptionsMenu(true);

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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        inflater.inflate(R.menu.menu_lista_tareas, menu);

        // Obtener el SearchView del menú
        MenuItem searchItem = menu.findItem(R.id.action_buscar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            // Listener para obtener el texto escrito
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    // Se llama en cada letra que escribe (búsqueda en tiempo real)
                    Cursor cursor = dbManager.getTareasFiltradas(newText);
                    adapter.updateCursor(cursor);
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    Cursor cursor = dbManager.getTareasFiltradas(query);
                    adapter.updateCursor(cursor);
                    return true;
                }
            });
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    public void cargarTareas() {
        cargarTareas("fecha");
    }

    public void cargarTareas(String orden) {
        Cursor cursor = null;
        if (orden.equals("fecha")) {
            cursor = dbManager.getTareas();
        } else if (orden.equals("prioridad")) {
            cursor = dbManager.getTareasByPrioridad();
        }

        if (adapter == null) {
            adapter = new TareasAdapter(requireContext(), cursor, listener);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateCursor(cursor);
        }

        // En landscape, seleccionar automáticamente la primera tarea al cargar
        if (!primeraSeleccionRealizada && listener != null
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (cursor != null && cursor.moveToFirst()) {
                long primeraTareaId = cursor.getLong(cursor.getColumnIndexOrThrow(DBmanager.COL_ID));
                Log.d(TAG, "Auto-seleccionando primera tarea con ID: " + primeraTareaId);
                listener.onTareaSeleccionada(primeraTareaId);
            }
            primeraSeleccionRealizada = true;
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_eliminar_completadas) {
            dbManager.deleteCompleted();
            cargarTareas();
            Toast.makeText(requireContext(), R.string.toast_completadas_eliminadas, Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.ordenar_fecha) {
            cargarTareas();
            return true;
        } else if (id == R.id.ordenar_prioridad) {
            cargarTareas("prioridad");
            return true;
        } else {
            return super.onOptionsItemSelected(item);
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
