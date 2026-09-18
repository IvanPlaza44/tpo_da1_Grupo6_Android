package com.example.myapplication.model;

public class AuthResponse {
    private String token;
    private Long usuarioId;
    private String email;
    private String username;

    public String getToken() { return token; }
    public Long getUsuarioId() { return usuarioId; }
    public String getEmail() { return email; }
    public String getUsername() { return username; }
}