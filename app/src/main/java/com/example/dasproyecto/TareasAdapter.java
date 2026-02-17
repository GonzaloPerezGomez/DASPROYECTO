package com.example.dasproyecto;

import static androidx.core.content.ContextCompat.startActivity;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.dasproyecto.Dialogs.EliminarTareaDialog;
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
        int completada = cursor.getInt(cursor.getColumnIndexOrThrow(DBmanager.COL_COMPLETADA));

        if (completada == 1) {
            holder.cardView.setCardBackgroundColor(Color.parseColor("#E0E0E0")); // Gris claro
            holder.tvTitulo.setPaintFlags(holder.tvTitulo.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG); // Opcional: Tachado
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTitulo.setPaintFlags(holder.tvTitulo.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG)); // Quitar tachado
        }

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
                    DBmanager dbManager = new DBmanager(context);
                    dbManager.open();
                    dbManager.actualizarEstado(id, 1);
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).onResume(); // Quick way to refresh
                    }
                    return true;

                } else if (itemId == R.id.action_eliminar) {
                    EliminarTareaDialog dialogo = EliminarTareaDialog.newInstance(id, titulo, new EliminarTareaDialog.ConfirmacionListener() {
                        @Override
                        public void onTareaEliminada() {
                            // Mantenemos tu lógica de refresco original
                            if (context instanceof MainActivity) {
                                ((MainActivity) context).onResume();
                            }
                            Toast.makeText(context, "Tarea eliminada correctamente", Toast.LENGTH_SHORT).show();
                        }
                    });

                    // Mostramos el diálogo usando el FragmentManager de la Activity
                    dialogo.show(((AppCompatActivity) context).getSupportFragmentManager(), "EliminarTareaDialog");

                    return true;
                } else if (itemId == R.id.action_editar) {
                    // Logic to edite task
                    Intent intent = new Intent(context, EditTareaActivity.class);
                    Log.i("EditTareaActivity", "Editando tarea con ID:" + id);
                    intent.putExtra(DBmanager.COL_ID, id);
                    context.startActivity(intent);
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).onResume(); // Quick way to refresh
                    }
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
        CardView cardView;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnMaps = itemView.findViewById(R.id.btnMaps);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            cardView = itemView.findViewById(R.id.cardViewTarea);
        }
    }
}
