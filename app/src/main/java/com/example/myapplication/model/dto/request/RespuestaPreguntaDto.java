package com.example.myapplication.model.dto.request;

public class RespuestaPreguntaDto {
    private String respuesta;

    public RespuestaPreguntaDto(String respuesta) {
        this.respuesta = respuesta;
    }

    public String getRespuesta() { return respuesta; }
}
