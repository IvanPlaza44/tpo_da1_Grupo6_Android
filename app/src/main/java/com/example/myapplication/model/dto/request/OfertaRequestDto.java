package com.example.myapplication.model.dto.request;

import java.math.BigDecimal;

public class OfertaRequestDto {
    private BigDecimal monto;
    private String mensaje;

    public OfertaRequestDto(BigDecimal monto, String mensaje) {
        this.monto = monto;
        this.mensaje = mensaje;
    }

    public BigDecimal getMonto() { return monto; }
    public String getMensaje() { return mensaje; }
}