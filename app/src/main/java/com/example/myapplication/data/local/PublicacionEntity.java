package com.example.myapplication.data.local;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "publicaciones_cache")
public class PublicacionEntity {

    @PrimaryKey
    public long id;

    public String titulo;
    public Double precio;
    public String estado;
    public String estadoArticulo;
    public String zonaEntrega;
    public String fotoPrincipal;
    public String vendedorNombre;

    // Orden en que llegaron del backend, para mostrarlas en el mismo orden
    // cuando las leemos despues desde el cache offline
    @ColumnInfo(name = "orden")
    public int orden;
}