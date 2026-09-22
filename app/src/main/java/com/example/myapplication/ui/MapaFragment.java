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
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OperacionResponseDto;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.network.ApiService;
import com.google.android.material.button.MaterialButton;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
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
@AndroidEntryPoint
public class MapaFragment extends Fragment {

    private static final String TAG = "MapaFragment";

    private long operacionId;
    private String direccionParaBuscar = "";
    private Double latitud;
    private Double longitud;
    @Inject ApiService apiService;

    private TextView tvArticulo, tvEstado;
    private MaterialButton btnComoLlegar, btnMarcarEntregada;

    /** Abre esta pantalla desde cualquier grafo anidado (detalle, ofertas, historial). */
    public static void abrir(Fragment fragment, long operacionId) {
        if (!fragment.isAdded() || fragment.getView() == null || operacionId <= 0) return;
        Bundle args = new Bundle();
        args.putLong("operacionId", operacionId);
        try {
            Navigation.findNavController(fragment.requireView())
                    .navigate(R.id.action_global_mapaFragment, args);
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "No se pudo abrir el punto de encuentro", e);
            Toast.makeText(fragment.requireContext(),
                    "No se pudo abrir el punto de encuentro", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Aceptar una oferta no devuelve el id de la operación. Buscamos la recién
     * creada (PENDIENTE_ENTREGA de esa publicación) y abrimos el punto de encuentro.
     */
    public static void abrirTrasAceptarOferta(Fragment fragment, ApiService apiService, long publicacionId) {
        apiService.misOperaciones().enqueue(new Callback<List<OperacionResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OperacionResponseDto>> call,
                                   @NonNull Response<List<OperacionResponseDto>> response) {
                if (!fragment.isAdded()) return;

                OperacionResponseDto pendiente = null;
                OperacionResponseDto cualquiera = null;
                if (response.isSuccessful() && response.body() != null) {
                    for (OperacionResponseDto operacion : response.body()) {
                        if (operacion.publicacionId != publicacionId) continue;
                        if (cualquiera == null) cualquiera = operacion;
                        if ("PENDIENTE_ENTREGA".equals(operacion.estado)) {
                            pendiente = operacion;
                            break;
                        }
                    }
                }

                OperacionResponseDto elegida = pendiente != null ? pendiente : cualquiera;
                if (elegida != null) {
                    abrir(fragment, elegida.id);
                } else {
                    Toast.makeText(fragment.requireContext(),
                            "La oferta se aceptó, pero no encontramos la operación para coordinar la entrega",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OperacionResponseDto>> call, @NonNull Throwable t) {
                if (!fragment.isAdded()) return;
                Toast.makeText(fragment.requireContext(),
                        "La oferta se aceptó, pero no se pudo abrir el mapa (sin conexión)",
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            operacionId = getArguments().getLong("operacionId");
        }

        tvArticulo = view.findViewById(R.id.tvArticulo);
        tvEstado = view.findViewById(R.id.tvEstado);
        btnComoLlegar = view.findViewById(R.id.btnComoLlegar);
        btnMarcarEntregada = view.findViewById(R.id.btnMarcarEntregada);

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

                    if (tieneUbicacion(op.direccionEncuentro, op.latitudEncuentro, op.longitudEncuentro)) {
                        aplicarUbicacion(op.direccionEncuentro, op.latitudEncuentro, op.longitudEncuentro);
                    } else {
                        // Todavía no cargaron lat/long. Usamos la zona de entrega de la publicación.
                        cargarZonaDeLaPublicacion(op.publicacionId);
                    }
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
        apiService.getDetalle(publicacionId).enqueue(new Callback<PublicacionDetalle>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalle> call,
                                   @NonNull Response<PublicacionDetalle> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null
                        && response.body().zonaEntrega != null
                        && !response.body().zonaEntrega.trim().isEmpty()) {
                    aplicarUbicacion(response.body().zonaEntrega, null, null);
                } else {
                    btnComoLlegar.setText("Ubicación no disponible");
                }
            }

            @Override
            public void onFailure(@NonNull Call<PublicacionDetalle> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Fallo la red: " + t.getMessage());
                btnComoLlegar.setText("Ubicación no disponible");
            }
        });
    }

    private boolean tieneUbicacion(String direccion, Double lat, Double lng) {
        return (lat != null && lng != null) || (direccion != null && !direccion.trim().isEmpty());
    }

    private void aplicarUbicacion(String direccion, Double lat, Double lng) {
        latitud = lat;
        longitud = lng;
        direccionParaBuscar = direccion != null ? direccion.trim() : "";
        btnComoLlegar.setEnabled(true);
        btnComoLlegar.setText("Cómo llegar (Abrir Maps)");
    }

    private void abrirMapaExterno() {
        Uri uri;
        if (latitud != null && longitud != null) {
            uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + latitud + "," + longitud);
        } else if (direccionParaBuscar != null && !direccionParaBuscar.isEmpty()) {
            uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination="
                    + Uri.encode(direccionParaBuscar));
        } else {
            return;
        }

        Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
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