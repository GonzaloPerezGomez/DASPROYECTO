package com.example.dasproyecto;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.dasproyecto.db.DBmanager;
import com.example.dasproyecto.fragment.ListaTareasFragment;

/**
 * Adaptador del RecyclerView que muestra la lista de tareas.
 * Coge los datos de un Cursor y los pinta en cada fila,
 * aplicando colores según la prioridad y tachando las completadas.
 */
public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private Context context;
    private Cursor cursor;
    private ListaTareasFragment.OnTareaSeleccionadaListener listener;

    /**
     * Constructor del adaptador.
     *
     * @param context  Contexto actual.
     * @param cursor   Cursor con las tareas de la BD.
     * @param listener Listener para cuando el usuario pulsa una tarea.
     */
    public TareasAdapter(Context context, Cursor cursor, ListaTareasFragment.OnTareaSeleccionadaListener listener) {
        this.context = context;
        this.cursor = cursor;
        this.listener = listener;
    }

    /**
     * Cambia el cursor por uno nuevo y refresca la lista.
     * Cierra el cursor anterior para no dejar nada abierto.
     *
     * @param newCursor El nuevo cursor con los datos actualizados.
     */
    public void updateCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
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
        if (!cursor.moveToPosition(position)) {
            return;
        }

        String titulo = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO));
        String descripcion = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION));
        String fechaBD = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE));
        String fechaUI = DBmanager.formatFechaToUI(fechaBD);
        int prioridad = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_PRIORIDAD));
        long id = cursor.getLong(cursor.getColumnIndexOrThrow(DBmanager.COL_ID));
        int completada = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_COMPLETADA));

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

        if (descripcion != null && !descripcion.trim().isEmpty()) {
            holder.tvDescripcion.setText(descripcion);
            holder.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescripcion.setVisibility(View.GONE);
        }

        if (fechaUI != null && !fechaUI.trim().isEmpty()) {
            holder.tvFecha.setText(fechaUI);
            holder.divider.setVisibility(View.VISIBLE);
            holder.layoutFecha.setVisibility(View.VISIBLE);
        } else {
            holder.divider.setVisibility(View.GONE);
            holder.layoutFecha.setVisibility(View.GONE);
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
     * Devuelve cuántas tareas hay en el cursor.
     */
    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
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
        long id = -1;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            divider = itemView.findViewById(R.id.divider);
            layoutFecha = itemView.findViewById(R.id.layoutFecha);
            cardView = itemView.findViewById(R.id.cardViewTarea);
        }
    }
}
