package com.example.dasproyecto;

import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dasproyecto.dialog.ElegirFechaDialog;
import com.example.dasproyecto.db.DBmanager;

public class EditTareaActivity extends AppCompatActivity {

    private static final String TAG = "EditTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
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
        tituloActivity.setText(R.string.titulo_editar_tarea);

        // 1. Inicializar Vistas

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);

        // 2. Setup Spinner (Mantenemos tu lógica: Alta=2, Media=1, Baja=0)
        String[] prioridades = { getString(R.string.prioridad_baja), getString(R.string.prioridad_media),
                getString(R.string.prioridad_alta) };
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
                spinnerPrioridad.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD)));
                cursor.close();
            }
        }

        etFecha.setOnClickListener(v -> configurarSelectorFecha());

        // 5. ACTUALIZAR
        btnGuardar.setOnClickListener(v -> actualizarTarea());
        btnCancelar.setOnClickListener(v -> finish());
    }

    private void actualizarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fecha = etFecha.getText().toString().trim();
        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }

        // 4. ACTUALIZAR EN LUGAR DE INSERTAR
        // Usamos el ID recuperado para modificar la tarea existente
        dbManager.actualizarTareaCompleta(tareaId, titulo, descripcion, prioridadIndex, fecha);

        Toast.makeText(this, R.string.toast_tarea_actualizada, Toast.LENGTH_SHORT).show();
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

        ElegirFechaDialog dialogoFecha;
        String fechaActual = etFecha.getText().toString().trim();
        if (!fechaActual.isEmpty()) {
            try {
                String[] partes = fechaActual.split("/");
                int day = Integer.parseInt(partes[0]);
                int month = Integer.parseInt(partes[1]) - 1; // Calendar months are 0-based
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
