package com.example.dasproyecto;

import android.Manifest;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.dasproyecto.notification.NotificacionReceiver;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import com.google.android.material.navigation.NavigationView;

import android.util.Log;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dasproyecto.db.DBmanager;
import android.database.Cursor;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private DBmanager dbManager;
    private TareasAdapter adapter;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ViewCompat.setOnApplyWindowInsetsListener(toolbar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), systemBars.top, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        DrawerLayout drawerLayout = findViewById(R.id.main_drawer);
        NavigationView navigationView = findViewById(R.id.nav_view);
        FloatingActionButton fab = findViewById(R.id.fabAddTarea);
        RecyclerView recyclerView = findViewById(R.id.recyclerViewTareas);

        // Initialize DB and Adapter
        dbManager = new DBmanager(this);
        dbManager.open();
        Cursor cursor = dbManager.getTareas();

        adapter = new TareasAdapter(this, cursor);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fab.setOnClickListener(v -> {
            Log.d(TAG, "Botón flotante presionado - Abriendo AddTareaActivity");
            Intent intent = new Intent(this, AddTareaActivity.class);
            startActivity(intent);
        });

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> insets);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);

        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // --- Pedir permiso de notificaciones (obligatorio en Android 13+) ---
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.POST_NOTIFICATIONS }, 0);
        }

        // --- Crear el canal de notificaciones (obligatorio en Android 8+) ---
        // Un canal agrupa notificaciones y el usuario puede configurar
        // sonido, vibración, etc. por separado para cada canal.
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "tareas_channel", // ID único del canal
                    "Tareas", // Nombre visible en Ajustes > Notificaciones
                    NotificationManager.IMPORTANCE_DEFAULT);
            manager.createNotificationChannel(channel);
        }

        // --- Programar la alarma diaria a las 8:00 AM ---
        // La alarma se configura aquí la primera vez que se abre la app.
        // Si ya estaba programada, AlarmManager la reemplaza (mismo PendingIntent).
        programarAlarmaDiaria(this);
    }

    public void refreshTareas() {
        if (dbManager != null) {
            Cursor newCursor = dbManager.getTareas();
            if (adapter != null) {
                adapter.updateCursor(newCursor);
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshTareas();
    }

    /**
     * Programa una alarma repetitiva que se dispara cada día a las 8:00 AM.
     *
     * Funcionamiento paso a paso:
     * 1. Crear un PendingIntent que apunta a NotificacionReceiver.
     * - PendingIntent es como un "cheque en blanco": le dice a Android
     * "cuando llegue el momento, ejecuta este Intent".
     * 2. Configurar un Calendar a las 8:00 AM de hoy.
     * - Si ya pasaron las 8:00, se programa para mañana a las 8:00.
     * 3. Usar AlarmManager.setInexactRepeating() para programar la alarma.
     * - setInexactRepeating es más eficiente que setRepeating: permite a
     * Android agrupar alarmas de distintas apps para ahorrar batería.
     * - INTERVAL_DAY hace que se repita cada 24 horas.
     *
     * Este método es static para poder llamarlo desde BootReceiver
     * sin necesidad de tener una instancia de MainActivity.
     *
     * @param context Contexto de la app (Activity o BroadcastReceiver)
     */
    public static void programarAlarmaDiaria(Context context) {
        // --- Paso 1: Crear el PendingIntent ---
        // Este Intent apunta a NotificacionReceiver: cuando la alarma salte,
        // Android ejecutará el onReceive() de NotificacionReceiver.
        Intent intent = new Intent(context, NotificacionReceiver.class);

        // PendingIntent.getBroadcast() crea un PendingIntent para un BroadcastReceiver.
        // - requestCode = 0: identificador de este PendingIntent (para poder cancelarlo
        // después).
        // - FLAG_UPDATE_CURRENT: si ya existe un PendingIntent igual, lo actualiza.
        // - FLAG_IMMUTABLE: obligatorio en Android 12+, indica que el Intent no se
        // puede modificar.
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // --- Paso 2: Configurar la hora de la alarma (8:00 AM) ---
        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, 8); // 8 de la mañana
        calendario.set(Calendar.MINUTE, 0);
        calendario.set(Calendar.SECOND, 0);
        calendario.set(Calendar.MILLISECOND, 0);

        // Si ya pasaron las 8:00 hoy, programar para mañana
        // (si no, la alarma se dispararía inmediatamente)
        if (calendario.getTimeInMillis() <= System.currentTimeMillis()) {
            calendario.add(Calendar.DAY_OF_YEAR, 1);
        }

        // --- Paso 3: Programar la alarma con AlarmManager ---
        // AlarmManager es un servicio del sistema que gestiona alarmas.
        // setInexactRepeating: la alarma puede desviarse unos minutos para
        // agruparse con otras alarmas y ahorrar batería.
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP, // Tipo: despertar el dispositivo si está dormido
                calendario.getTimeInMillis(), // Cuándo: primera ejecución
                AlarmManager.INTERVAL_DAY, // Repetición: cada 24 horas
                pendingIntent // Qué hacer: ejecutar NotificacionReceiver
        );

        Log.d(TAG, "Alarma diaria programada para las 8:00 AM → próxima: " + calendario.getTime());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbManager != null) {
            dbManager.close();
        }
    }

}