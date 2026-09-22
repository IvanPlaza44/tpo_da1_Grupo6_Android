package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class BusquedaGuardadaRequestDto {
    public String nombre;
    @SerializedName("query")
    public String query;
    public Long categoriaId;
    public Double precioMin;
    public Double precioMax;
    public String estadoArticulo;
    public String zona;
}
