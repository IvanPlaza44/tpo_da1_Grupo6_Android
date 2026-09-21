package com.example.myapplication.model;

import com.example.myapplication.model.Publicacion.PublicacionResumen;
import java.util.List;

public class PerfilPublicoResponseDto {
    public long id;
    public String nombre;
    public String zona;
    public String fechaAlta;
    public double promedioEstrellas;
    public long totalCalificaciones;
    public List<PublicacionResumen> publicacionesActivas;
}