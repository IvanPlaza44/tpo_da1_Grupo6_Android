package com.example.myapplication.model.dto.request;

public class PreguntaRequestDto {
    private String mensaje;

    public PreguntaRequestDto(String mensaje) {
        this.mensaje = mensaje;
    }

    public String getMensaje() { return mensaje; }
}