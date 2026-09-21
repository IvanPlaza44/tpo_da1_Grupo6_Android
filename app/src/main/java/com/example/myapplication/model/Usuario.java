package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

/**
 * Modelo de datos que representa a un usuario de la plataforma.
 * Gson (el conversor configurado en RetrofitClient con GsonConverterFactory)
 * convierte automáticamente el JSON que devuelve el servidor en un objeto
 * de esta clase. Por eso necesita un constructor vacío: Gson lo instancia
 * primero y después completa los campos usando reflection.
 */
public class Usuario {

    private long id;
    private String nombre;
    private String email;
    private String telefono;
    private String zona;

    // El backend la llama "fotoPerfil"; se acepta también "fotoUrl".
    // El nombre del atributo en Java no cambia, así que los getters siguen igual.
    @SerializedName(value = "fotoPerfil", alternate = {"fotoUrl"})
    private String fotoUrl;

    private String fechaAlta; // se usa para mostrar "antigüedad en la plataforma"

    // Reputación: viene dentro de GET /api/usuarios/me (PerfilResponseDto)
    private Double promedioEstrellas;
    private long operacionesComoComprador;
    private long operacionesComoVendedor;

    // Constructor vacío requerido por Gson para deserializar el JSON
    public Usuario() {
    }

    // Getters y setters: la UI los usa para leer/escribir cada campo
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    public String getZona() { return zona; }
    public void setZona(String zona) { this.zona = zona; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getFechaAlta() { return fechaAlta; }
    public void setFechaAlta(String fechaAlta) { this.fechaAlta = fechaAlta; }

    public Double getPromedioEstrellas() { return promedioEstrellas; }
    public long getOperacionesComoComprador() { return operacionesComoComprador; }
    public long getOperacionesComoVendedor() { return operacionesComoVendedor; }
}