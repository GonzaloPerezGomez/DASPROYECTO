package com.example.dasproyecto;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
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

public class EditTareaActivity extends AppCompatActivity {

    private static final String TAG = "EditTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha, etDireccion;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar;
    private DBmanager dbManager;
    private TextView tituloActivity;
    private long tareaId = -1; // Almacenamos el ID para saber qué tarea editar

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.editar_tarea_titulo);


        // 1. Inicializar Vistas

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        etDireccion = findViewById(R.id.etDireccion);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // 2. Setup Spinner (Mantenemos tu lógica: Alta=2, Media=1, Baja=0)
        String[] prioridades = {"Baja", "Media", "Alta"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        // 3. RECUPERAR DATOS DEL INTENT
        if (getIntent().hasExtra(DBmanager.COL_ID)) {
            dbManager = new DBmanager(this);
            dbManager.open();
            Log.d(TAG, "Recuperando tarea con ID: " + getIntent().getLongExtra(DBmanager.COL_ID, -1));
            tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);
            Cursor cursor = dbManager.getTarea(tareaId);
            if (cursor != null && cursor.moveToFirst()) {
                etTitulo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
                etDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION)));
                etFecha.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE)));
                etDireccion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DIRECCION)));
                spinnerPrioridad.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD)));
            }
        }

        etFecha.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                configurarSelectorFecha(etFecha.getText().toString());
            }
        });

        // 5. ACTUALIZAR
        btnGuardar.setOnClickListener(v -> actualizarTarea());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void actualizarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        String direccion = etDireccion.getText().toString().trim();
        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError("El título es obligatorio");
            return;
        }

        // 4. ACTUALIZAR EN LUGAR DE INSERTAR
        // Usamos el ID recuperado para modificar la tarea existente
        dbManager.actualizarTareaCompleta(tareaId, titulo, descripcion, prioridadIndex, fecha, direccion);

        Toast.makeText(this, "Tarea actualizada", Toast.LENGTH_SHORT).show();
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

    private void configurarSelectorFecha(String fechaActual) {
        int d = 0, m = 0, a = 0;

        // Intentar leer la fecha que ya está puesta
        if (!fechaActual.isEmpty()) {
            try {
                String[] partes = fechaActual.split("/");
                d = Integer.parseInt(partes[0]);
                m = Integer.parseInt(partes[1]) - 1; // El diálogo usa meses de 0 a 11
                a = Integer.parseInt(partes[2]);
            } catch (Exception e) {
                // Si falla el parseo, se quedan en 0 y el diálogo usará la actual
            }
        }

        ElegirFechaDialog dialogoFecha = ElegirFechaDialog.newInstance(d, m, a, (view, year, month, dayOfMonth) -> {
            String fechaSeleccionada = dayOfMonth + "/" + (month + 1) + "/" + year;
            etFecha.setText(fechaSeleccionada);
        });

        dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
    }
}
