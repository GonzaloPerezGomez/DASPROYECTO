package com.example.dasproyecto;

import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.dasproyecto.db.DBmanager;

public class ViewTareaActivity extends AppCompatActivity {

    private static final String TAG = "ViewTareaActivity";
    private TextView tvTitulo, tvDescripcion, tvFecha, tvPrioridad;
    private Button btnVolver;
    private DBmanager dbManager;

    private static final String[] PRIORIDADES = { "Baja", "Media", "Alta" };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tarea);

        // 1. Inicializar Vistas
        tvTitulo = findViewById(R.id.tvTitulo);
        tvDescripcion = findViewById(R.id.tvDescripcion);
        tvFecha = findViewById(R.id.tvFecha);
        tvPrioridad = findViewById(R.id.tvPrioridad);
        btnVolver = findViewById(R.id.btnVolver);

        // 2. Recuperar y mostrar datos
        long tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);
        if (tareaId == -1) {
            Toast.makeText(this, "Error: tarea no encontrada", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        dbManager = new DBmanager(this);
        dbManager.open();

        Cursor cursor = dbManager.getTarea(tareaId);
        if (cursor != null && cursor.moveToFirst()) {
            Log.d(TAG, "Tarea encontrada: " + cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
            tvTitulo.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO)));
            tvDescripcion.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION)));
            tvFecha.setText(cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE)));

            int prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD));
            tvPrioridad.setText(prioridad >= 0 && prioridad < PRIORIDADES.length
                    ? PRIORIDADES[prioridad]
                    : "Desconocida");

            cursor.close();
        }

        // 3. Configurar botón de salida
        btnVolver.setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }
}
