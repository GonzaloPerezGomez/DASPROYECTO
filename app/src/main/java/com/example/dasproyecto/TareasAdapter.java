package com.example.dasproyecto;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.PopupMenu;
import android.widget.Toast;

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
        if (newCursor != null) {
            notifyDataSetChanged();
        }
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

        holder.tvTitulo.setText(titulo);
        holder.tvDescripcion.setText(descripcion);
        holder.tvFecha.setText("📅 " + fecha);

        // Optional: Change color based on priority
        // Simple visual indicator: High priority (2) -> Red, Medium (1) -> Orange/Yellow, Low (0) -> Green/Default
        int color;
        switch (prioridad) {
            case 2: color = Color.RED; break;
            case 1: color = Color.rgb(255, 165, 0); break; // Orange
            default: color = Color.GREEN; break; // Green
        }
        holder.tvTitulo.setTextColor(color);

        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.inflate(R.menu.menu_item_tarea);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_completar) {
                    // Logic to complete task
                    Toast.makeText(context, "Completar tarea: " + titulo, Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.action_eliminar) {
                    // Logic to delete task
                    new AlertDialog.Builder(context)
                        .setTitle("Eliminar tarea")
                        .setMessage("¿Estás seguro de que quieres eliminar la tarea '" + titulo + "'?")
                        .setPositiveButton("Eliminar", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DBmanager dbManager = new DBmanager(context);
                                dbManager.open();
                                dbManager.eliminar(id);
                                dbManager.close();
                                
                                // Refresh logic
                                if (context instanceof MainActivity) {
                                    ((MainActivity) context).onResume(); // Quick way to refresh
                                }
                                Toast.makeText(context, "Tarea eliminada", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
                    
                    return true;
                } else if (itemId == R.id.action_editar) {
                    // Logic to delete task
                    Toast.makeText(context, "Editar tarea: " + titulo, Toast.LENGTH_SHORT).show();
                    return true;
                }

                return false;
            });
            popup.show();
        });
    }

    @Override
    public int getItemCount() {
        return cursor == null ? 0 : cursor.getCount();
    }

    static class TareaViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvDescripcion, tvFecha;
        ImageView btnMaps, btnMenu;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnMaps = itemView.findViewById(R.id.btnMaps);
            btnMenu = itemView.findViewById(R.id.btnMenu);
        }
    }
}
