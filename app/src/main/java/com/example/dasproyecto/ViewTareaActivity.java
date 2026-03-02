package com.example.dasproyecto;

import android.os.Bundle;

import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.DetalleTareaFragment;

public class ViewTareaActivity extends BaseActivity implements DetalleTareaFragment.OnTareaEliminadaListener,
        DetalleTareaFragment.OnTareaCompletadaListener {

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

    @Override
    public void onTareaEliminada() {
        finish();
    }

    @Override
    public void onTareaCompletada(long tareaId) {
    }
}
