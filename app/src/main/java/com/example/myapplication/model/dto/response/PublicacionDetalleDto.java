package com.example.myapplication.model.dto.response;

import com.example.myapplication.model.enums.EstadoArticulo;
import com.example.myapplication.model.enums.EstadoPublicacion;

import java.util.List;

public class PublicacionDetalleDto {
    private long id;
    private String titulo;
    private String descripcion;
    private String categoria;
    private double precio;
    private EstadoArticulo estadoArticulo;
    private EstadoPublicacion estado;
    private String zonaEntrega;
    private String fechaPublicacion;
    private List<String> fotos;
    private long vendedorId;
    private String vendedorNombre;
    private Double vendedorPromedioEstrellas;
    private boolean esPropia;

    public long getId() { return id; }
    public String getTitulo() { return titulo; }
    public String getDescripcion() { return descripcion; }
    public String getCategoria() { return categoria; }
    public double getPrecio() { return precio; }
    public EstadoArticulo getEstadoArticulo() { return estadoArticulo; }
    public EstadoPublicacion getEstado() { return estado; }
    public String getZonaEntrega() { return zonaEntrega; }
    public String getFechaPublicacion() { return fechaPublicacion; }
    public List<String> getFotos() { return fotos; }
    public long getVendedorId() { return vendedorId; }
    public String getVendedorNombre() { return vendedorNombre; }
    public Double getVendedorPromedioEstrellas() { return vendedorPromedioEstrellas; }
    public boolean isEsPropia() { return esPropia; }
}