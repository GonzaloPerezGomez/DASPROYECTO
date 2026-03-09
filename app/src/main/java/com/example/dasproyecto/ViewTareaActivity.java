package com.example.dasproyecto;

import android.os.Bundle;

import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.DetalleTareaFragment;
import androidx.annotation.NonNull;

/**
 * Pantalla que muestra el detalle de una tarea en modo vertical.
 * Básicamente mete el fragmento DetalleTareaFragment dentro de esta actividad.
 */
public class ViewTareaActivity extends BaseActivity implements DetalleTareaFragment.OnTareaEliminadaListener,
        DetalleTareaFragment.OnTareaCompletadaListener {

    /**
     * Se ejecuta al abrir la pantalla.
     * Crea el fragmento de detalle y le pasa el ID de la tarea.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_tarea);

        if (savedInstanceState == null) {
            long tareaId = getIntent().getLongExtra(DBmanager.COL_ID, -1);

            DetalleTareaFragment fragment = new DetalleTareaFragment();
            Bundle args = new Bundle();
            args.putLong("tarea_id", tareaId);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_detalle_tarea, fragment)
                    .commit();
        }
    }

    /**
     * Se llama cuando se borra la tarea.
     * Simplemente cierra esta pantalla y vuelve atrás.
     */
    @Override
    public void onTareaEliminada() {
        finish();
    }

    /**
     * Se llama cuando cambia el estado completada/pendiente de la tarea.
     * En esta pantalla no hace nada extra.
     *
     * @param tareaId ID de la tarea.
     */
    @Override
    public void onTareaCompletada(long tareaId) {
    }
}
