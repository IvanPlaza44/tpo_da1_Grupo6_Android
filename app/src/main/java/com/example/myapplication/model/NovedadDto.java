package com.example.myapplication.model;

public class NovedadDto {
    public long id;
    public String tipo;          // PREGUNTA_NUEVA, PREGUNTA_RESPONDIDA, OFERTA_RECIBIDA,
    // OFERTA_ACEPTADA, OFERTA_RECHAZADA, OFERTA_CONTRAOFERTADA
    public String mensaje;       // el texto que se muestra en la notificación
    public Long publicacionId;   // a q publicación se refiere
    public Long ofertaId;        // puede ser null si el aviso es de una pregunta
}