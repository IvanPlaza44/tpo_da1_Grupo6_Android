package com.example.myapplication.data.network;

import com.example.myapplication.data.model.LoginRequest;
import com.example.myapplication.data.model.LoginResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    // Reemplazá "auth/login" por el endpoint real de tu API.
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);
}