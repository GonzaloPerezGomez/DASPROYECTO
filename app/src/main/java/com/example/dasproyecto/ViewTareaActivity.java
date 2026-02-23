package com.example.dasproyecto;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.dialog.EliminarTareaDialog;

public class ViewTareaActivity extends AppCompatActivity {

    private static final String TAG = "ViewTareaActivity";
    private TextView tvTitulo, tvDescripcion, tvFecha, tvPrioridad;
    private Button btnCompletar;
    private DBmanager dbManager;
    private long tareaId = -1;
    private int estadoCompletada = 0;

    private String[] prioridades;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tarea);

        // 1. Configurar Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(this::onMenuItemClick);

        // Cargar prioridades desde recursos
        prioridades = new String[] {
                getString(R.string.prioridad_baja),
                getString(R.string.prioridad_media),
                getString(R.string.prioridad_alta)
        };

        // Listener para cuando EliminarTareaDialog confirma la eliminación
        getSupportFragmentManager().setFragmentResultListener(
                EliminarTareaDialog.RESULT_KEY, this, (key, bundle) -> finish());

        // 2. Inicializar Vistas
        tvTitulo = findViewById(R.id.tvTitulo);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvFecha = findViewById(R.id.tvFecha);
        tvPrioridad = findViewById(R.id.tvPrioridad);
        btnCompletar = findViewById(R.id.btnCompletar);

        // 3. Recuperar y mostrar datos
        tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);
        if (tareaId == -1) {
            Toast.makeText(this, R.string.error_tarea_no_encontrada, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbManager = new DBmanager(this);
        dbManager.open();

        cargarDatos();

        // 4. Botón Completar/No completar
        btnCompletar.setOnClickListener(v -> {
            estadoCompletada = (estadoCompletada == 0) ? 1 : 0;
            dbManager.actualizarEstado(tareaId, estadoCompletada);
            finish();
        });
    }

    private void actualizarBotonCompletar() {
        btnCompletar.setText(estadoCompletada == 0
                ? R.string.btn_completada
                : R.string.btn_no_completada);
    }

    private void cargarDatos() {
        Cursor cursor = dbManager.getTarea(tareaId);
        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "Tarea encontrada: " + cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
            tvTitulo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
            tvDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION)));
            tvFecha.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE)));

            estadoCompletada = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_COMPLETADA));

            int prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD));
            tvPrioridad.setText(prioridad >= 0 && prioridad < prioridades.length
                    ? prioridades[prioridad]
                    : getString(R.string.prioridad_desconocida));

            cursor.close();
        }
        actualizarBotonCompletar();
    }

    private boolean onMenuItemClick(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_editar) {
            // Pencil icon → navigate to EditTareaActivity
            Intent intent = new Intent(this, EditTareaActivity.class);
            intent.putExtra(DBmanager.COL_ID, tareaId);
            startActivity(intent);
            return true;

        } else if (id == R.id.action_eliminar) {
            // Overflow menu → mostrar diálogo de confirmación
            String titulo = tvTitulo.getText().toString();
            EliminarTareaDialog dialogo = EliminarTareaDialog.newInstance(tareaId, titulo);
            dialogo.show(getSupportFragmentManager(), "EliminarTareaDialog");
            return true;
        }

        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbManager != null) {
            cargarDatos();
        }
    }
}
