package com.example.myapplication.network;

import com.example.myapplication.model.dto.request.OfertaRequestDto;
import com.example.myapplication.model.dto.request.PreguntaRequestDto;
import com.example.myapplication.model.dto.request.RespuestaPreguntaDto;
import com.example.myapplication.model.dto.response.OfertaResponseDto;
import com.example.myapplication.model.dto.response.PreguntaResponseDto;
import com.example.myapplication.model.dto.response.PublicacionDetalleDto;

import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.LoginResponse;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest body);
    @GET("publicaciones/{id}")
    Call<PublicacionDetalleDto> getDetalle(@Path("id") long id);

    @POST("publicaciones/{publicacionId}/preguntas")
    Call<PreguntaResponseDto> preguntar(
            @Path("publicacionId") long publicacionId,
            @Body PreguntaRequestDto body
    );

    @GET("publicaciones/{publicacionId}/preguntas")
    Call<List<PreguntaResponseDto>> listarPreguntas(@Path("publicacionId") long publicacionId);

    @PUT("publicaciones/{publicacionId}/preguntas/{preguntaId}/respuesta")
    Call<PreguntaResponseDto> responderPregunta(
            @Path("publicacionId") long publicacionId,
            @Path("preguntaId") long preguntaId,
            @Body RespuestaPreguntaDto body
    );

    @POST("publicaciones/{publicacionId}/ofertas")
    Call<OfertaResponseDto> ofertar(
            @Path("publicacionId") long publicacionId,
            @Body OfertaRequestDto body
    );


    @GET("publicaciones/{publicacionId}/ofertas")
    Call<List<OfertaResponseDto>> listarOfertas(@Path("publicacionId") long publicacionId);
}