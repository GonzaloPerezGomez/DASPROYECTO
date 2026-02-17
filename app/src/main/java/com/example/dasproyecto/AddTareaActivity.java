package com.example.dasproyecto;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dasproyecto.Dialogs.ElegirFechaDialog;
import com.example.dasproyecto.db.DBmanager;

import android.util.Log;

public class AddTareaActivity extends AppCompatActivity {

    private static final String TAG = "AddTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha, etDireccion;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar;
    private TextView tituloActivity;
    private DBmanager dbManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.añadir_tarea_titulo);

        // Initialize Views
        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // Setup Spinner
        String[] prioridades = {"Baja", "Media", "Alta"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        // Initialize DB
        dbManager = new DBmanager(this);
        dbManager.open();

        // Listeners

        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurarSelectorFecha();
            }
        });

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

        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }

        dbManager.insertar(titulo, descripcion, prioridadIndex, fecha, direccion);
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

    private void configurarSelectorFecha() {
        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Instanciar con el nuevo nombre de clase
                ElegirFechaDialog dialogoFecha = ElegirFechaDialog.newInstance((view, year, month, dayOfMonth) -> {
                    // Formatear fecha (Mes + 1 porque Enero es 0)
                    String fechaSeleccionada = dayOfMonth + "/" + (month + 1) + "/" + year;
                    etFecha.setText(fechaSeleccionada);
                });

                // Mostrar usando el FragmentManager
                dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
            }
        });
    }
}
