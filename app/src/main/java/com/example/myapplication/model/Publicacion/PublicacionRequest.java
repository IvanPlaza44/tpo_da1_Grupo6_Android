package com.example.myapplication.model.Publicacion;

import java.util.List;

public class PublicacionRequest {
    public String titulo;
    public String descripcion;
    public Long categoriaId;
    public Double precio;
    public String estadoArticulo;   // "NUEVO" | "COMO_NUEVO" | "USADO"
    public String zonaEntrega;
    public List<String> fotosUrls;
}