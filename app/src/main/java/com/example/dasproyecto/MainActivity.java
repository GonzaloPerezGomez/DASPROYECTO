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
import android.content.BroadcastReceiver;
import android.content.IntentFilter;

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
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import androidx.core.content.FileProvider;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import com.bumptech.glide.Glide;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import android.provider.Settings;

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
    private ActivityResultLauncher<Uri> takePictureLauncher;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Uri photoUri;
    private String currentPhotoPath;
    private com.google.android.material.imageview.ShapeableImageView ivPerfil;

    private BroadcastReceiver proximidadReceiver;
    private boolean isProximidadActivo = false;

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

        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    if (isGranted) {
                        dispatchTakePictureIntent();
                    } else {
                        Toast.makeText(this, "Permiso de cámara denegado", Toast.LENGTH_SHORT).show();
                    }
                });

        takePictureLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicture(),
                success -> {
                    if (success) {
                        subirFotoServidor();
                    }
                });

        setupNavigationDrawer();

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_lista_tarea, new ListaTareasFragment())
                    .commit();
        }

        proximidadReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ProximidadService.ACTION_PROXIMIDAD.equals(intent.getAction())) {
                    String titulo = intent.getStringExtra(ProximidadService.EXTRA_TAREA_TITULO);
                    Toast.makeText(MainActivity.this, "📍 ¡Estás muy cerca de: " + titulo + "!", Toast.LENGTH_LONG).show();
                }
            }
        };

        solicitarPermisosNotificaciones();
        comprobarPlayServices();
        programarSincronizacionPeriodica();
        ejecutarSyncInicial();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(proximidadReceiver, new IntentFilter(ProximidadService.ACTION_PROXIMIDAD), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(proximidadReceiver, new IntentFilter(ProximidadService.ACTION_PROXIMIDAD));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(proximidadReceiver);
    }

    /**
     * Prepara el menú lateral (Drawer) y define qué pasa al pulsar cada opción.
     */
    private void setupNavigationDrawer() {
        DrawerLayout drawerLayout = findViewById(R.id.main_drawer);
        NavigationView navigationView = findViewById(R.id.nav_view);

        // Acceder a la cabecera para configurar la foto de perfil
        android.view.View headerView = navigationView.getHeaderView(0);
        ivPerfil = headerView.findViewById(R.id.ivPerfil);
        android.widget.TextView tvUserName = headerView.findViewById(R.id.textViewUserName);
        android.widget.TextView tvUserEmail = headerView.findViewById(R.id.textViewUserEmail);
        
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        tvUserName.setText(pref.getString("session_user_name", "Usuario"));
        tvUserEmail.setText(pref.getString("session_user_email", "email@ejemplo.com"));
        
        cargarFotoPerfilLocal();

        ivPerfil.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                dispatchTakePictureIntent();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

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
                } else if (id == R.id.nav_proximidad) {
                    toggleProximidadService(item);
                    drawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
                return false;
            }
        });
    }

    private void toggleProximidadService(MenuItem item) {
        if (!isProximidadActivo) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 101);
                Toast.makeText(this, "Se requiere permiso de ubicación. Inténtalo de nuevo tras concederlo.", Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && 
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_BACKGROUND_LOCATION}, 102);
                Toast.makeText(this, "Se requiere permitir ubicación TODO EL TIEMPO. Inténtalo de nuevo tras concederlo.", Toast.LENGTH_LONG).show();
                return;
            }
            
            Intent intent = new Intent(this, ProximidadService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent);
            } else {
                startService(intent);
            }
            isProximidadActivo = true;
            item.setTitle("Detener Servicio Proximidad");
            Toast.makeText(this, "Servicio iniciado", Toast.LENGTH_SHORT).show();
        } else {
            Intent intent = new Intent(this, ProximidadService.class);
            stopService(intent);
            isProximidadActivo = false;
            item.setTitle("Iniciar Servicio Proximidad");
            Toast.makeText(this, "Servicio detenido", Toast.LENGTH_SHORT).show();
        }
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
        new Thread(() -> {
            try {
                org.json.JSONObject resultJson = dbManager.getTareasProvider("fechaLimite");
                if (resultJson == null || !resultJson.has("exito")) {
                    runOnUiThread(() -> Toast.makeText(this, "Error al obtener tareas para exportar", Toast.LENGTH_SHORT).show());
                    return;
                }

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

    /**
     * Lanza el intent de la cámara tras preparar el archivo donde se guardará la foto.
     */
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        try {
            File photoFile = createImageFile();
            if (photoFile != null) {
                photoUri = FileProvider.getUriForFile(this,
                        getApplicationContext().getPackageName() + ".fileprovider",
                        photoFile);
                takePictureLauncher.launch(photoUri);
            }
        } catch (IOException ex) {
            Log.e(TAG, "Error al crear el archivo de imagen", ex);
            Toast.makeText(this, "Error al preparar la cámara", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Crea un archivo temporal en el almacenamiento privado de la app.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    /**
     * Carga la foto de perfil si ya existe una URL guardada.
     */
    private void cargarFotoPerfilLocal() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String fotoUrl = prefs.getString("session_user_foto", null);
        if (fotoUrl != null && !fotoUrl.isEmpty() && !fotoUrl.equalsIgnoreCase("null")) {
            Glide.with(this)
                .load(fotoUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(ivPerfil);
        } else {
            ivPerfil.setImageResource(R.drawable.ic_launcher_background);
        }
    }

    /**
     * Convierte la imagen capturada a Base64 y llama al DBmanager para subirla.
     */
    private void subirFotoServidor() {
        if (currentPhotoPath == null) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("session_user_id", -1);

        if (userId != -1) {
            Toast.makeText(this, "Subiendo foto...", Toast.LENGTH_SHORT).show();
            DBmanager db = new DBmanager(this);
            db.actualizarFotoPerfilRemoto(userId, currentPhotoPath).observe(this, workInfo -> {
                if (workInfo != null && workInfo.getState().isFinished()) {
                    String resultado = workInfo.getOutputData().getString("datos");
                    try {
                        org.json.JSONObject json = new org.json.JSONObject(resultado);
                        if (json.getBoolean("exito")) {
                            String fotoUrl = json.isNull("foto_url") ? "" : json.optString("foto_url", "");
                            prefs.edit().putString("session_user_foto", fotoUrl).apply();
                            cargarFotoPerfilLocal();
                            Toast.makeText(this, "Foto actualizada", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Error: " + json.getString("mensaje"), Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parseando respuesta foto", e);
                    }
                }
            });
        }
    }

    /**
     * Programa una sincronización periódica cada 15 minutos con el servidor.
     * Usa ExistingPeriodicWorkPolicy.KEEP para no duplicar si ya existe.
     */
    private void programarSincronizacionPeriodica() {
        androidx.work.PeriodicWorkRequest syncRequest =
                new androidx.work.PeriodicWorkRequest.Builder(SyncWorker.class, 15, java.util.concurrent.TimeUnit.MINUTES)
                        .build();

        androidx.work.WorkManager.getInstance(this).enqueueUniquePeriodicWork(
                "sync_tareas_periodica",
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                syncRequest
        );
        Log.d(TAG, "Sincronización periódica programada cada 15 minutos");
    }

    /**
     * Ejecuta una sincronización inmediata al abrir la app para que Room esté actualizado.
     */
    private void ejecutarSyncInicial() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        int userId = prefs.getInt("session_user_id", -1);
        if (userId != -1) {
            androidx.work.OneTimeWorkRequest syncRequest =
                    new androidx.work.OneTimeWorkRequest.Builder(SyncWorker.class).build();
            androidx.work.WorkManager.getInstance(this).enqueue(syncRequest);
            Log.d(TAG, "Sincronización inicial lanzada");
        }
    }
}