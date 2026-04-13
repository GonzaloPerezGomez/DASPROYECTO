package com.example.dasproyecto.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.preference.PreferenceManager;

import com.example.dasproyecto.R;
import com.example.dasproyecto.data.db.DBmanager;
import com.example.dasproyecto.data.db.AppDatabase;
import com.example.dasproyecto.data.db.TareaDao;
import com.example.dasproyecto.data.db.TareaEntity;
import com.example.dasproyecto.ui.activities.LoginActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Proveedor del Widget de Tareas
 * Lee las próximas 3 tareas desde la base de datos local Room de forma síncrona
 * y actualiza la UI del widget.
 */
public class TareasWidgetProvider extends AppWidgetProvider {

    private static final String TAG = "TareasWidgetProvider";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate: Refrescando widget...");

        // Ejecutar en segundo plano (Room no permite consultas en el hilo principal)
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(() -> {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            int userId = prefs.getInt("session_user_id", -1);

            List<TareaEntity> tareasPendientes = new ArrayList<>();

            if (userId != -1) {
                // Obtener tareas ordenadas por fecha
                TareaDao dao = AppDatabase.getInstance(context).tareaDao();
                List<TareaEntity> todas = dao.getTareasPorFecha(userId);

                // Filtrar solo las pendientes y limitar a 3
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String fechaHoyDB = sdf.format(Calendar.getInstance().getTime());

                for (TareaEntity t : todas) {
                    if (t.completada == 0 && t.fechaLimite != null && !t.fechaLimite.isEmpty()) {
                        if (t.fechaLimite.compareTo(fechaHoyDB) >= 0) { // Solo futuras o de hoy
                            tareasPendientes.add(t);
                            if (tareasPendientes.size() >= 3) break;
                        }
                    }
                }
            }

            // Actualizar todos los widgets
            for (int appWidgetId : appWidgetIds) {
                updateAppWidget(context, appWidgetManager, appWidgetId, tareasPendientes);
            }
        });
    }

    /**
     * Actualiza un solo widget con la lista de tareas.
     */
    private void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                 int appWidgetId, List<TareaEntity> tareas) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_tareas);

        // 1. Configurar Click para abrir LoginActivity
        Intent intent = new Intent(context, LoginActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 
                0, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        views.setOnClickPendingIntent(R.id.widget_root, pendingIntent);

        // 2. Ocultar todos por defecto
        views.setViewVisibility(R.id.widget_tarea1_container, View.GONE);
        views.setViewVisibility(R.id.widget_tarea2_container, View.GONE);
        views.setViewVisibility(R.id.widget_tarea3_container, View.GONE);
        views.setViewVisibility(R.id.widget_empty_view, View.GONE);

        // 3. Rellenar datos
        if (tareas.isEmpty()) {
            views.setViewVisibility(R.id.widget_empty_view, View.VISIBLE);
        } else {
            if (tareas.size() > 0) {
                views.setViewVisibility(R.id.widget_tarea1_container, View.VISIBLE);
                views.setTextViewText(R.id.widget_tarea1_titulo, tareas.get(0).titulo);
                views.setTextViewText(R.id.widget_tarea1_fecha, DBmanager.formatFechaToUI(tareas.get(0).fechaLimite));
            }
            if (tareas.size() > 1) {
                views.setViewVisibility(R.id.widget_tarea2_container, View.VISIBLE);
                views.setTextViewText(R.id.widget_tarea2_titulo, tareas.get(1).titulo);
                views.setTextViewText(R.id.widget_tarea2_fecha, DBmanager.formatFechaToUI(tareas.get(1).fechaLimite));
            }
            if (tareas.size() > 2) {
                views.setViewVisibility(R.id.widget_tarea3_container, View.VISIBLE);
                views.setTextViewText(R.id.widget_tarea3_titulo, tareas.get(2).titulo);
                views.setTextViewText(R.id.widget_tarea3_fecha, DBmanager.formatFechaToUI(tareas.get(2).fechaLimite));
            }
        }

        // 4. Decirle al Manager que aplique los cambios
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
}
