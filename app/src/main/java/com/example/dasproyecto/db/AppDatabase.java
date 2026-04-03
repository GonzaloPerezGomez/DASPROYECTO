package com.example.dasproyecto.db;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Base de datos Room (Singleton).
 * Gestiona la creación y acceso a la base de datos local de tareas.
 */
@Database(entities = {TareaEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "tareas_room.db";
    private static volatile AppDatabase INSTANCE;

    /**
     * Devuelve el DAO para operar con la tabla de tareas.
     */
    public abstract TareaDao tareaDao();

    /**
     * Obtiene la instancia única de la base de datos (Singleton thread-safe).
     *
     * @param context Contexto de la aplicación.
     * @return Instancia de AppDatabase.
     */
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    )
                    .fallbackToDestructiveMigration()
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
