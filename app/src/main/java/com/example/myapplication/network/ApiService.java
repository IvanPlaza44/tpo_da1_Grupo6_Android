package com.example.myapplication.network;

import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.OtpVerifyRequest;
import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;
import com.example.myapplication.model.Reputacion;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.model.UsuarioUpdateRequest;
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.model.dto.request.OfertaRequestDto;
import com.example.myapplication.model.dto.request.PreguntaRequestDto;
import com.example.myapplication.model.dto.request.RespuestaPreguntaDto;
import com.example.myapplication.model.dto.response.OfertaResponseDto;
import com.example.myapplication.model.dto.response.PreguntaResponseDto;
import com.example.myapplication.model.dto.response.PublicacionDetalleDto;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

public interface ApiService {

    // --- Auth ---
    @POST("auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("auth/otp/solicitar")
    Call<Void> solicitarOtp(@Body EmailRequest body);

    @POST("api/auth/otp/verificar")
    Call<AuthResponse> verificarOtp(@Body OtpVerifyRequest body);

    @POST("api/auth/otp/reenviar")
    Call<Void> reenviarOtp(@Body EmailRequest body);

    // --- Usuarios ---
    @GET("usuarios/{id}")
    Call<Usuario> obtenerUsuario(@Path("id") long id);

    @PUT("usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") long id, @Body UsuarioUpdateRequest body);

    // --- Categorías ---
    @GET("categorias")
    Call<List<CategoriaDto>> obtenerCategorias();

    // --- Publicar artículo (borrador) ---
    @GET("publicaciones/borrador")
    Call<PublicacionDetalle> obtenerBorrador();

    @PUT("publicaciones/{id}")
    Call<PublicacionDetalle> guardarPaso(@Path("id") long id, @Body PublicacionRequest body);

    @POST("publicaciones/{id}/publicar")
    Call<Void> publicar(@Path("id") long id);

    // --- Detalle de Publicación (punto 4) ---
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
    @GET("usuarios/me")
    Call<Reputacion> obtenerReputacion();

    @GET("publicaciones/{publicacionId}/ofertas")
    Call<List<OfertaResponseDto>> listarOfertas(@Path("publicacionId") long publicacionId);
}