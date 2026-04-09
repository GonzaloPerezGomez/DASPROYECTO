package com.example.dasproyecto.services.background;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.Observer;
import androidx.work.WorkInfo;

import com.example.dasproyecto.R;
import com.example.dasproyecto.data.db.DBmanager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProximidadService extends Service {
    private static final String TAG = "ProximidadService";
    private static final String CHANNEL_ID = "ProximidadCanal";
    public static final String ACTION_PROXIMIDAD = "com.example.dasproyecto.ACTION_PROXIMIDAD";
    public static final String EXTRA_TAREA_TITULO = "tarea_titulo";

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private DBmanager dbManager;

    private List<JSONObject> tareasActivas = new ArrayList<>();
    private List<Long> tareasNotificadasId = new ArrayList<>();

    private Observer<WorkInfo> tareasObserver;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Servicio creado");
        
        dbManager = new DBmanager(this);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        crearCanalNotificacion();
        iniciarForeground();

        cargarTareasServidor();
        iniciarLocationTracking();
    }

    private void crearCanalNotificacion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CHANNEL_ID,
                    "Servicio de Proximidad",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(canal);
            }
            
            // Canal para notificaciones de alerta (Alta prioridad)
            NotificationChannel canalAlerta = new NotificationChannel(
                    "AlertaProximidad",
                    "Alertas de Tareas Cercanas",
                    NotificationManager.IMPORTANCE_HIGH
            );
            if (manager != null) {
                manager.createNotificationChannel(canalAlerta);
            }
        }
    }

    private void iniciarForeground() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Asegurarse de que este icono existe
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Monitorizando tareas cercanas")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true)
                .setAutoCancel(false);

        Notification notification = builder.build();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            int type = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // "location" type is required from Android 14, but constant might be different. 
                // Using 8 based on ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
                type = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            } else {
                type = 8;
            }
            try {
                startForeground(1, notification, type);
            } catch (Exception e) {
                Log.e(TAG, "Error starting foreground with type", e);
                startForeground(1, notification);
            }
        } else {
            startForeground(1, notification);
        }
    }

    private void cargarTareasServidor() {
        new Thread(() -> {
            try {
                JSONObject json = dbManager.getTareasProvider("fechaLimite");
                if (json != null && json.optBoolean("exito", false)) {
                    JSONArray tareasArray = json.getJSONArray("tareas");
                    tareasActivas.clear();
                    for (int i = 0; i < tareasArray.length(); i++) {
                        JSONObject t = tareasArray.getJSONObject(i);
                        // Filtrar las completadas y las que no tengan lat/lng
                        if (t.getInt("completada") == 0 && 
                            !t.isNull("latitud") && !t.isNull("longitud") &&
                            !t.getString("latitud").isEmpty() && !t.getString("longitud").isEmpty()) {
                            
                            try {
                                double lat = t.getDouble("latitud");
                                double lng = t.getDouble("longitud");
                                tareasActivas.add(t);
                            } catch (Exception e) {
                                // ignorar tarea sin validez num
                            }
                        }
                    }
                    Log.d(TAG, "Tareas activas con ubicación cargadas: " + tareasActivas.size());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error parseando tareas", e);
            }
        }).start();
    }

    private void iniciarLocationTracking() {
        LocationRequest peticion = new LocationRequest.Builder(10000)
                .setMinUpdateIntervalMillis(5000)
                .setPriority(Priority.PRIORITY_BALANCED_POWER_ACCURACY)
                .build();

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);
                Location current = locationResult.getLastLocation();
                if (current != null) {
                    Log.d(TAG, "Ubicacion actualizada: " + current.getLatitude() + "," + current.getLongitude());
                    verificarProximidad(current);
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(peticion, locationCallback, Looper.getMainLooper());
        } catch (SecurityException e) {
            Log.e(TAG, "Faltan permisos de ubicacion", e);
        }
    }

    private void verificarProximidad(Location currentLoc) {
        for (JSONObject tarea : tareasActivas) {
            try {
                long id = tarea.getLong("id");
                // Evitamos notificar de nuevo la misma tarea repetidamente
                if (tareasNotificadasId.contains(id)) continue;

                double lat = tarea.getDouble("latitud");
                double lng = tarea.getDouble("longitud");
                String titulo = tarea.getString("titulo");

                Location tareaLoc = new Location("");
                tareaLoc.setLatitude(lat);
                tareaLoc.setLongitude(lng);

                float dist = currentLoc.distanceTo(tareaLoc);
                if (dist < 200.0f) {
                    Log.i(TAG, "¡Tarea cerca! " + titulo + " a " + dist + " metros");
                    tareasNotificadasId.add(id);

                    // 1. Enviar Broadcast
                    Intent broadcastIntent = new Intent(ACTION_PROXIMIDAD);
                    broadcastIntent.putExtra(EXTRA_TAREA_TITULO, titulo);
                    sendBroadcast(broadcastIntent);

                    // 2. Mostrar Notificación de alta prioridad
                    mostrarNotificacionAlerta(titulo, (int) dist);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void mostrarNotificacionAlerta(String titulo, int distanciaMts) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, "AlertaProximidad")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("¡Tarea Pendiente Cercana!")
                .setContentText("A " + distanciaMts + " m: " + titulo)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand ejecutado");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Servicio detenido");
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
