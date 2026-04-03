package com.example.dasproyecto.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * Entidad Room que representa una tarea en la base de datos local.
 * Los campos reflejan la estructura de la tabla remota del servidor.
 */
@Entity(tableName = "tareas")
public class TareaEntity {

    @PrimaryKey
    @ColumnInfo(name = "id")
    public int id;

    @ColumnInfo(name = "usuario_id")
    public int usuarioId;

    @ColumnInfo(name = "titulo")
    public String titulo;

    @ColumnInfo(name = "descripcion")
    public String descripcion;

    @ColumnInfo(name = "prioridad")
    public int prioridad;

    @ColumnInfo(name = "fechaLimite")
    public String fechaLimite;

    @ColumnInfo(name = "completada")
    public int completada;

    @ColumnInfo(name = "latitud")
    public String latitud;

    @ColumnInfo(name = "longitud")
    public String longitud;

    @ColumnInfo(name = "direccion")
    public String direccion;

    /**
     * Constructor vacío requerido por Room.
     */
    public TareaEntity() {}

    /**
     * Constructor de conveniencia para crear una entidad desde un JSONObject del servidor.
     */
    public TareaEntity(int id, int usuarioId, String titulo, String descripcion,
                       int prioridad, String fechaLimite, int completada,
                       String latitud, String longitud, String direccion) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.titulo = titulo;
        this.descripcion = descripcion;
        this.prioridad = prioridad;
        this.fechaLimite = fechaLimite;
        this.completada = completada;
        this.latitud = latitud;
        this.longitud = longitud;
        this.direccion = direccion;
    }
}
