package com.example.dasproyecto;

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

import android.widget.PopupMenu;
import android.widget.Toast;

import com.example.dasproyecto.dialog.EliminarTareaDialog;
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
            holder.tvTitulo
                    .setPaintFlags(holder.tvTitulo.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
        } else {
            holder.cardView.setCardBackgroundColor(Color.WHITE);
            holder.tvTitulo
                    .setPaintFlags(holder.tvTitulo.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
        }

        holder.tvTitulo.setText(titulo);
        holder.tvDescripcion.setText(descripcion);
        holder.tvFecha.setText(fecha);

        int color;
        switch (prioridad) {
            case 2:
                color = Color.RED;
                break;
            case 1:
                color = Color.rgb(255, 165, 0);
                break;
            default:
                color = Color.GREEN;
                break;
        }
        holder.tvTitulo.setTextColor(color);

        holder.btnMenu.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, holder.btnMenu);
            popup.inflate(R.menu.menu_item_tarea);
            popup.setOnMenuItemClickListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.action_completar) {
                    DBmanager dbManager = new DBmanager(context);
                    dbManager.open();
                    dbManager.actualizarEstado(id, 1);
                    dbManager.close();
                    Toast.makeText(context, "Tarea '" + titulo + "' completada", Toast.LENGTH_SHORT).show();
                    if (context instanceof MainActivity) {
                        ((MainActivity) context).refreshTareas();
                    }
                    return true;

                } else if (itemId == R.id.action_eliminar) {
                    // El diálogo se encarga de la eliminación, el Toast y el refresco tras
                    // confirmar
                    EliminarTareaDialog dialogo = EliminarTareaDialog.newInstance(id, titulo);
                    dialogo.show(((AppCompatActivity) context).getSupportFragmentManager(), "EliminarTareaDialog");
                    return true;

                } else if (itemId == R.id.action_editar) {
                    Intent intent = new Intent(context, EditTareaActivity.class);
                    intent.putExtra(DBmanager.COL_ID, id);
                    context.startActivity(intent);
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
        ImageView btnMenu;
        CardView cardView;

        public TareaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvDescripcion = itemView.findViewById(R.id.tvDescripcion);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnMenu = itemView.findViewById(R.id.btnMenu);
            cardView = itemView.findViewById(R.id.cardViewTarea);
        }
    }
}
