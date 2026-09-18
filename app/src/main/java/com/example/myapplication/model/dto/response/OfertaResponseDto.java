package com.example.myapplication.model.dto.response;

public class OfertaResponseDto {
    private long id;
    private long publicacionId;
    private String publicacionTitulo;
    private long autorId;
    private String autorNombre;
    private double monto;
    private String mensaje;
    private String estado;
    private String fecha;
    private String fechaVencimiento;
    private String tipo;

    public long getId() { return id; }
    public long getPublicacionId() { return publicacionId; }
    public String getPublicacionTitulo() { return publicacionTitulo; }
    public long getAutorId() { return autorId; }
    public String getAutorNombre() { return autorNombre; }
    public double getMonto() { return monto; }
    public String getMensaje() { return mensaje; }
    public String getEstado() { return estado; }
    public String getFecha() { return fecha; }
    public String getFechaVencimiento() { return fechaVencimiento; }
    public String getTipo() { return tipo; }
}