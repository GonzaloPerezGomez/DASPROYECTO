package com.example.dasproyecto.ui.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.dasproyecto.R;
import com.example.dasproyecto.ui.dialogs.ElegirFechaDialog;
import com.example.dasproyecto.data.db.DBmanager;

import android.content.Intent;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import android.util.Log;

import androidx.preference.PreferenceManager;
import android.provider.CalendarContract;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import android.database.Cursor;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;

/**
 * Pantalla para crear una nueva tarea.
 * Aquí el usuario rellena título, descripción, fecha y prioridad,
 * y al guardar se inserta en la base de datos.
 */
public class AddTareaActivity extends BaseActivity {

    private static final String TAG = "AddTareaActivity";
    private EditText etTitulo, etDescripcion, etFecha;
    private Spinner spinnerPrioridad;
    private Button btnGuardar, btnCancelar, btnSeleccionarUbicacion;
    private TextView tituloActivity, tvUbicacionSeleccionada;
    private DBmanager dbManager;

    private Double latitudDB = null;
    private Double longitudDB = null;
    private String direccionDB = null;

    private final ActivityResultLauncher<Intent> mapLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    latitudDB = data.getDoubleExtra("latitud", 0.0);
                    longitudDB = data.getDoubleExtra("longitud", 0.0);
                    direccionDB = data.getStringExtra("direccion");
                    
                    if (direccionDB != null && !direccionDB.isEmpty()) {
                        tvUbicacionSeleccionada.setText("📍 " + direccionDB);
                    } else {
                        tvUbicacionSeleccionada.setText(String.format(java.util.Locale.getDefault(), "📍 Lat: %.4f, Lng: %.4f", latitudDB, longitudDB));
                    }
                }
            }
    );

    /**
     * Se ejecuta al abrir la pantalla.
     * Prepara los campos del formulario, el spinner de prioridad y los botones.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_tarea);

        tituloActivity = findViewById(R.id.tituloActivity);
        tituloActivity.setText(R.string.titulo_nueva_tarea);

        etTitulo = findViewById(R.id.etTitulo);
        etDescripcion = findViewById(R.id.etDescripcion);
        etFecha = findViewById(R.id.etFecha);
        spinnerPrioridad = findViewById(R.id.spinnerPrioridad);
        btnGuardar = findViewById(R.id.btnGuardar);
        btnCancelar = findViewById(R.id.btnCancelar);
        btnSeleccionarUbicacion = findViewById(R.id.btnSeleccionarUbicacion);
        tvUbicacionSeleccionada = findViewById(R.id.tvUbicacionSeleccionada);

        String[] prioridades = getResources().getStringArray(R.array.prioridades_array);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, prioridades);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPrioridad.setAdapter(adapter);

        dbManager = new DBmanager(this);

        etFecha.setOnClickListener(v -> configurarSelectorFecha());

        btnSeleccionarUbicacion.setOnClickListener(v -> mapLauncher.launch(new Intent(this, SeleccionarUbicacionActivity.class)));

        btnGuardar.setOnClickListener(v -> guardarTarea());

        btnCancelar.setOnClickListener(v -> {
            Log.d(TAG, "Cancelando creación de tarea");
            finish();
        });
    }

    /**
     * Comprueba que los campos estén bien y guarda la tarea en la BD.
     * Si falta el título o la fecha, muestra un error.
     */
    private void guardarTarea() {
        String titulo = etTitulo.getText().toString().trim();
        String descripcion = etDescripcion.getText().toString().trim();
        String fechaUI = etFecha.getText().toString().trim();
        String fechaDB = DBmanager.formatFechaToDB(fechaUI);

        int prioridadIndex = spinnerPrioridad.getSelectedItemPosition();

        if (TextUtils.isEmpty(titulo)) {
            etTitulo.setError(getString(R.string.error_titulo_requerido));
            return;
        }
        if (TextUtils.isEmpty(fechaUI)) {
            etFecha.setError(getString(R.string.error_fecha_requerida));
            return;
        }

        btnGuardar.setEnabled(false);
        Toast.makeText(this, "Guardando tarea en el servidor...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {
            boolean exito = dbManager.insertarProvider(titulo, descripcion, prioridadIndex, fechaDB, latitudDB, longitudDB, direccionDB);
            runOnUiThread(() -> {
                btnGuardar.setEnabled(true);
                if (exito) {
                    Log.i(TAG, "Tarea guardada exitosamente: " + titulo);
                    
                    // Sincronización con Google Calendar
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
                    if (prefs.getBoolean("sync_google_calendar", false)) {
                        String ubicacion = (direccionDB != null) ? direccionDB : "";
                        sincronizarEventoCalendario(titulo, descripcion, fechaDB, ubicacion);
                    }

                    Toast.makeText(this, R.string.toast_tarea_guardada, Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(this, "Error insertando la tarea con ContentProvider", Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * Se llama al cerrar la pantalla.
     * Cierra la conexión con la base de datos para no dejar nada abierto.
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    /**
     * Inserta un evento en el calendario de Google del usuario.
     * Busca la cuenta que coincida con el email de la sesión o la principal.
     */
    private void sincronizarEventoCalendario(String titulo, String descripcion, String fechaDB, String ubicacion) {
        try {
            // 1. Obtener el email del usuario de SharedPreferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String userEmail = prefs.getString("session_user_email", "");

            // 2. Buscar ID del calendario (priorizando el del email o el de Google)
            long calendarId = -1;
            String selectedAccount = "N/A";
            String[] projection = {
                CalendarContract.Calendars._ID, 
                CalendarContract.Calendars.ACCOUNT_NAME,
                CalendarContract.Calendars.ACCOUNT_TYPE
            };
            
            ContentResolver cr = getContentResolver();
            Cursor cur = cr.query(CalendarContract.Calendars.CONTENT_URI, projection, null, null, null);
            
            if (cur != null) {
                while (cur.moveToNext()) {
                    long id = cur.getLong(0);
                    String accountName = cur.getString(1);
                    String accountType = cur.getString(2);

                    if (calendarId == -1) {
                        calendarId = id;
                        selectedAccount = accountName;
                    }

                    // Preferir cuenta de Google sobre local
                    if ("com.google".equals(accountType) && !accountName.equalsIgnoreCase(userEmail)) {
                        calendarId = id;
                        selectedAccount = accountName;
                    }

                    // El ideal es que coincida con el email
                    if (accountName.equalsIgnoreCase(userEmail)) {
                        calendarId = id;
                        selectedAccount = accountName;
                        break;
                    }
                }
                cur.close();
            }

            Log.i(TAG, "Calendario seleccionado: ID=" + calendarId + ", Cuenta=" + selectedAccount);

            if (calendarId == -1) {
                Log.e(TAG, "No se encontró ningún calendario para sincronizar.");
                return;
            }

            // 3. Parsear la fecha (YYYY-MM-DD) a milisegundos (UTC para All Day)
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            sdf.setTimeZone(TimeZone.getTimeZone("UTC")); // All day events must be in UTC
            Date date = sdf.parse(fechaDB);
            if (date == null) return;
            long startMillis = date.getTime();
            long endMillis = startMillis + (24 * 60 * 60 * 1000); // 24 horas para evento de día completo

            // 4. Insertar el evento
            ContentValues values = new ContentValues();
            values.put(CalendarContract.Events.DTSTART, startMillis);
            values.put(CalendarContract.Events.DTEND, endMillis);
            values.put(CalendarContract.Events.TITLE, "[DAS] " + titulo);
            values.put(CalendarContract.Events.DESCRIPTION, descripcion);
            values.put(CalendarContract.Events.EVENT_LOCATION, ubicacion);
            values.put(CalendarContract.Events.CALENDAR_ID, calendarId);
            values.put(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
            values.put(CalendarContract.Events.ALL_DAY, 1); // Hacerlo evento de todo el día mejora visibilidad

            Uri uri = cr.insert(CalendarContract.Events.CONTENT_URI, values);
            if (uri != null) {
                Log.i(TAG, "Evento de calendario creado: " + uri.toString());
            }

        } catch (Exception e) {
            Log.e(TAG, "Error sincronizando con el calendario: " + e.getMessage());
        }
    }

    /**
     * Abre el selector de fecha y recoge la fecha que el usuario elija.
     */
    private void configurarSelectorFecha() {
        getSupportFragmentManager().setFragmentResultListener("fechaSeleccionada", this, (requestKey, bundle) -> {
            int year = bundle.getInt("year");
            int month = bundle.getInt("month");
            int day = bundle.getInt("day");
            String fecha = String.format(java.util.Locale.getDefault(), "%02d/%02d/%04d", day, month + 1, year);
            etFecha.setText(fecha);
        });

        ElegirFechaDialog dialogoFecha = new ElegirFechaDialog();
        dialogoFecha.show(getSupportFragmentManager(), "ElegirFecha");
    }
}
