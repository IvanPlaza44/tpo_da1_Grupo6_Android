package com.example.myapplication.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {PublicacionEntity.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    public abstract PublicacionDao publicacionDao();

    private static volatile AppDatabase instancia;

    // Todavia no migramos a Hilt, asi que usamos el mismo patron manual
    // que RetrofitClient: un singleton clasico con doble chequeo.
    public static AppDatabase getInstance(Context context) {
        if (instancia == null) {
            synchronized (AppDatabase.class) {
                if (instancia == null) {
                    instancia = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "ronda_db"
                    ).build();
                }
            }
        }
        return instancia;
    }
}