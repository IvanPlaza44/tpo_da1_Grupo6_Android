package com.example.myapplication.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.model.PublicacionDetalleDto;
import com.example.myapplication.model.Publicacion.OperacionResponseDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.android.material.button.MaterialButton;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Pantalla "Punto de encuentro": aparece justo después de aceptar una oferta.
 * Recibe el operacionId REAL por argumento de navegación (no un valor fijo de
 * prueba), y desde acá se puede:
 *  - Abrir una app de mapas para llegar a la zona de entrega.
 *  - Marcar la operación como ENTREGADA, que es lo único que habilita
 *    calificar en el Historial (ver README: puedeCalificar solo es true
 *    después de PUT /api/operaciones/{id}/entregada).
 */
public class MapaFragment extends Fragment {

    private static final String TAG = "MapaFragment";

    private long operacionId;
    private String direccionParaBuscar = "";
    private ApiService apiService;

    private TextView tvArticulo, tvEstado;
    private MaterialButton btnComoLlegar, btnMarcarEntregada;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(com.example.myapplication.R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            operacionId = getArguments().getLong("operacionId");
        }

        tvArticulo = view.findViewById(com.example.myapplication.R.id.tvArticulo);
        tvEstado = view.findViewById(com.example.myapplication.R.id.tvEstado);
        btnComoLlegar = view.findViewById(com.example.myapplication.R.id.btnComoLlegar);
        btnMarcarEntregada = view.findViewById(com.example.myapplication.R.id.btnMarcarEntregada);

        apiService = RetrofitClient.getApiService(requireContext());

        if (operacionId <= 0) {
            tvEstado.setText("No se pudo identificar la operación.");
            return;
        }

        btnComoLlegar.setOnClickListener(v -> abrirMapaExterno());
        btnMarcarEntregada.setOnClickListener(v -> confirmarEntrega());

        cargarOperacion();
    }

    private void cargarOperacion() {
        apiService.detalleOperacion(operacionId).enqueue(new Callback<OperacionResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<OperacionResponseDto> call,
                                   @NonNull Response<OperacionResponseDto> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    OperacionResponseDto op = response.body();
                    tvArticulo.setText(op.publicacionTitulo);

                    if ("ENTREGADA".equals(op.estado)) {
                        tvEstado.setText("Ya marcaste esta entrega. Podés calificar desde tu Historial.");
                        btnMarcarEntregada.setEnabled(false);
                        btnMarcarEntregada.setText("Entrega ya marcada");
                    } else {
                        btnMarcarEntregada.setEnabled(true);
                    }

                    // La operación todavía no tiene una dirección propia cargada
                    // (eso se guarda con PUT /api/operaciones/{id}/punto-encuentro,
                    // que hoy no tiene una pantalla para completarlo). Mientras tanto,
                    // usamos la zona de entrega que puso el vendedor al publicar.
                    cargarZonaDeLaPublicacion(op.publicacionId);
                } else {
                    tvEstado.setText("No se pudo cargar la operación (código " + response.code() + ").");
                }
            }

            @Override
            public void onFailure(@NonNull Call<OperacionResponseDto> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error de red: " + t.getMessage());
                tvEstado.setText("Sin conexión. No se pudo cargar la operación.");
            }
        });
    }

    private void cargarZonaDeLaPublicacion(long publicacionId) {
        apiService.getDetallePublicacion(publicacionId).enqueue(new Callback<PublicacionDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleDto> call,
                                   @NonNull Response<PublicacionDetalleDto> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().getZonaEntrega() != null) {
                    direccionParaBuscar = response.body().getZonaEntrega();
                    btnComoLlegar.setEnabled(true);
                    btnComoLlegar.setText("Cómo llegar (Abrir Maps)");
                } else {
                    btnComoLlegar.setText("Ubicación no disponible");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleDto> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Fallo la red: " + t.getMessage());
                btnComoLlegar.setText("Ubicación no disponible");
            }
        });
    }

    private void abrirMapaExterno() {
        if (direccionParaBuscar == null || direccionParaBuscar.trim().isEmpty()) return;

        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(direccionParaBuscar));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

        try {
            startActivity(mapIntent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), "No se encontró ninguna aplicación de mapas instalada", Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmarEntrega() {
        btnMarcarEntregada.setEnabled(false);

        apiService.marcarEntregada(operacionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            "¡Entrega confirmada! Ya podés calificar desde Perfil > Historial y calificaciones.",
                            Toast.LENGTH_LONG).show();
                    tvEstado.setText("Entrega marcada. Ya podés calificar desde tu Historial.");
                    btnMarcarEntregada.setText("Entrega ya marcada");
                } else if (response.code() == 409) {
                    Toast.makeText(requireContext(), "Esta operación ya estaba marcada como entregada", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(requireContext(), "No se pudo marcar la entrega (código " + response.code() + ")", Toast.LENGTH_LONG).show();
                    btnMarcarEntregada.setEnabled(true);
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error de red: " + t.getMessage());
                Toast.makeText(requireContext(), "Sin conexión, intentá de nuevo", Toast.LENGTH_SHORT).show();
                btnMarcarEntregada.setEnabled(true);
            }
        });
    }
}