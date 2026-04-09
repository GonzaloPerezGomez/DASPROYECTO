package com.example.dasproyecto.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONObject;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.example.dasproyecto.R;
import com.example.dasproyecto.ui.dialogs.ElegirFechaDialog;
import com.example.dasproyecto.data.db.DBmanager;

/**
 * Pantalla para editar una tarea que ya existe.
 * Carga los datos actuales y deja que el usuario los modifique y guarde.
 */
public class EditTareaActivity extends BaseActivity {

    private static final String TAG = "EditTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar, btnSeleccionarUbicacion;
    private DBmanager dbManager;
    private TextView tituloActivity, tvUbicacionSeleccionada;
    private long tareaId = -1;

    private Double latitudDB = null;
    private Double longitudDB = null;
    private String direccionDB = null;

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    latitudDB = data.getDoubleExtra("latitud", 0.0);
                    longitudDB = data.getDoubleExtra("longitud", 0.0);
                    direccionDB = data.getStringExtra("direccion");
                    
                    if (direccionDB != null && !direccionDB.isEmpty()) {
                        tvUbicacionSeleccionada.setText("📍 " + direccionDB);
                    } else {
                        tvUbicacionSeleccionada.setText(String.format(java.util.Locale.getDefault(), "📍 Lat: %.4f, Lng: %.4f", latitudDB, longitudDB));
                    }
                }
            }
    );

    /**
     * Se ejecuta al abrir la pantalla.
     * Prepara el formulario y carga los datos de la tarea a editar.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.titulo_editar_tarea);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnSeleccionarUbicacion = findViewById(R.id.btnSeleccionarUbicacion);
        tvUbicacionSeleccionada = findViewById(R.id.tvUbicacionSeleccionada);

        btnSeleccionarUbicacion.setOnClickListener(v -> mapLauncher.launch(new Intent(this, SeleccionarUbicacionActivity.class)));

        String[] prioridades = getResources().getStringArray(R.array.prioridades_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        if (getIntent().hasExtra(DBmanager.COL_ID)) {
            dbManager = new DBmanager(this);
            Log.d(TAG, "Recuperando tarea con ID: " + getIntent().getLongExtra(DBmanager.COL_ID, -1));
            tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);
            
            new Thread(() -> {
                JSONObject json = dbManager.getTareaProvider(tareaId);
                runOnUiThread(() -> {
                    try {
                        if (json.getBoolean("exito")) {
                            JSONObject tarea = json.getJSONObject("tarea");
                            etTitulo.setText(tarea.optString("titulo", ""));
                            String desc = tarea.optString("descripcion", "");
                            if (!desc.equals("null")) etDescripcion.setText(desc);
                            
                            String fechaBD = tarea.optString("fechaLimite", "");
                            if (!fechaBD.equals("null")) etFecha.setText(DBmanager.formatFechaToUI(fechaBD));
                            
                            spinnerPrioridad.setSelection(tarea.optInt("prioridad", 0));
                            String direccion = tarea.optString("direccion", "null");
                            String lat = tarea.optString("latitud", "null");
                            String lng = tarea.optString("longitud", "null");
                            
                            if (!lat.equals("null") && !lat.isEmpty()) {
                                latitudDB = Double.parseDouble(lat);
                                longitudDB = Double.parseDouble(lng);
                                tvUbicacionSeleccionada.setText(String.format("📍 Lat: %s | Lng: %s", lat, lng));
                            }
                            
                            if (!direccion.equals("null") && !direccion.isEmpty()) {
                                direccionDB = direccion;
                                tvUbicacionSeleccionada.setText("📍 " + direccion);
                            }
                            
                            if (latitudDB == null && direccionDB == null) {
                                tvUbicacionSeleccionada.setText(R.string.ubicacion_no_establecida);
                            }
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando la tarea", e);
                    }
                });
            }).start();
        }

        etFecha.setOnClickListener(v -> configurarSelectorFecha());

        btnGuardar.setOnClickListener(v -> actualizarTarea());
        btnCancelar.setOnClickListener(v -> finish());
    }

    /**
     * Comprueba que los campos estén bien y actualiza la tarea en la BD.
     */
    private void actualizarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fechaUI = etFecha.getText().toString().trim();
        String fechaDB = DBmanager.formatFechaToDB(fechaUI);
        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }

        btnGuardar.setEnabled(false);
        Toast.makeText(this, "Guardando...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            boolean exito = dbManager.actualizarTareaCompletaProvider(tareaId, titulo, descripcion, prioridadIndex, fechaDB, latitudDB, longitudDB, direccionDB);
            runOnUiThread(() -> {
                btnGuardar.setEnabled(true);
                if (exito) {
                    Toast.makeText(this, R.string.toast_tarea_actualizada, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error actualizando la tarea con ContentProvider", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * Se llama al cerrar la pantalla.
     * Cierra la conexión con la BD.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Abre el selector de fecha y recoge la fecha elegida.
     * Si ya había una fecha puesta, la usa como punto de partida.
     */
    private void configurarSelectorFecha() {
        getSupportFragmentManager().setFragmentResultListener("fechaSeleccionada", this, (requestKey, bundle) -> {
            int year = bundle.getInt("year");
            int month = bundle.getInt("month");
            int day = bundle.getInt("day");
            String fecha = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            etFecha.setText(fecha);
        });

        ElegirFechaDialog dialogoFecha;
        String fechaActual = etFecha.getText().toString().trim();
        if (!fechaActual.isEmpty()) {
            try {
                String[] partes = fechaActual.split("/");
                int day = Integer.parseInt(partes[0]);
                int month = Integer.parseInt(partes[1]) - 1;
                int year = Integer.parseInt(partes[2]);
                dialogoFecha = ElegirFechaDialog.newInstance(day, month, year);
            } catch (Exception e) {
                dialogoFecha = new ElegirFechaDialog();
            }
        } else {
            dialogoFecha = new ElegirFechaDialog();
        }
        dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
    }
}
