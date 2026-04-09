package com.example.dasproyecto.ui.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.dasproyecto.R;
import com.example.dasproyecto.data.db.DBmanager;

import org.json.JSONObject;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Actividad para registrar un nuevo usuario en la base de datos remota.
 */
public class RegistroActivity extends BaseActivity {

    private static final String TAG = "RegistroActivity";

    private TextInputEditText etNombre, etEmail, etPassword, etConfirmarPassword;
    private MaterialButton btnRegistrarse, btnVolverLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        etNombre = findViewById(R.id.etNombre);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmarPassword = findViewById(R.id.etConfirmarPassword);
        btnRegistrarse = findViewById(R.id.btnRegistrarse);
        btnVolverLogin = findViewById(R.id.btnVolverLogin);

        btnRegistrarse.setOnClickListener(v -> intentarRegistro());

        btnVolverLogin.setOnClickListener(v -> {
            // Volver atrás destruyendo esta activity
            finish();
        });
    }

    private void intentarRegistro() {
        String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmar = etConfirmarPassword.getText() != null ? etConfirmarPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("El email es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("La contraseña es obligatoria");
            return;
        }

        if (!password.equals(confirmar)) {
            etConfirmarPassword.setError("Las contraseñas no coinciden");
            return;
        }

        Log.d(TAG, "Intentando registro de usuario: " + email);

        // =========================================================================
        // HITO 2 - Crear usuario remotamente
        // =========================================================================
        Toast.makeText(this, "Registrando en la base de datos...", Toast.LENGTH_SHORT).show();
        btnRegistrarse.setEnabled(false);

        DBmanager db = new DBmanager(this);
        db.registroRemoto(nombre, email, password).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                btnRegistrarse.setEnabled(true);
                String resultado = workInfo.getOutputData().getString("datos");
                try {
                    JSONObject json = new JSONObject(resultado);
                    if (json.getBoolean("exito")) {
                        Toast.makeText(this, "Cuenta creada con éxito. Ya puedes iniciar sesión.", Toast.LENGTH_LONG)
                                .show();
                        finish(); // Volvemos al LoginActivity
                    } else {
                        Toast.makeText(this, "Error: " + json.getString("mensaje"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error inesperado: " + resultado, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
