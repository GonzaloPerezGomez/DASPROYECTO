package com.example.dasproyecto;

import android.os.Bundle;

import com.example.dasproyecto.fragment.AjustesFragment;

public class AjustesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ajustes);

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_ajustes, new AjustesFragment())
                    .commit();
        }
    }
}
