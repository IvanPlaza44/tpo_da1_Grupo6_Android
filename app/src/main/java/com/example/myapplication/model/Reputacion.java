package com.example.myapplication.model;
public class Reputacion {

    private double promedioEstrellas;
    private int cantidadComoComprador;
    private int cantidadComoVendedor;

    public Reputacion() {
        // Constructor vacio requerido por Gson
    }

    public double getPromedioEstrellas() { return promedioEstrellas; }
    public void setPromedioEstrellas(double promedioEstrellas) { this.promedioEstrellas = promedioEstrellas; }

    public int getCantidadComoComprador() { return cantidadComoComprador; }
    public void setCantidadComoComprador(int cantidadComoComprador) { this.cantidadComoComprador = cantidadComoComprador; }

    public int getCantidadComoVendedor() { return cantidadComoVendedor; }
    public void setCantidadComoVendedor(int cantidadComoVendedor) { this.cantidadComoVendedor = cantidadComoVendedor; }

    // Metodo de conveniencia: no viene del JSON, lo calculamos en el cliente
    public int getTotalOperaciones() {
        return cantidadComoComprador + cantidadComoVendedor;
    }
}