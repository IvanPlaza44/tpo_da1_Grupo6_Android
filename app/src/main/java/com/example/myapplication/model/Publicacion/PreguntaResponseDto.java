package com.example.myapplication.model.Publicacion;

public class PreguntaResponseDto {
    public long id;
    public String autorNombre;
    public String mensaje;
    public String respuesta;
    public String fecha;

    public boolean tieneRespuesta() {
        return respuesta != null && !respuesta.isEmpty();
    }
}