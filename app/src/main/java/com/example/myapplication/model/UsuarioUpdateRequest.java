package com.example.myapplication.model;

/**
 * Body que se envía en el PUT /api/usuarios/{id}.
 * Es una clase distinta de Usuario a propósito: el servidor no debería
 * recibir "id", "email" ni "fechaAlta" en una edición de perfil (esos campos
 * no son editables por el usuario), así que este objeto solo lleva lo que
 * realmente se puede modificar.
 */
public class UsuarioUpdateRequest {

    private String nombre;
    private String telefono;
    private String zona;

    public UsuarioUpdateRequest(String nombre, String telefono, String zona) {
        this.nombre = nombre;
        this.telefono = telefono;
        this.zona = zona;
    }

    public String getNombre() { return nombre; }
    public String getTelefono() { return telefono; }
    public String getZona() { return zona; }
}