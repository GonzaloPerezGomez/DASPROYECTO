package com.example.dasproyecto.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dasproyecto.AddTareaActivity;
import com.example.dasproyecto.MainActivity;
import com.example.dasproyecto.R;
import com.example.dasproyecto.TareasAdapter;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.dialog.EliminarTareasCompletadasDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class ListaTareasFragment extends Fragment {

    private static final String TAG = "ListaTareasFragment";
    private RecyclerView recyclerView;
    private DBmanager dbManager;
    private TareasAdapter adapter;
    private boolean primeraSeleccionRealizada = false;

    private DetalleTareaFragment.OnTareaEliminadaListener listenerEliminada;

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

        if (context instanceof DetalleTareaFragment.OnTareaEliminadaListener) {
            listenerEliminada = (DetalleTareaFragment.OnTareaEliminadaListener) context;
        } else {
            throw new ClassCastException(
                    "La clase " + context.toString() + " debe implementar OnTareaEliminadaListener");
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

        FloatingActionButton fab = view.findViewById(R.id.fabAddTarea);
        fab.setOnClickListener(v -> {
            Log.d(TAG, "Botón flotante presionado - Abriendo AddTareaActivity");
            Intent intent = new Intent(requireContext(), AddTareaActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(fab, (v, insets) -> {
            Insets navBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            params.bottomMargin = navBars.bottom + (int) (36 * getResources().getDisplayMetrics().density);
            v.setLayoutParams(params);
            return insets;
        });

        recyclerView = view.findViewById(R.id.recyclerViewTareas);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        dbManager = new DBmanager(requireContext());
        dbManager.open();

        cargarTareas();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_lista_tareas, menu);

        MenuItem searchItem = menu.findItem(R.id.action_buscar);
        SearchView searchView = (SearchView) searchItem.getActionView();
        if (searchView != null) {
            searchView.setQueryHint(getString(R.string.search_hint));
            searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextChange(String newText) {
                    if (dbManager != null && adapter != null) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        boolean ocultar = prefs.getBoolean("ocultar_completadas", false);
                        Cursor cursor = dbManager.getTareasFiltradas(newText, ocultar);
                        adapter.updateCursor(cursor);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (dbManager != null && adapter != null) {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
                        boolean ocultar = prefs.getBoolean("ocultar_completadas", false);
                        Cursor cursor = dbManager.getTareasFiltradas(query, ocultar);
                        adapter.updateCursor(cursor);
                    }
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_eliminar_completadas) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean confirmarEliminar = prefs.getBoolean("confirmar_eliminar", true);
            if (confirmarEliminar) {
                EliminarTareasCompletadasDialog dialogo = EliminarTareasCompletadasDialog
                        .newInstance(listenerEliminada);
                dialogo.show(getParentFragmentManager(), "EliminarCompletadas");
            } else {
                listenerEliminada.onTareaEliminada();
                dbManager.eliminarCompletadas();
                cargarTareas();
                Toast.makeText(requireContext(), R.string.toast_completadas_eliminadas, Toast.LENGTH_SHORT).show();
            }
            return true;
        } else if (id == R.id.ordenar_fecha) {
            cargarTareas("fecha");
            return true;
        } else if (id == R.id.ordenar_prioridad) {
            cargarTareas("prioridad");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void cargarTareas() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String ordenDefecto = prefs.getString("orden_defecto", "fecha");
        cargarTareas(ordenDefecto);
    }

    public void cargarTareas(String orden) {
        if (dbManager == null || recyclerView == null) {
            Log.w(TAG, "cargarTareas: dbManager o recyclerView no inicializados");
            return;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean ocultar = prefs.getBoolean("ocultar_completadas", false);

        Cursor cursor;
        try {
            if ("prioridad".equals(orden)) {
                cursor = dbManager.getTareasByPrioridad(ocultar);
            } else {
                cursor = dbManager.getTareas(ocultar);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al cargar tareas: " + e.getMessage(), e);
            return;
        }

        if (cursor == null) {
            Log.w(TAG, "cargarTareas: cursor es null");
            return;
        }

        if (adapter == null) {
            adapter = new TareasAdapter(requireContext(), cursor, listener);
            recyclerView.setAdapter(adapter);
        } else {
            adapter.updateCursor(cursor);
        }

        if (!primeraSeleccionRealizada && listener != null
                && getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (cursor.moveToFirst()) {
                long primeraTareaId = cursor.getLong(cursor.getColumnIndexOrThrow(DBmanager.COL_ID));
                Log.d(TAG, "Auto-seleccionando primera tarea con ID: " + primeraTareaId);
                listener.onTareaSeleccionada(primeraTareaId);
            }
            primeraSeleccionRealizada = true;
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
