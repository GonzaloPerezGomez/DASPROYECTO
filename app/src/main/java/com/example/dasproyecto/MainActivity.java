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

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.widget.Toolbar;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.dasproyecto.fragment.DetalleTareaFragment;
import com.example.dasproyecto.fragment.ListaTareasFragment;
import com.example.dasproyecto.notification.NotificacionReceiver;

import android.util.Log;

import com.example.dasproyecto.db.DBmanager;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements ListaTareasFragment.OnTareaSeleccionadaListener,
        DetalleTareaFragment.OnTareaEliminadaListener, DetalleTareaFragment.OnTareaCompletadaListener {

    private static final int NOTIFICACION_CODE = 0;
    private static final String TAG = "MainActivity";

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Cargar el ListaTareasFragment solo si es la primera vez (no en rotaciones)
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_lista_tarea, new ListaTareasFragment())
                    .commit();
        }

        // Permisos de notificación
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.POST_NOTIFICATIONS }, NOTIFICACION_CODE);
        }
    }

    /**
     * Configura el DrawerLayout con el Toolbar después de que el Fragment lo
     * establezca como ActionBar.
     * Se llama desde el Fragment después de setSupportActionBar().
     */
    public void setupDrawerToggle(Toolbar toolbar) {
        DrawerLayout drawerLayout = findViewById(R.id.main_drawer);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        ViewCompat.setOnApplyWindowInsetsListener(drawerLayout, (v, insets) -> insets);
    }

    @Override
    public void onTareaSeleccionada(long tareaId) {
        int orientacion = getResources().getConfiguration().orientation;

        if (orientacion == Configuration.ORIENTATION_LANDSCAPE) {
            // Landscape: crear nueva instancia y mostrar en el panel derecho
            DetalleTareaFragment fragment = new DetalleTareaFragment();
            Bundle args = new Bundle();
            args.putLong("tarea_id", tareaId);
            fragment.setArguments(args);

            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_detalle, fragment)
                    .commit();
        } else {
            // Portrait: abrir ViewTareaActivity
            Intent intent = new Intent(this, ViewTareaActivity.class);
            intent.putExtra(DBmanager.COL_ID, tareaId);
            startActivity(intent);
        }
    }

    @Override
    public void onTareaEliminada() {
        // Refrescar la lista
        ListaTareasFragment listaFragment = (ListaTareasFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_lista_tarea);
        if (listaFragment != null) {
            listaFragment.cargarTareas();
        }
        // Quitar el detalle del panel derecho
        Fragment detalleFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_detalle);
        if (detalleFragment != null) {
            getSupportFragmentManager().beginTransaction()
                    .remove(detalleFragment)
                    .commit();
        }
    }

    @Override
    public void onTareaCompletada(long tareaId) {
        ListaTareasFragment listaFragment = (ListaTareasFragment) getSupportFragmentManager()
                .findFragmentById(R.id.fragment_lista_tarea);
        if (listaFragment != null) {
            listaFragment.cargarTareas();
        }
        // Quitar el detalle del panel derecho
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

    public static void programarAlarmaDiaria(Context context) {

        Intent intent = new Intent(context, NotificacionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendario = Calendar.getInstance();
        calendario.set(Calendar.HOUR_OF_DAY, 8); // 8 de la mañana
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

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case NOTIFICACION_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permiso concedido");

                    NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        NotificationChannel channel = new NotificationChannel(
                                "tareas_channel",
                                "Tareas",
                                NotificationManager.IMPORTANCE_DEFAULT);
                        manager.createNotificationChannel(channel);
                    }
                    programarAlarmaDiaria(this);
                }
            }
        }
    }
}