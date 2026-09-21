package com.example.myapplication.model.Publicacion;

public class CalificacionOperacionRequestDto {
    public int estrellas;
    public String comentario;

    public CalificacionOperacionRequestDto(int estrellas, String comentario) {
        this.estrellas = estrellas;
        this.comentario = comentario;
    }
}