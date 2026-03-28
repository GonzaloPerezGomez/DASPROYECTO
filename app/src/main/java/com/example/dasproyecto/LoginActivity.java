package com.example.dasproyecto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.example.dasproyecto.db.DBmanager;

import org.json.JSONObject;

import androidx.preference.PreferenceManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.textfield.TextInputEditText;

/**
 * Actividad principal de Login.
 * Si el usuario no tiene sesión iniciada, debe pasar por aquí.
 */
public class LoginActivity extends BaseActivity {

    private static final String TAG = "LoginActivity";

    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnIrRegistro;
    private MaterialCheckBox chkRecuerdame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Auto-login check (Gestión de sesión)
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        if (pref.getInt("session_user_id", -1) != -1) {
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        setContentView(R.layout.activity_login);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnIrRegistro = findViewById(R.id.btnIrRegistro);
        chkRecuerdame = findViewById(R.id.chkRecuerdame);

        // Comprobar si teníamos guardado un email previo (si usó "Recuérdame" antes
        // pero cerró sesión)
        String correoGuardado = pref.getString("login_email", "");
        if (!correoGuardado.isEmpty()) {
            etEmail.setText(correoGuardado);
            chkRecuerdame.setChecked(true);
        }

        btnLogin.setOnClickListener(v -> intentarLogin());

        btnIrRegistro.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegistroActivity.class);
            startActivity(intent);
        });
    }

    private void intentarLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            etEmail.setError("El email es obligatorio");
            return;
        }

        if (TextUtils.isEmpty(password)) {
            etPassword.setError("La contraseña es obligatoria");
            return;
        }

        Log.d(TAG, "Intentando login con: " + email);

        // Guardar email en preferencias si marcó "Recuérdame"
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = pref.edit();
        if (chkRecuerdame.isChecked()) {
            editor.putString("login_email", email);
        } else {
            editor.remove("login_email");
        }
        editor.apply();

        // =========================================================================
        // HITO 2 - Llamar al ConexionWorker via DBmanager
        // =========================================================================
        Toast.makeText(this, "Conectando al servidor...", Toast.LENGTH_SHORT).show();
        btnLogin.setEnabled(false);

        DBmanager db = new DBmanager(this);
        db.loginRemoto(email, password).observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                btnLogin.setEnabled(true);
                String resultado = workInfo.getOutputData().getString("datos");
                try {
                    JSONObject json = new JSONObject(resultado);
                    if (json.getBoolean("exito")) {
                        // Guardar estado de sesión (ej: id_usuario logueado)
                        int userId = json.optInt("usuario_id", -1);
                        pref.edit().putInt("session_user_id", userId).apply();

                        Toast.makeText(this, "¡Bienvenido!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "Error: " + json.getString("mensaje"), Toast.LENGTH_LONG).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Error de parseo: " + resultado, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
}
