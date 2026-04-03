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
import org.json.JSONArray;
import org.json.JSONObject;

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

/**
 * Fragmento principal que muestra la lista de tareas en un RecyclerView.
 * Permite filtrar, ordenar y añadir tareas nuevas.
 */
public class ListaTareasFragment extends Fragment {

    private static final String TAG = "ListaTareasFragment";
    private RecyclerView recyclerView;
    private DBmanager dbManager;
    private TareasAdapter adapter;
    private boolean primeraSeleccionRealizada = false;
    private String ordenDefectoActual = null;
    private String ordenActual = null;

    private DetalleTareaFragment.OnTareaEliminadaListener listenerEliminada;

    /**
     * Interfaz para avisar a la Activity cuando se pulsa una tarea.
     */
    public interface OnTareaSeleccionadaListener {
        void onTareaSeleccionada(long id);
    }

    private OnTareaSeleccionadaListener listener;

    /**
     * Al adjuntarse a la Activity, comprueba que implemente los listeners.
     */
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

    /**
     * Infla el layout del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_lista_tarea, container, false);
    }

    /**
     * Una vez creada la vista, prepara el RecyclerView, el botón flotante,
     * la toolbar y la conexión con la BD.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setHasOptionsMenu(true);

        Toolbar toolbar = view.findViewById(R.id.toolbar);
        ((AppCompatActivity) requireActivity()).setSupportActionBar(toolbar);
        ((AppCompatActivity) requireActivity()).getSupportActionBar().setTitle(R.string.toolbar_title);

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

        cargarTareas();
    }

    /**
     * Monta el menú con la barra de búsqueda y filtra tareas según lo que escriba
     * el usuario.
     */
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
                    if (adapter != null) {
                        adapter.filtrar(newText);
                    }
                    return true;
                }

                @Override
                public boolean onQueryTextSubmit(String query) {
                    if (adapter != null) {
                        adapter.filtrar(query);
                    }
                    return true;
                }
            });
        }
    }

    /**
     * Gestiona los clics del menú: ordenar por fecha/prioridad o borrar
     * completadas.
     */
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
                new Thread(() -> {
                    Log.d(TAG, "Eliminando tareas completadas usando provider...");
                    int deleted = dbManager.eliminarCompletadasProvider();
                    Log.d(TAG, "Tareas completadas eliminadas: " + deleted);
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            if (deleted > 0) {
                                if (listenerEliminada != null) listenerEliminada.onTareaEliminada();
                                Toast.makeText(requireContext(), R.string.toast_completadas_eliminadas, Toast.LENGTH_SHORT).show();
                                cargarTareas(ordenActual != null ? ordenActual : "fechaLimite");
                            } else {
                                Toast.makeText(requireContext(), "No había tareas completadas", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }).start();
            }
            return true;
        } else if (id == R.id.ordenar_fecha) {
            cargarTareas("fechaLimite");
            return true;
        } else if (id == R.id.ordenar_prioridad) {
            cargarTareas("prioridad");
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Carga las tareas usando el orden guardado en preferencias.
     */
    public void cargarTareas() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String ordenDefecto = prefs.getString("orden_defecto", "fechaLimite");
        this.ordenDefectoActual = ordenDefecto;
        cargarTareas(ordenDefecto);
    }

    /**
     * Carga las tareas de la BD con el orden indicado a través de
     * getTareasRemoto().
     */
    public void cargarTareas(String orden) {
        this.ordenActual = orden;
        Log.d(TAG, "cargarTareas() invocado. Orden: " + orden);

        if (dbManager == null || recyclerView == null) {
            Log.w(TAG, "cargarTareas: dbManager o recyclerView no inicializados");
            return;
        }

        // Usando DBmanager para cargar las tareas vía Content Provider (Hito 7)
        new Thread(() -> {
            JSONObject resultJson = dbManager.getTareasProvider(orden);
            if (resultJson != null && resultJson.optBoolean("exito", false)) {
                JSONArray tareasArray;
                try {
                    tareasArray = resultJson.getJSONArray("tareas");
                } catch (Exception e) {
                    Log.e(TAG, "Error extrayendo array de tareas de JSON", e);
                    tareasArray = new JSONArray();
                }

                JSONArray finalTareasArray = tareasArray;
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (!isAdded() || getActivity() == null) return;
                        
                        if (adapter == null) {
                            adapter = new TareasAdapter(requireContext(), listener);
                            recyclerView.setAdapter(adapter);
                        }
                        adapter.setTareas(finalTareasArray);

                        boolean orientationLandscape = getResources()
                                .getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
                        if (!primeraSeleccionRealizada && listener != null && orientationLandscape
                                && finalTareasArray.length() > 0) {
                            try {
                                long primeraTareaId = finalTareasArray.getJSONObject(0).getLong("id");
                                listener.onTareaSeleccionada(primeraTareaId);
                                primeraSeleccionRealizada = true;
                            } catch (Exception e) {
                                Log.e(TAG, "Error auto-seleccionando tarea", e);
                            }
                        }
                    });
                }
            } else {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        Toast.makeText(requireContext(), "Error obteniendo tareas del Provider", Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }

    /**
     * Al volver a esta pantalla, recarga las tareas por si hubo cambios.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (dbManager != null && adapter != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
            String defectoActual = prefs.getString("orden_defecto", "fechaLimite");
            if (defectoActual != this.ordenDefectoActual) {
                this.ordenActual = defectoActual;
                this.ordenDefectoActual = defectoActual;
                cargarTareas(ordenActual);
            } else {
                cargarTareas(ordenActual);
            }
        }
    }

    /**
     * Al destruirse la vista, cierra la conexión con la BD.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}
