package com.example.dasproyecto;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dasproyecto.dialog.ElegirFechaDialog;
import com.example.dasproyecto.db.DBmanager;

import android.util.Log;

public class AddTareaActivity extends AppCompatActivity {

    private static final String TAG = "AddTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar;
    private TextView tituloActivity;
    private DBmanager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.titulo_nueva_tarea);

        // Initialize Views
        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Setup Spinner
        String[] prioridades = { getString(R.string.prioridad_baja), getString(R.string.prioridad_media),
                getString(R.string.prioridad_alta) };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        // Initialize DB
        dbManager = new DBmanager(this);
        dbManager.open();

        // Listeners

        etFecha.setOnClickListener(v -> configurarSelectorFecha());

        btnGuardar.setOnClickListener(v -> guardarTarea());

        btnCancelar.setOnClickListener(v -> {
            Log.d(TAG, "Cancelando creación de tarea");
            finish();
        });
    }

    private void guardarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();

        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }
        if (TextUtils.isEmpty(fecha)) {
            etFecha.setError(getString(R.string.error_fecha_requerida));
            return;
        }

        dbManager.insertar(titulo, descripcion, prioridadIndex, fecha);
        Log.i(TAG, "Tarea guardada: " + titulo);
        Toast.makeText(this, R.string.toast_tarea_guardada, Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    private void configurarSelectorFecha() {
        getSupportFragmentManager().setFragmentResultListener("fechaSeleccionada", this, (requestKey, bundle) -> {
            int year = bundle.getInt("year");
            int month = bundle.getInt("month");
            int day = bundle.getInt("day");
            String fecha = day + "/" + (month + 1) + "/" + year;
            etFecha.setText(fecha);
        });

        ElegirFechaDialog dialogoFecha = new ElegirFechaDialog();
        dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
    }
}
