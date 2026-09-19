package com.example.myapplication.network;

import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.LoginResponse;
import com.example.myapplication.model.PublicacionDetalleDto; // Asegurate de que esta importación exista

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;

public interface ApiService {

    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);

    // Nuevo endpoint para traer el detalle y la zona de entrega
    @GET("api/publicaciones/{id}")
    Call<PublicacionDetalleDto> getDetallePublicacion(@Path("id") Long id);
}