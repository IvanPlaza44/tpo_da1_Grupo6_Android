package com.example.myapplication.model.Publicacion;

import java.util.List;

public class PublicacionDetalle {
    public long id;
    public String titulo;
    public String descripcion;
    public String categoria;
    public Double precio;
    public String estadoArticulo;
    public String estado;          // BORRADOR, ACTIVA, PAUSADA, VENDIDA
    public String zonaEntrega;
    public List<String> fotos;
    public boolean esPropia;
}