package com.example.myapplication.network;

import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // Reemplazá "auth/login" por el endpoint real de tu API.
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);
}