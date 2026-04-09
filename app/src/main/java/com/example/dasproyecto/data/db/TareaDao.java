package com.example.dasproyecto.data.db;

import android.database.Cursor;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * DAO (Data Access Object) de Room para operaciones CRUD sobre la tabla de tareas.
 * Proporciona métodos tanto para uso directo (List) como para ContentProvider (Cursor).
 */
@Dao
public interface TareaDao {

    // =========================================================================
    // CONSULTAS (Lecturas)
    // =========================================================================

    /**
     * Obtiene todas las tareas de un usuario ordenadas por fecha límite.
     */
    @Query("SELECT * FROM tareas WHERE usuario_id = :usuarioId ORDER BY fechaLimite ASC")
    List<TareaEntity> getTareasPorFecha(int usuarioId);

    /**
     * Obtiene todas las tareas de un usuario ordenadas por prioridad (descendente).
     */
    @Query("SELECT * FROM tareas WHERE usuario_id = :usuarioId ORDER BY prioridad DESC")
    List<TareaEntity> getTareasPorPrioridad(int usuarioId);

    /**
     * Obtiene una tarea específica por su ID.
     */
    @Query("SELECT * FROM tareas WHERE id = :tareaId LIMIT 1")
    TareaEntity getTarea(int tareaId);

    /**
     * Devuelve un Cursor con tareas filtradas y ordenadas dinámicamente.
     * Necesario para el ContentProvider.
     */
    @androidx.room.RawQuery
    Cursor getCursorTareasDinamico(androidx.sqlite.db.SupportSQLiteQuery query);

    /**
     * Devuelve un Cursor con una tarea específica.
     * Necesario para el ContentProvider.
     */
    @Query("SELECT * FROM tareas WHERE id = :tareaId LIMIT 1")
    Cursor getCursorTarea(int tareaId);

    // =========================================================================
    // INSERCIONES
    // =========================================================================

    /**
     * Inserta o reemplaza una lista de tareas (usado en sincronización completa).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<TareaEntity> tareas);

    /**
     * Inserta o reemplaza una sola tarea.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(TareaEntity tarea);

    // =========================================================================
    // ELIMINACIONES
    // =========================================================================

    /**
     * Borra todas las tareas de un usuario (para limpiar antes de sincronizar).
     */
    @Query("DELETE FROM tareas WHERE usuario_id = :usuarioId")
    void deleteAll(int usuarioId);

    /**
     * Borra las tareas completadas de un usuario.
     */
    @Query("DELETE FROM tareas WHERE usuario_id = :usuarioId AND completada = 1")
    int deleteCompletadas(int usuarioId);

    /**
     * Borra una tarea específica por su ID.
     */
    @Query("DELETE FROM tareas WHERE id = :tareaId")
    int deleteTarea(int tareaId);
}
