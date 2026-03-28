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
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;

import com.example.dasproyecto.EditTareaActivity;
import com.example.dasproyecto.R;
import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.dialog.EliminarTareaDialog;

/**
 * Fragmento que muestra los detalles de una tarea
 * y permite completarla, editarla o borrarla.
 */
public class DetalleTareaFragment extends Fragment {

    private static final String TAG = "DetalleTareaFragment";
    private static final String ARG_TAREA_ID = "tarea_id";

    private TextView tvTitulo, tvDescripcion, tvFecha, tvPrioridad, tvUbicacion;
    private ImageButton btnAbrirMapa;
    private Button btnCompletar;
    private DBmanager dbManager;
    private long tareaId = -1;
    private int estadoCompletada = 0;

    private String[] prioridades;

    /**
     * Interfaz para avisar a la Activity cuando se borra una tarea.
     */
    public interface OnTareaEliminadaListener {
        void onTareaEliminada();
    }

    private DetalleTareaFragment.OnTareaEliminadaListener listenerEliminada;

    /**
     * Interfaz para avisar a la Activity cuando se completa o descompleta una
     * tarea.
     */
    public interface OnTareaCompletadaListener {
        void onTareaCompletada(long tareaId);
    }

    private DetalleTareaFragment.OnTareaCompletadaListener listenerCompletada;

    /**
     * Al adjuntarse a la Activity, comprueba que implemente los listeners
     * necesarios.
     */
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

    /**
     * Infla el layout del fragmento.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_detalle_tarea, container, false);
    }

    /**
     * Una vez creada la vista, conecta los campos, carga los datos de la tarea
     * y prepara el botón de completar.
     */
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
        tvUbicacion = view.findViewById(R.id.tvUbicacion);
        btnAbrirMapa = view.findViewById(R.id.btnAbrirMapa);
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
            btnCompletar.setEnabled(false);
            int nuevoEstado = (estadoCompletada == 0) ? 1 : 0;
            dbManager.actualizarEstadoRemoto(tareaId, nuevoEstado).observe(getViewLifecycleOwner(), workInfo -> {
                if (workInfo != null && workInfo.getState().isFinished()) {
                    btnCompletar.setEnabled(true);
                    estadoCompletada = nuevoEstado;
                    listenerCompletada.onTareaCompletada(tareaId);
                    actualizarBotonCompletar();
                }
            });
        });
    }

    /**
     * Cambia el texto del botón según si la tarea está completada o no.
     */
    private void actualizarBotonCompletar() {
        btnCompletar.setText(estadoCompletada == 0
                ? R.string.btn_completada
                : R.string.btn_no_completada);
    }

    /**
     * Lee los datos de la tarea de la BD y rellena los campos en pantalla.
     */
    private void cargarDatos() {
        dbManager.getTareaRemoto(tareaId).observe(getViewLifecycleOwner(), workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                String resultado = workInfo.getOutputData().getString("datos");
                if (resultado == null) return;
                try {
                    JSONObject json = new JSONObject(resultado);
                    if (json.getBoolean("exito")) {
                        JSONObject tarea = json.getJSONObject("tarea");
                        Log.d(TAG, "Tarea remota cargada: " + tarea.optString("titulo"));
                        
                        tvTitulo.setText(tarea.optString("titulo", ""));
                        String desc = tarea.optString("descripcion", "");
                        if (!desc.equals("null")) tvDescripcion.setText(desc);
                        
                        String fechaBD = tarea.optString("fechaLimite", "");
                        if (!fechaBD.equals("null")) tvFecha.setText(DBmanager.formatFechaToUI(fechaBD));
                        
                        estadoCompletada = tarea.optInt("completada", 0);
                        int prioridad = tarea.optInt("prioridad", 0);
                        tvPrioridad.setText(prioridades[prioridad]);
                        actualizarBotonCompletar();
                        
                        String direccion = tarea.optString("direccion", "null");
                        String lat = tarea.optString("latitud", "null");
                        String lng = tarea.optString("longitud", "null");
                        
                        if (!lat.equals("null") && !lat.isEmpty() || (!direccion.equals("null") && !direccion.isEmpty())) {
                            if (!direccion.equals("null") && !direccion.isEmpty()) {
                                tvUbicacion.setText(direccion);
                            } else {
                                tvUbicacion.setText(String.format("Lat: %s\nLng: %s", lat, lng));
                            }
                            btnAbrirMapa.setVisibility(View.VISIBLE);
                            
                            btnAbrirMapa.setOnClickListener(v -> {
                                try {
                                    String uriText;
                                    if (!direccion.equals("null") && !direccion.isEmpty()) {
                                        uriText = "geo:0,0?q=" + android.net.Uri.encode(direccion);
                                    } else {
                                        uriText = "geo:" + lat + "," + lng + "?q=" + lat + "," + lng;
                                    }
                                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriText));
                                    mapIntent.setPackage("com.google.android.apps.maps");
                                    if (mapIntent.resolveActivity(requireContext().getPackageManager()) != null) {
                                        startActivity(mapIntent);
                                    } else {
                                        Intent genericMapIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uriText));
                                        startActivity(genericMapIntent);
                                    }
                                } catch (Exception e) {
                                    Toast.makeText(requireContext(), "No se pudo abrir el mapa", Toast.LENGTH_SHORT).show();
                                }
                            });
                        } else {
                            tvUbicacion.setText(R.string.ubicacion_no_establecida);
                            btnAbrirMapa.setVisibility(View.GONE);
                            btnAbrirMapa.setOnClickListener(null);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Tarea no encontrada remotamente", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando la tarea", e);
                }
            }
        });
    }

    /**
     * Gestiona los clics del menú de la toolbar (Editar, Eliminar).
     */
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
                dbManager.eliminarRemoto(tareaId).observe(getViewLifecycleOwner(), workInfo -> {
                    if (workInfo != null && workInfo.getState().isFinished()) {
                        listenerEliminada.onTareaEliminada();
                        Toast.makeText(getActivity(), getString(R.string.toast_tarea_eliminada, titulo), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }

        return false;
    }

    /**
     * Al volver a esta pantalla, recarga los datos por si se editaron.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (dbManager != null && tareaId != -1) {
            cargarDatos();
        }
    }

    /**
     * Al destruirse la vista, cierra la conexión con la BD.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
