package com.example.myapplication.network;

import com.example.myapplication.model.PerfilPublicoResponseDto;
import com.example.myapplication.model.Publicacion.OperacionResponseDto;
import com.example.myapplication.model.Publicacion.CalificacionOperacionRequestDto;
import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.model.UsuarioUpdateRequest;
import com.example.myapplication.model.Reputacion;
import com.example.myapplication.model.Publicacion.PreguntaRequestDto;
import com.example.myapplication.model.Publicacion.PreguntaResponseDto;
import com.example.myapplication.model.Publicacion.RespuestaPreguntaDto;
import com.example.myapplication.model.Publicacion.OfertaRequestDto;
import com.example.myapplication.model.Publicacion.OfertaResponseDto;
import java.util.List;
import com.example.myapplication.model.OtpVerifyRequest;
import com.example.myapplication.model.Publicacion.PaginaDto;
import retrofit2.http.Query;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import okhttp3.MultipartBody;
import retrofit2.http.Multipart;
import retrofit2.http.Part;
import retrofit2.http.PATCH;

/**
 * Interfaz de Retrofit: aca NO se implementa ninguna logica de red.
 * Cada metodo es un endpoint (Level 2 del Richardson Maturity Model:
 * recursos + verbos HTTP correctos). Retrofit lee las anotaciones
 * (@GET, @PUT, @Path, @Body) y genera automaticamente el codigo que
 * arma el request y parsea la respuesta.
 */
public interface ApiService {

    @GET("api/publicaciones")
    Call<PaginaDto<PublicacionResumen>> explorar(
            @Query("pagina") int pagina,
            @Query("tamanio") int tamanio,
            @Query("query") String query,
            @Query("categoriaId") Long categoriaId,
            @Query("precioMin") Double precioMin,
            @Query("precioMax") Double precioMax,
            @Query("estadoArticulo") String estadoArticulo,
            @Query("zona") String zona,
            @Query("orden") String orden
    );

    @GET("api/categorias")
    Call<List<CategoriaDto>> obtenerCategorias();

    @GET("api/publicaciones/borrador")
    Call<PublicacionDetalle> obtenerBorrador();

    @PUT("api/publicaciones/me")
    Call<PublicacionDetalle> guardarPaso(@Path("id") long publicacionId, @Body PublicacionRequest body);

    @POST("api/publicaciones/me/publicar")
    Call<Void> publicar(@Path("id") long publicacionId);

    @Multipart
    @POST("api/publicaciones/{id}/fotos")
    Call<PublicacionDetalle> subirFoto(@Path("id") long publicacionId, @Part MultipartBody.Part archivo);

    @POST("api/auth/otp/solicitar")
    Call<Void> solicitarOtp(@Body EmailRequest body);

    @POST("api/auth/otp/reenviar")
    Call<Void> reenviarOtp(@Body EmailRequest body);

    @POST("api/auth/otp/verificar")
    Call<AuthResponse> verificarOtp(@Body OtpVerifyRequest body);

    @POST("api/auth/login")
    Call<AuthResponse> login(@Body LoginRequest body);

    @POST("api/auth/registro")
    Call<Void> registrar(@Body LoginRequest request);

    // ---------- PERFIL Y REPUTACION ----------

    // GET /api/usuarios/5 -> trae los datos de un usuario por id.
    // @Path("id") reemplaza el "{id}" de la URL por el valor que le pasemos.
    @GET("api/usuarios/{id}")
    Call<Usuario> obtenerUsuario(@Path("id") long id);

    // GET /api/usuarios/me -> tu perfil completo (con email y telefono).
    // El backend te identifica por el token, por eso no lleva id.
    @GET("api/usuarios/me")
    Call<Usuario> obtenerMiPerfil();

    // PUT /api/usuarios/me -> actualiza nombre/telefono/zona del usuario logueado.
    // El backend lo identifica por el token (Authorization), por eso no lleva id.
    // @Body serializa el objeto UsuarioUpdateRequest a JSON automaticamente.
    @PUT("api/usuarios/me")
    Call<Usuario> actualizarUsuario(@Body UsuarioUpdateRequest body);

    // GET /api/usuarios/5/reputacion -> trae el promedio de estrellas y operaciones.
    @GET("api/usuarios/{id}/reputacion")
    Call<Reputacion> obtenerReputacion(@Path("id") long id);

    // ---------- MIS PUBLICACIONES ----------

    @GET("api/publicaciones/mias")
    Call<List<PublicacionResumen>> misPublicaciones();

    @PATCH("api/publicaciones/{id}/pausar")
    Call<Void> pausar(@Path("id") long id);

    @PATCH("api/publicaciones/{id}/reactivar")
    Call<Void> reactivar(@Path("id") long id);

    @PATCH("api/publicaciones/{id}/vendida")
    Call<Void> marcarVendida(@Path("id") long id);

    // ---------- DETALLE / PREGUNTAS / OFERTAS ----------

    @GET("api/publicaciones/{id}")
    Call<PublicacionDetalle> getDetalle(@Path("id") long id);

    @POST("api/publicaciones/{publicacionId}/preguntas")
    Call<PreguntaResponseDto> preguntar(
            @Path("publicacionId") long publicacionId,
            @Body PreguntaRequestDto body
    );

    @GET("api/publicaciones/{publicacionId}/preguntas")
    Call<List<PreguntaResponseDto>> listarPreguntas(@Path("publicacionId") long publicacionId);

    @PUT("api/publicaciones/{publicacionId}/preguntas/{preguntaId}/respuesta")
    Call<PreguntaResponseDto> responderPregunta(
            @Path("publicacionId") long publicacionId,
            @Path("preguntaId") long preguntaId,
            @Body RespuestaPreguntaDto body
    );

    @POST("api/publicaciones/{publicacionId}/ofertas")
    Call<OfertaResponseDto> ofertar(
            @Path("publicacionId") long publicacionId,
            @Body OfertaRequestDto body
    );

    @GET("api/publicaciones/{publicacionId}/ofertas")
    Call<List<OfertaResponseDto>> listarOfertas(@Path("publicacionId") long publicacionId);

    @GET("api/operaciones/mias")
    Call<List<OperacionResponseDto>> misOperaciones();

    @GET("api/operaciones/{operacionId}")
    Call<OperacionResponseDto> detalleOperacion(@Path("operacionId") long operacionId);

    @POST("api/operaciones/{operacionId}/calificar")
    Call<Void> calificarOperacion(
            @Path("operacionId") long operacionId,
            @Body CalificacionOperacionRequestDto body
    );

    @GET("api/usuarios/{id}")
    Call<PerfilPublicoResponseDto> obtenerPerfilPublico(@Path("id") long id);

    @PUT("api/ofertas/{ofertaId}/aceptar")
    Call<Void> aceptarOferta(@Path("ofertaId") long ofertaId);

    @PUT("api/ofertas/{ofertaId}/rechazar")
    Call<Void> rechazarOferta(@Path("ofertaId") long ofertaId);

    @POST("api/ofertas/{ofertaId}/contraofertar")
    Call<OfertaResponseDto> contraofertar(
            @Path("ofertaId") long ofertaId,
            @Body OfertaRequestDto body
    );

    @GET("api/ofertas/recibidas")
    Call<List<OfertaResponseDto>> misOfertasRecibidas();

    @GET("api/ofertas/enviadas")
    Call<List<OfertaResponseDto>> misOfertasEnviadas();
}