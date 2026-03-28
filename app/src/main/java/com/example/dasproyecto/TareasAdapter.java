package com.example.dasproyecto;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.ListaTareasFragment;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adaptador del RecyclerView que muestra la lista de tareas.
 * Coge los datos de un JSONArray (servidor) y los pinta en cada fila,
 * aplicando colores según la prioridad y tachando las completadas.
 */
public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private Context context;
    private List<JSONObject> tareasList;
    private List<JSONObject> tareasOriginales;
    private ListaTareasFragment.OnTareaSeleccionadaListener listener;

    /**
     * Constructor del adaptador.
     *
     * @param context  Contexto actual.
     * @param listener Listener para cuando el usuario pulsa una tarea.
     */
    public TareasAdapter(Context context, ListaTareasFragment.OnTareaSeleccionadaListener listener) {
        this.context = context;
        this.listener = listener;
        this.tareasList = new ArrayList<>();
        this.tareasOriginales = new ArrayList<>();
    }

    /**
     * Reemplaza la lista actual de tareas con los datos del servidor (JSON).
     *
     * @param jsonArray Array JSON devuelto por php
     */
    public void setTareas(JSONArray jsonArray) {
        tareasList.clear();
        tareasOriginales.clear();
        try {
            if (jsonArray != null) {
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject obj = jsonArray.getJSONObject(i);
                    tareasList.add(obj);
                    tareasOriginales.add(obj);
                }
            }
        } catch (Exception e) {
            Log.e("TareasAdapter", "Error al parsear array de tareas", e);
        }
        notifyDataSetChanged();
    }

    /**
     * Filtra la lista localmente al escribir en el buscador
     */
    public void filtrar(String texto) {
        tareasList.clear();
        if (texto == null || texto.isEmpty()) {
            tareasList.addAll(tareasOriginales);
        } else {
            String q = texto.toLowerCase(Locale.getDefault());
            for (JSONObject t : tareasOriginales) {
                String tit = t.optString("titulo", "").toLowerCase(Locale.getDefault());
                String desc = t.optString("descripcion", "").toLowerCase(Locale.getDefault());
                if (tit.contains(q) || desc.contains(q)) {
                    tareasList.add(t);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * Crea el ViewHolder inflando el layout de cada fila.
     */
    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    /**
     * Rellena una fila con los datos de la tarea correspondiente.
     * Pone el título, descripción, fecha, color de prioridad y tachado si está
     * completada.
     *
     * @param holder   ViewHolder de la fila.
     * @param position Posición de la fila en la lista.
     */
    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        JSONObject tarea = tareasList.get(position);

        String titulo = tarea.optString("titulo", "");
        String descripcion = tarea.optString("descripcion", "");
        String fechaBD = tarea.optString("fechaLimite", "");
        if (fechaBD.equals("null") || fechaBD.isEmpty()) fechaBD = "";
        String fechaUI = DBmanager.formatFechaToUI(fechaBD);
        int prioridad = tarea.optInt("prioridad", 0);
        long id = tarea.optLong("id", -1);
        int completada = tarea.optInt("completada", 0);

        if (completada == 1) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0")); // Gris claro
            holder.tvTitulo
                    .setPaintFlags(holder.tvTitulo.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTitulo
                    .setPaintFlags(holder.tvTitulo.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.tvTitulo.setText(titulo);

        if (descripcion != null && !descripcion.trim().isEmpty() && !descripcion.equals("null")) {
            holder.tvDescripcion.setText(descripcion);
            holder.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescripcion.setVisibility(View.GONE);
        }

        if (fechaUI != null && !fechaUI.trim().isEmpty() && !fechaUI.equals("null")) {
            holder.tvFecha.setText(fechaUI);
            holder.divider.setVisibility(View.VISIBLE);
            holder.layoutFecha.setVisibility(View.VISIBLE);
        } else {
            holder.divider.setVisibility(View.GONE);
            holder.layoutFecha.setVisibility(View.GONE);
        }

        String latitudStr = tarea.optString("latitud", "null");
        String longitudStr = tarea.optString("longitud", "null");
        String direccionStr = tarea.optString("direccion", "null");
        
        if (!latitudStr.equals("null") && !latitudStr.isEmpty() || (!direccionStr.equals("null") && !direccionStr.isEmpty())) {
            holder.ivIconoUbicacion.setVisibility(View.VISIBLE);
            holder.ivIconoUbicacion.setOnClickListener(v -> {
                try {
                    String uriText;
                    if (!direccionStr.equals("null") && !direccionStr.isEmpty()) {
                        uriText = "geo:0,0?q=" + android.net.Uri.encode(direccionStr);
                    } else {
                        uriText = "geo:" + latitudStr + "," + longitudStr + "?q=" + latitudStr + "," + longitudStr;
                    }
                    android.content.Intent mapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriText));
                    mapIntent.setPackage("com.google.android.apps.maps");
                    if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
                        context.startActivity(mapIntent);
                    } else {
                        android.content.Intent genericMapIntent = new android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(uriText));
                        context.startActivity(genericMapIntent);
                    }
                } catch (Exception e) {
                    android.widget.Toast.makeText(context, "No se pudo abrir el mapa", android.widget.Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            holder.ivIconoUbicacion.setVisibility(View.GONE);
            holder.ivIconoUbicacion.setOnClickListener(null);
        }

        holder.id = id;

        int color;
        switch (prioridad) {
            case 2:
                color = Color.RED;
                break;
            case 1:
                color = Color.rgb(255, 165, 0);
                break;
            default:
                color = Color.BLACK;
                break;
        }

        holder.tvTitulo.setTextColor(color);

        holder.itemView.setOnClickListener(v -> {
            Log.d("TareasAdapter", "Tarea seleccionada: " + holder.getAdapterPosition());
            if (listener != null) {
                listener.onTareaSeleccionada(holder.id);
            }
        });
    }

    /**
     * Devuelve cuántas tareas hay en el array.
     */
    @Override
    public int getItemCount() {
        return tareasList == null ? 0 : tareasList.size();
    }

    /**
     * ViewHolder que guarda las referencias a las vistas de cada fila
     * para no tener que buscarlas cada vez.
     */
    static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFecha;
        View divider;
        LinearLayout layoutFecha;
        CardView cardView;
        ImageView ivIconoUbicacion;
        long id = -1;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            divider = itemView.findViewById(R.id.divider);
            layoutFecha = itemView.findViewById(R.id.layoutFecha);
            cardView = itemView.findViewById(R.id.cardViewTarea);
            ivIconoUbicacion = itemView.findViewById(R.id.ivIconoUbicacion);
        }
    }
}
