package com.example.dasproyecto;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dasproyecto.dialog.ElegirFechaDialog;
import com.example.dasproyecto.db.DBmanager;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import org.json.JSONObject;

import android.util.Log;
import androidx.annotation.NonNull;

/**
 * Pantalla para crear una nueva tarea.
 * Aquí el usuario rellena título, descripción, fecha y prioridad,
 * y al guardar se inserta en la base de datos.
 */
public class AddTareaActivity extends BaseActivity {

    private static final String TAG = "AddTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar, btnSeleccionarUbicacion;
    private TextView tituloActivity, tvUbicacionSeleccionada;
    private DBmanager dbManager;

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
     * Prepara los campos del formulario, el spinner de prioridad y los botones.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.titulo_nueva_tarea);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnSeleccionarUbicacion = findViewById(R.id.btnSeleccionarUbicacion);
        tvUbicacionSeleccionada = findViewById(R.id.tvUbicacionSeleccionada);

        String[] prioridades = getResources().getStringArray(R.array.prioridades_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        dbManager = new DBmanager(this);
        dbManager.open();

        etFecha.setOnClickListener(v -> configurarSelectorFecha());

        btnSeleccionarUbicacion.setOnClickListener(v -> mapLauncher.launch(new Intent(this, SeleccionarUbicacionActivity.class)));

        btnGuardar.setOnClickListener(v -> guardarTarea());

        btnCancelar.setOnClickListener(v -> {
            Log.d(TAG, "Cancelando creación de tarea");
            finish();
        });
    }

    /**
     * Comprueba que los campos estén bien y guarda la tarea en la BD.
     * Si falta el título o la fecha, muestra un error.
     */
    private void guardarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fechaUI = etFecha.getText().toString().trim();
        String fechaDB = DBmanager.formatFechaToDB(fechaUI);

        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }
        if (TextUtils.isEmpty(fechaUI)) {
            etFecha.setError(getString(R.string.error_fecha_requerida));
            return;
        }

        btnGuardar.setEnabled(false);
        Toast.makeText(this, "Guardando tarea en el servidor...", Toast.LENGTH_SHORT).show();

        dbManager.insertarRemoto(titulo, descripcion, prioridadIndex, fechaDB, latitudDB, longitudDB, direccionDB).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                btnGuardar.setEnabled(true);
                String resultado = workInfo.getOutputData().getString("datos");
                if (resultado == null) {
                    Toast.makeText(this, "Error de red al guardar", Toast.LENGTH_SHORT).show();
                    return;
                }
                try {
                    JSONObject json = new JSONObject(resultado);
                    if (json.getBoolean("exito")) {
                        Log.i(TAG, "Tarea guardada exitosamente: " + titulo);
                        Toast.makeText(this, R.string.toast_tarea_guardada, Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Error del servidor: " + json.optString("mensaje"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parseando la respuesta al insertar: " + resultado, e);
                    Toast.makeText(this, "Error interno al guardar", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    /**
     * Se llama al cerrar la pantalla.
     * Cierra la conexión con la base de datos para no dejar nada abierto.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    /**
     * Abre el selector de fecha y recoge la fecha que el usuario elija.
     */
    private void configurarSelectorFecha() {
        getSupportFragmentManager().setFragmentResultListener("fechaSeleccionada", this, (requestKey, bundle) -> {
            int year = bundle.getInt("year");
            int month = bundle.getInt("month");
            int day = bundle.getInt("day");
            String fecha = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            etFecha.setText(fecha);
        });

        ElegirFechaDialog dialogoFecha = new ElegirFechaDialog();
        dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
    }
}
