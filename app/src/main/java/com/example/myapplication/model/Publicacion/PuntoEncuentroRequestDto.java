package com.example.myapplication.model.Publicacion;

/**
 * Body para PUT /api/operaciones/{id}/punto-encuentro.
 * Lo puede cargar cualquiera de las dos partes de la operación (comprador o vendedor).
 * Con latitud/longitud el backend arma el link de "Cómo llegar" de Google Maps.
 */
public class PuntoEncuentroRequestDto {
    public String direccion;
    public Double latitud;
    public Double longitud;

    public PuntoEncuentroRequestDto(String direccion, Double latitud, Double longitud) {
        this.direccion = direccion;
        this.latitud = latitud;
        this.longitud = longitud;
    }
}
