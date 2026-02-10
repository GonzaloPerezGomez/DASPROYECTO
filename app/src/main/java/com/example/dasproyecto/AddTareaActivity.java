package com.example.dasproyecto;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dasproyecto.db.DBmanager;

import android.util.Log;

public class AddTareaActivity extends AppCompatActivity {

    private static final String TAG = "AddTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha, etDireccion;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar;
    private DBmanager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        // Initialize Views
        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Setup Spinner
        // Note: In a real app we might want to map these to the integer values 0, 1, 2 more explicitly
        // Here we assume index 0 = High (2), 1 = Medium (1), 2 = Low (0) or similar
        // For simplicity let's use a simple array.
        String[] prioridades = {"Baja", "Media", "Alta"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);


        // Initialize DB
        dbManager = new DBmanager(this);
        dbManager.open();

        // Listeners
        btnGuardar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                guardarTarea();
            }
        });

        btnCancelar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(TAG, "Cancelando creación de tarea");
                finish();
            }
        });
    }

    private void guardarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        
        // Priority logic: Alta (index 0) = 2, Media (index 1) = 1, Baja (index 2) = 0
        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();
        int prioridad = 0;
        switch (prioridadIndex) {
            case 0: prioridad = 2; break; // Alta
            case 1: prioridad = 1; break; // Media
            default: prioridad = 0; break; // Baja
        }

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }

        dbManager.insertar(titulo, descripcion, prioridad, fecha, direccion);
        Log.i(TAG, "Tarea guardada: " + titulo);
        Toast.makeText(this, "Tarea guardada", Toast.LENGTH_SHORT).show();
        
        // Return OK result to MainActivity
        setResult(RESULT_OK);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
