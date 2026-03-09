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
import androidx.annotation.NonNull;

import com.example.dasproyecto.dialog.ElegirFechaDialog;
import com.example.dasproyecto.db.DBmanager;

/**
 * Pantalla para editar una tarea que ya existe.
 * Carga los datos actuales y deja que el usuario los modifique y guarde.
 */
public class EditTareaActivity extends BaseActivity {

    private static final String TAG = "EditTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar;
    private DBmanager dbManager;
    private TextView tituloActivity;
    private long tareaId = -1;

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

        String[] prioridades = getResources().getStringArray(R.array.prioridades_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        if (getIntent().hasExtra(DBmanager.COL_ID)) {
            dbManager = new DBmanager(this);
            dbManager.open();
            Log.d(TAG, "Recuperando tarea con ID: " + getIntent().getLongExtra(DBmanager.COL_ID, -1));
            tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);
            Cursor cursor = dbManager.getTarea(tareaId);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    etTitulo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
                    etDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION)));
                    String fechaBD = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE));
                    etFecha.setText(DBmanager.formatFechaToUI(fechaBD));
                    spinnerPrioridad.setSelection(cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD)));
                }
                cursor.close();
            }
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

        dbManager.actualizarTareaCompleta(tareaId, titulo, descripcion, prioridadIndex, fechaDB);

        Toast.makeText(this, R.string.toast_tarea_actualizada, Toast.LENGTH_SHORT).show();
        finish();
    }

    /**
     * Se llama al cerrar la pantalla.
     * Cierra la conexión con la BD.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
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
