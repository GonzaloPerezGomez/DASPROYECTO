package com.example.dasproyecto;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.dasproyecto.fragment.DetalleTareaFragment;
import com.example.dasproyecto.fragment.ListaTareasFragment;
import com.example.dasproyecto.notification.NotificacionReceiver;

import android.util.Log;
import android.view.MenuItem;

import com.example.dasproyecto.db.DBmanager;
import com.google.android.material.navigation.NavigationView;

import java.util.Calendar;
import android.net.Uri;
import android.database.Cursor;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import java.io.OutputStream;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;

/**
 * Pantalla principal de la app.
 * Tiene el menú lateral, muestra la lista de tareas
 * y coordina la navegación entre la lista y el detalle. También permite
 * exportar tareas.
 */
public class MainActivity extends BaseActivity implements ListaTareasFragment.OnTareaSeleccionadaListener,
        DetalleTareaFragment.OnTareaEliminadaListener, DetalleTareaFragment.OnTareaCompletadaListener {

    private static final String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> exportTxtLauncher;

    /**
     * Se ejecuta al abrir la app.
     * Monta el menú lateral, carga la lista de tareas y pide permiso de
     * notificaciones.
     *
     * @param savedInstanceState Estado guardado anterior, si lo hay.
     */
    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        exportTxtLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            exportTareasToFile(uri);
                        }
                    }
                });

        setupNavigationDrawer();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_lista_tarea, new ListaTareasFragment())
                    .commit();
        }

        solicitarPermisosNotificaciones();
        comprobarPlayServices();
    }

    /**
     * Prepara el menú lateral (Drawer) y define qué pasa al pulsar cada opción.
     */
    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.main_drawer);
        NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.nav_tareas) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_lista_tarea, new ListaTareasFragment())
                            .commit();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                } else if (id == R.id.nav_exportar) {
                    exportarTareasTxt();
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                } else if (id == R.id.nav_ajustes) {
                    Intent intent = new Intent(MainActivity.this, AjustesActivity.class);
                    startActivity(intent);
                    return true;
                }
                return false;
            }
        });
    }

    /**
     * Abre el selector del sistema para crear un archivo .txt donde se exportarán
     * las tareas.
     */
    private void exportarTareasTxt() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_TITLE, "tareas.txt");
        exportTxtLauncher.launch(intent);
    }

    /**
     * Lee las tareas de la BD y las escribe en el archivo elegido.
     * Se hace en un hilo aparte para no bloquear la pantalla.
     *
     * @param uri Ruta del archivo de destino.
     */
    private void exportTareasToFile(Uri uri) {
        DBmanager dbManager = new DBmanager(this);
        dbManager.getTareasRemoto("fechaLimite").observe(this, workInfo -> {
            if (workInfo != null && workInfo.getState().isFinished()) {
                String resultado = workInfo.getOutputData().getString("datos");
                if (resultado == null) {
                    Toast.makeText(this, "Error al obtener tareas para exportar", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(() -> {
                    try {
                        org.json.JSONObject resultJson = new org.json.JSONObject(resultado);
                        if (!resultJson.getBoolean("exito")) {
                            runOnUiThread(() -> Toast.makeText(this, "Error del servidor al exportar", Toast.LENGTH_SHORT).show());
                            return;
                        }

                        org.json.JSONArray tareasArray = resultJson.getJSONArray("tareas");
                        StringBuilder sb = new StringBuilder();
                        sb.append("MIS TAREAS\n");
                        sb.append("====================\n\n");

                        for (int i = 0; i < tareasArray.length(); i++) {
                            org.json.JSONObject tarea = tareasArray.getJSONObject(i);
                            String titulo = tarea.optString("titulo", "");
                            String desc = tarea.optString("descripcion", "");
                            String fechaBD = tarea.optString("fechaLimite", "");
                            String fechaUI = DBmanager.formatFechaToUI(fechaBD);
                            int prioridad = tarea.optInt("prioridad", 0);
                            int completada = tarea.optInt("completada", 0);

                            String strPrioridad = "Baja";
                            if (prioridad == 1) strPrioridad = "Media";
                            else if (prioridad == 2) strPrioridad = "Alta";

                            String strEstado = (completada == 1) ? "Completada" : "Pendiente";
                            fechaUI = (fechaUI != null && !fechaUI.isEmpty() && !fechaUI.equals("null")) ? fechaUI : "Sin fecha";

                            sb.append(String.format("- [%s] %s (Prioridad: %s, Límite: %s)\n", strEstado, titulo, strPrioridad, fechaUI));
                            if (desc != null && !desc.trim().isEmpty() && !desc.equals("null")) {
                                sb.append("  ").append(desc.replace("\n", "\n  ")).append("\n");
                            }

                            String direccion = tarea.optString("direccion", "null");
                            if (!direccion.equals("null") && !direccion.isEmpty()) {
                                sb.append("  📍 Ubicación: ").append(direccion).append("\n");
                            } else {
                                String lat = tarea.optString("latitud", "null");
                                String lng = tarea.optString("longitud", "null");
                                if (!lat.equals("null") && !lat.isEmpty()) {
                                    sb.append(String.format("  📍 Ubicación: Lat: %s | Lng: %s\n", lat, lng));
                                }
                            }
                            
                            sb.append("\n");
                        }

                        try (OutputStream os = getContentResolver().openOutputStream(uri);
                             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os))) {
                            writer.write(sb.toString());
                            runOnUiThread(() -> Toast.makeText(this, "Tareas exportadas correctamente", Toast.LENGTH_SHORT).show());
                        } catch (Exception e) {
                            e.printStackTrace();
                            runOnUiThread(() -> Toast.makeText(this, "Error al escribir el archivo", Toast.LENGTH_SHORT).show());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        runOnUiThread(() -> Toast.makeText(this, "Error parseando tareas para exportar", Toast.LENGTH_SHORT).show());
                    }
                }).start();
            }
        });
    }

    /**
     * Configura el botón de hamburguesa para abrir el menú lateral
     * y el botón atrás del dispositivo para cerrarlo.
     *
     * @param toolbar La Toolbar de la pantalla.
     */
    public void setupDrawerToggle(Toolbar toolbar) {
        DrawerLayout drawerLayout = findViewById(R.id.main_drawer);

        getSupportActionBar().setHomeAsUpIndicator(R.drawable.hamburger_menu);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(v -> drawerLayout.open());

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> insets);

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                DrawerLayout elmenudesplegable = findViewById(R.id.main_drawer);
                if (elmenudesplegable.isDrawerOpen(GravityCompat.START)) {
                    elmenudesplegable.closeDrawer(GravityCompat.START);
                } else {
                    finish();
                }
            }
        });
    }

    /**
     * Se llama cuando el usuario pulsa una tarea de la lista.
     * En horizontal la abre al lado; en vertical abre una pantalla nueva.
     *
     * @param tareaId ID de la tarea seleccionada.
     */
    @Override
    public void onTareaSeleccionada(long tareaId) {
        int orientacion = getResources().getConfiguration().orientation;

        if (orientacion == Configuration.ORIENTATION_LANDSCAPE) {
            DetalleTareaFragment fragment = new DetalleTareaFragment();
            Bundle args = new Bundle();
            args.putLong("tarea_id", tareaId);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_detalle, fragment)
                    .commit();
        } else {
            Intent intent = new Intent(this, ViewTareaActivity.class);
            intent.putExtra(DBmanager.COL_ID, tareaId);
            startActivity(intent);
        }
    }

    /**
     * Se llama cuando se borra una tarea.
     * Refresca la lista y quita el fragmento de detalle.
     */
    @Override
    public void onTareaEliminada() {
        ListaTareasFragment listaFragment = (ListaTareasFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_lista_tarea);
        if (listaFragment != null) {
            listaFragment.cargarTareas();
        }

        Fragment detalleFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_detalle);
        if (detalleFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(detalleFragment)
                    .commit();
        }
    }

    /**
     * Se llama cuando se marca o desmarca una tarea como completada.
     * Actualiza la lista y el detalle.
     *
     * @param tareaId ID de la tarea modificada.
     */
    @Override
    public void onTareaCompletada(long tareaId) {
        ListaTareasFragment listaFragment = (ListaTareasFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_lista_tarea);
        if (listaFragment != null) {
            listaFragment.cargarTareas();
        }

        Fragment detalleFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_detalle);
        if (detalleFragment != null) {
            DetalleTareaFragment fragment = new DetalleTareaFragment();
            Bundle args = new Bundle();
            args.putLong("tarea_id", tareaId);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_detalle, fragment)
                    .commit();

        }
    }

    /**
     * Programa una alarma diaria a las 8:00 AM para comprobar tareas pendientes
     * y mostrar una notificación si las hay.
     *
     * @param context Contexto de la app.
     */
    public static void programarAlarmaDiaria(Context context) {

        Intent intent = new Intent(context, NotificacionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, 8);
        calendario.set(Calendar.MINUTE, 0);
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);

        if (calendario.getTimeInMillis() <= System.currentTimeMillis()) {
            calendario.add(Calendar.DAY_OF_YEAR, 1);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendario.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY,
                pendingIntent);

        Log.d(TAG, "Alarma diaria programada para las 8:00 AM → próxima: " + calendario.getTime());
    }
}