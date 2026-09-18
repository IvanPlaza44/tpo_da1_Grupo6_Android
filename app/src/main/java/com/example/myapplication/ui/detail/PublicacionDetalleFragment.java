package com.example.myapplication.ui.detail;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.dto.request.OfertaRequestDto;
import com.example.myapplication.model.dto.request.PreguntaRequestDto;
import com.example.myapplication.model.dto.request.RespuestaPreguntaDto;
import com.example.myapplication.model.dto.response.OfertaResponseDto;
import com.example.myapplication.model.dto.response.PreguntaResponseDto;
import com.example.myapplication.model.dto.response.PublicacionDetalleDto;
import com.example.myapplication.network.ApiClient;
import com.example.myapplication.network.ApiService;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublicacionDetalleFragment extends Fragment {

    private static final String ARG_PUBLICACION_ID = "publicacionId";

    private long publicacionId;
    private boolean esPropia = false;
    private ApiService apiService;

    // Vistas
    private ProgressBar progressBar;
    private TextView tvError, tvTitulo, tvPrecio, tvEstadoCategoria, tvDescripcion,
            tvZona, tvFecha, tvVendedor, tvSoyVendedor;
    private LinearLayout contenido, seccionInteresado;
    private RecyclerView rvFotos, rvPreguntas;
    private EditText etPregunta, etMontoOferta;
    private Button btnPreguntar, btnOfertar;

    public static PublicacionDetalleFragment newInstance(long publicacionId) {
        PublicacionDetalleFragment fragment = new PublicacionDetalleFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_PUBLICACION_ID, publicacionId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            publicacionId = getArguments().getLong(ARG_PUBLICACION_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicacion_detalle, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = ApiClient.getApiService(requireContext());

        progressBar = view.findViewById(R.id.progressBar);
        tvError = view.findViewById(R.id.tvError);
        contenido = view.findViewById(R.id.contenido);
        tvTitulo = view.findViewById(R.id.tvTitulo);
        tvPrecio = view.findViewById(R.id.tvPrecio);
        tvEstadoCategoria = view.findViewById(R.id.tvEstadoCategoria);
        tvDescripcion = view.findViewById(R.id.tvDescripcion);
        tvZona = view.findViewById(R.id.tvZona);
        tvFecha = view.findViewById(R.id.tvFecha);
        tvVendedor = view.findViewById(R.id.tvVendedor);
        tvSoyVendedor = view.findViewById(R.id.tvSoyVendedor);
        seccionInteresado = view.findViewById(R.id.seccionInteresado);
        rvFotos = view.findViewById(R.id.rvFotos);
        rvPreguntas = view.findViewById(R.id.rvPreguntas);
        etPregunta = view.findViewById(R.id.etPregunta);
        etMontoOferta = view.findViewById(R.id.etMontoOferta);
        btnPreguntar = view.findViewById(R.id.btnPreguntar);
        btnOfertar = view.findViewById(R.id.btnOfertar);

        rvFotos.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        rvPreguntas.setLayoutManager(new LinearLayoutManager(requireContext()));

        btnPreguntar.setOnClickListener(v -> enviarPregunta());
        btnOfertar.setOnClickListener(v -> enviarOferta());

        cargarDetalle();
    }

    private void cargarDetalle() {
        mostrarCargando();

        apiService.getDetalle(publicacionId).enqueue(new Callback<PublicacionDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleDto> call,
                                   @NonNull Response<PublicacionDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mostrarDetalle(response.body());
                    cargarPreguntas();
                } else if (response.code() == 404) {
                    mostrarError("Esta publicación no existe o fue eliminada.");
                } else {
                    mostrarError("No se pudo cargar la publicación (código " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleDto> call, @NonNull Throwable t) {
                mostrarError("Sin conexión. Revisá tu internet e intentá de nuevo.");
            }
        });
    }

    private void mostrarDetalle(PublicacionDetalleDto dto) {
        esPropia = dto.isEsPropia();

        tvTitulo.setText(dto.getTitulo());
        tvPrecio.setText(String.format(Locale.getDefault(), "$ %.2f", dto.getPrecio()));
        tvEstadoCategoria.setText(dto.getCategoria() + " · " + dto.getEstadoArticulo());
        tvDescripcion.setText(dto.getDescripcion());
        tvZona.setText("Zona de entrega: " + dto.getZonaEntrega());
        tvFecha.setText("Publicado: " + dto.getFechaPublicacion());

        String estrellas = dto.getVendedorPromedioEstrellas() != null
                ? String.format(Locale.getDefault(), "%.1f★", dto.getVendedorPromedioEstrellas())
                : "Sin calificaciones";
        tvVendedor.setText(dto.getVendedorNombre() + " · " + estrellas);

        List<String> fotos = dto.getFotos() != null ? dto.getFotos() : new ArrayList<>();
        rvFotos.setAdapter(new FotosAdapter(fotos));

        seccionInteresado.setVisibility(esPropia ? View.GONE : View.VISIBLE);
        tvSoyVendedor.setVisibility(esPropia ? View.VISIBLE : View.GONE);

        contenido.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void cargarPreguntas() {
        apiService.listarPreguntas(publicacionId).enqueue(new Callback<List<PreguntaResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<PreguntaResponseDto>> call,
                                   @NonNull Response<List<PreguntaResponseDto>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    rvPreguntas.setAdapter(new PreguntasAdapter(
                            response.body(),
                            esPropia,
                            PublicacionDetalleFragment.this::responderPregunta
                    ));
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PreguntaResponseDto>> call, @NonNull Throwable t) {
                // no es crítico: el detalle ya se ve, solo no cargaron las preguntas
            }
        });
    }

    private void enviarPregunta() {
        String mensaje = etPregunta.getText().toString().trim();
        if (mensaje.isEmpty()) {
            Toast.makeText(requireContext(), "Escribí una pregunta", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.preguntar(publicacionId, new PreguntaRequestDto(mensaje))
                .enqueue(new Callback<PreguntaResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<PreguntaResponseDto> call,
                                           @NonNull Response<PreguntaResponseDto> response) {
                        if (response.isSuccessful()) {
                            etPregunta.setText("");
                            Toast.makeText(requireContext(), "Pregunta enviada", Toast.LENGTH_SHORT).show();
                            cargarPreguntas();
                        } else if (response.code() == 401) {
                            Toast.makeText(requireContext(), "Iniciá sesión para preguntar", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "No se pudo enviar la pregunta", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PreguntaResponseDto> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void enviarOferta() {
        String montoTexto = etMontoOferta.getText().toString().trim();
        if (montoTexto.isEmpty()) {
            Toast.makeText(requireContext(), "Ingresá un monto", Toast.LENGTH_SHORT).show();
            return;
        }

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoTexto);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Monto inválido", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.ofertar(publicacionId, new OfertaRequestDto(monto, null))
                .enqueue(new Callback<OfertaResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<OfertaResponseDto> call,
                                           @NonNull Response<OfertaResponseDto> response) {
                        if (response.isSuccessful()) {
                            etMontoOferta.setText("");
                            Toast.makeText(requireContext(), "Oferta enviada", Toast.LENGTH_SHORT).show();
                        } else if (response.code() == 401) {
                            Toast.makeText(requireContext(), "Iniciá sesión para ofertar", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "No se pudo enviar la oferta", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<OfertaResponseDto> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void responderPregunta(PreguntaResponseDto pregunta, String respuesta) {
        apiService.responderPregunta(publicacionId, pregunta.getId(), new RespuestaPreguntaDto(respuesta))
                .enqueue(new Callback<PreguntaResponseDto>() {
                    @Override
                    public void onResponse(@NonNull Call<PreguntaResponseDto> call,
                                           @NonNull Response<PreguntaResponseDto> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(requireContext(), "Respuesta enviada", Toast.LENGTH_SHORT).show();
                            cargarPreguntas();
                        } else {
                            Toast.makeText(requireContext(), "No se pudo responder", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<PreguntaResponseDto> call, @NonNull Throwable t) {
                        Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void mostrarCargando() {
        progressBar.setVisibility(View.VISIBLE);
        contenido.setVisibility(View.GONE);
        tvError.setVisibility(View.GONE);
    }

    private void mostrarError(String mensaje) {
        progressBar.setVisibility(View.GONE);
        contenido.setVisibility(View.GONE);
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }
}