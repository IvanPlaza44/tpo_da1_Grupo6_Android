package com.example.myapplication.model;

public class OtpVerifyRequest {
    private String email;
    private String codigo;

    public OtpVerifyRequest(String email, String codigo) {
        this.email = email;
        this.codigo = codigo;
    }
}