package com.example.myapplication.model.dto.response;

public class PreguntaResponseDto {
    private long id;
    private String autorNombre;
    private String mensaje;
    private String respuesta;
    private String fecha;

    public long getId() { return id; }
    public String getAutorNombre() { return autorNombre; }
    public String getMensaje() { return mensaje; }
    public String getRespuesta() { return respuesta; }
    public String getFecha() { return fecha; }

    public boolean tieneRespuesta() {
        return respuesta != null && !respuesta.isEmpty();
    }
}