package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo de datos para la reputación de un usuario.
 * Se arma a partir de las calificaciones recibidas: un promedio de estrellas
 * y la cantidad de operaciones concretadas, separadas por rol (comprador/vendedor),
 * tal como lo pide el TPO en el punto "Perfil y Reputación".
 */
public class Reputacion {

    private double promedioEstrellas;

    @SerializedName("operacionesComoComprador")
    private int cantidadComoComprador;

    @SerializedName("operacionesComoVendedor")
    private int cantidadComoVendedor;

    public Reputacion() {
        // Constructor vacío requerido por Gson
    }

    public double getPromedioEstrellas() { return promedioEstrellas; }
    public void setPromedioEstrellas(double promedioEstrellas) { this.promedioEstrellas = promedioEstrellas; }

    public int getCantidadComoComprador() { return cantidadComoComprador; }
    public void setCantidadComoComprador(int cantidadComoComprador) { this.cantidadComoComprador = cantidadComoComprador; }

    public int getCantidadComoVendedor() { return cantidadComoVendedor; }
    public void setCantidadComoVendedor(int cantidadComoVendedor) { this.cantidadComoVendedor = cantidadComoVendedor; }

    // Método de conveniencia: no viene del JSON, lo calculamos en el cliente
    public int getTotalOperaciones() {
        return cantidadComoComprador + cantidadComoVendedor;
    }
}