package com.example.myapplication.network;

import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;

import java.util.List;
import com.example.myapplication.model.OtpVerifyRequest;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;

import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;

public interface ApiService {

    @Multipart
    @POST("api/publicaciones/{id}/fotos")
    Call<PublicacionDetalle> subirFoto(@Path("id") long publicacionId, @Part MultipartBody.Part archivo);


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
}