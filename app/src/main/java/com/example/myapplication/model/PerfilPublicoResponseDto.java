package com.example.myapplication.model;

import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PerfilPublicoResponseDto {
    public long id;
    public String nombre;
    public String zona;
    public String fechaAlta;
    public double promedioEstrellas;
    public long totalCalificaciones;
    public List<PublicacionResumen> publicacionesActivas;

    // El backend la llama "fotoPerfil"; se acepta también "fotoUrl".
    @SerializedName(value = "fotoPerfil", alternate = {"fotoUrl"})
    public String fotoUrl;
}