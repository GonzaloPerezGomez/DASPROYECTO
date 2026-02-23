package com.example.dasproyecto;

import android.content.Context;
import android.content.Intent;
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

public class TareasAdapter extends RecyclerView.Adapter<TareasAdapter.TareaViewHolder> {

    private Context context;
    private Cursor cursor;

    public TareasAdapter(Context context, Cursor cursor) {
        this.context = context;
        this.cursor = cursor;
    }

    public void updateCursor(Cursor newCursor) {
        if (cursor != null) {
            cursor.close();
        }
        cursor = newCursor;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public TareaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tarea, parent, false);
        return new TareaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TareaViewHolder holder, int position) {
        if (!cursor.moveToPosition(position)) {
            return;
        }

        String titulo = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_TITULO));
        String descripcion = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_DESCRIPCION));
        String fecha = cursor.getString(cursor.getColumnIndexOrThrow(DBmanager.COL_FECHALIMITE));
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

        // Descripción: ocultar si vacía
        if (descripcion != null && !descripcion.trim().isEmpty()) {
            holder.tvDescripcion.setText(descripcion);
            holder.tvDescripcion.setVisibility(View.VISIBLE);
        } else {
            holder.tvDescripcion.setVisibility(View.GONE);
        }

        // Fecha + separador: ocultar si vacía
        if (fecha != null && !fecha.trim().isEmpty()) {
            holder.tvFecha.setText(fecha);
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

    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

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
            itemView.setOnClickListener(v -> {
                Log.d("TareasAdapter", "Tarea seleccionada: " + getAdapterPosition());
                Intent intent = new Intent(v.getContext(), ViewTareaActivity.class);
                intent.putExtra(DBmanager.COL_ID, id);
                v.getContext().startActivity(intent);
            });
        }
    }
}
