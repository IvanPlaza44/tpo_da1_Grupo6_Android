package com.example.myapplication.model;

public class ApiError {
    private String timestamp;
    private int status;
    private String error;
    private String mensaje;

    public String getMensaje() { return mensaje; }
    public int getStatus() { return status; }
}
