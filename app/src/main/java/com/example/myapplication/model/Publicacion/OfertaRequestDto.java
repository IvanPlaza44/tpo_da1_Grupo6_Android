package com.example.myapplication.model.Publicacion;

import java.math.BigDecimal;

public class OfertaRequestDto {
    public BigDecimal monto;
    public String mensaje;

    public OfertaRequestDto(BigDecimal monto, String mensaje) {
        this.monto = monto;
        this.mensaje = mensaje;
    }
}