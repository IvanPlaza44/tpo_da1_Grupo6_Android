package com.example.myapplication.network;

import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.model.UsuarioUpdateRequest;
import com.example.myapplication.model.Reputacion;

import java.util.List;
import com.example.myapplication.model.OtpVerifyRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

/**
 * Interfaz de Retrofit: acá NO se implementa ninguna lógica de red.
 * Cada método es un endpoint (Level 2 del Richardson Maturity Model:
 * recursos + verbos HTTP correctos). Retrofit lee las anotaciones
 * (@GET, @PUT, @Path, @Body) y genera automáticamente el código que
 * arma el request y parsea la respuesta.
 */
public interface ApiService {

    @GET("api/categorias")
    Call<List<CategoriaDto>> obtenerCategorias();

    @GET("api/publicaciones/borrador")
    Call<PublicacionDetalle> obtenerBorrador();

    @PUT("api/publicaciones/{id}")
    Call<PublicacionDetalle> guardarPaso(@Path("id") long publicacionId, @Body PublicacionRequest body);

    @POST("api/publicaciones/{id}/publicar")
    Call<Void> publicar(@Path("id") long publicacionId);

    @POST("api/auth/otp/solicitar")
    Call<Void> solicitarOtp(@Body EmailRequest body);

    @POST("api/auth/otp/reenviar")
    Call<Void> reenviarOtp(@Body EmailRequest body);

    @POST("api/auth/otp/verificar")
    Call<AuthResponse> verificarOtp(@Body OtpVerifyRequest body);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    // ---------- PERFIL Y REPUTACIÓN ----------

    // GET /api/usuarios/5  → trae los datos de un usuario por id.
    // @Path("id") reemplaza el "{id}" de la URL por el valor que le pasemos.
    @GET("api/usuarios/{id}")
    Call<Usuario> obtenerUsuario(@Path("id") long id);

    // PUT /api/usuarios/5  → actualiza nombre/teléfono/zona.
    // @Body serializa el objeto UsuarioUpdateRequest a JSON automáticamente.
    @PUT("api/usuarios/{id}")
    Call<Usuario> actualizarUsuario(@Path("id") long id, @Body UsuarioUpdateRequest body);

    // GET /api/usuarios/5/reputacion → trae el promedio de estrellas y operaciones.
    @GET("api/usuarios/{id}/reputacion")
    Call<Reputacion> obtenerReputacion(@Path("id") long id);
}