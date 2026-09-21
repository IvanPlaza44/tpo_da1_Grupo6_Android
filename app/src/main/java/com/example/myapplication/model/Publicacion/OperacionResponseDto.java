package com.example.myapplication.model.Publicacion;

public class OperacionResponseDto {
    public long id;
    public long publicacionId;
    public String publicacionTitulo;
    public long compradorId;
    public String compradorNombre;
    public long vendedorId;
    public String vendedorNombre;
    public double montoFinal;
    public String estado;
    public String fechaAcordada;
    public String fechaEntrega;
    public String direccionEncuentro;
    public Double latitudEncuentro;
    public Double longitudEncuentro;
    public boolean puedeCalificar;
    public String tipo; // "COMPRA" o "VENTA"
}