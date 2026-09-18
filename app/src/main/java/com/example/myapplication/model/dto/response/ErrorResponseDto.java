package com.example.myapplication.model.dto.response;

public class ErrorResponseDto {
    private String timestamp;
    private int status;
    private String error;
    private String mensaje;

    public String getTimestamp() { return timestamp; }
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMensaje() { return mensaje; }
}
