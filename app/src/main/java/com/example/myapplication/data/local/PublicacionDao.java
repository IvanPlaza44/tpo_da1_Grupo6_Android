package com.example.myapplication.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface PublicacionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertarTodas(List<PublicacionEntity> publicaciones);

    @Query("DELETE FROM publicaciones_cache")
    void borrarTodo();

    @Query("SELECT * FROM publicaciones_cache ORDER BY orden ASC")
    List<PublicacionEntity> obtenerTodas();
}