package com.example.dasproyecto;

import android.os.Bundle;
import androidx.appcompat.widget.Toolbar;
import androidx.annotation.NonNull;

import com.example.dasproyecto.fragment.AjustesFragment;

/**
 * Pantalla de ajustes de la app.
 * Carga el fragmento de preferencias donde se cambia el idioma, el tema, etc.
 */
public class AjustesActivity extends BaseActivity {

    /**
     * Se ejecuta al abrir la pantalla.
     * Configura la Toolbar y mete el fragmento de ajustes.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        // Configurar la Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar_ajustes);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_ajustes, new AjustesFragment())
                    .commit();
        }
    }
}
