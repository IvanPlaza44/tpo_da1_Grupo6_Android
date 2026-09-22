package com.example.myapplication.ui.ofertas;

import android.app.AlertDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import com.example.myapplication.session.SessionManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OfertaRequestDto;
import com.example.myapplication.model.Publicacion.OfertaResponseDto;
import com.example.myapplication.network.ApiService;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MisOfertasFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject SessionManager sessionManager;

    private RadioGroup rgTipoOferta;
    private ProgressBar progressBar;
    private TextView tvVacio;
    private RecyclerView rvMisOfertas;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_ofertas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgTipoOferta = view.findViewById(R.id.rgTipoOferta);
        progressBar = view.findViewById(R.id.progressBar);
        tvVacio = view.findViewById(R.id.tvVacio);
        rvMisOfertas = view.findViewById(R.id.rvMisOfertas);

        rvMisOfertas.setLayoutManager(new LinearLayoutManager(requireContext()));

        rgTipoOferta.setOnCheckedChangeListener((group, checkedId) -> cargarOfertas());

        cargarOfertas();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Por si volvemos de aceptar/rechazar, refrescamos siempre actualizado
        if (apiService != null) cargarOfertas();
    }

    private void cargarOfertas() {
        boolean esRecibidas = rgTipoOferta.getCheckedRadioButtonId() == R.id.rbRecibidas;

        progressBar.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);

        Call<List<OfertaResponseDto>> call = esRecibidas
                ? apiService.misOfertasRecibidas()
                : apiService.misOfertasEnviadas();

        call.enqueue(new Callback<List<OfertaResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OfertaResponseDto>> call,
                                   @NonNull Response<List<OfertaResponseDto>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);

                if (response.isSuccessful() && response.body() != null) {
                    List<OfertaResponseDto> lista = response.body();
                    long miId = sessionManager.getUsuarioId();
                    List<OfertaResponseDto> filtradas = new java.util.ArrayList<>();
                    for (OfertaResponseDto o : lista) {
                        boolean esMia = o.autorId == miId;
                        if (esRecibidas && !esMia) {
                            filtradas.add(o);
                        } else if (!esRecibidas && esMia) {
                            filtradas.add(o);
                        }
                    }
                    lista = filtradas;

                    tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                    tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                    rvMisOfertas.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
                    rvMisOfertas.setAdapter(new MisOfertasAdapter(lista, esRecibidas, new MisOfertasAdapter.OnAccionListener() {
                        @Override
                        public void onAceptar(OfertaResponseDto oferta) {
                            responder(oferta.id, true);
                        }

                        @Override
                        public void onRechazar(OfertaResponseDto oferta) {
                            responder(oferta.id, false);
                        }

                        @Override
                        public void onContraofertar(OfertaResponseDto oferta) {
                            mostrarDialogoContraoferta(oferta);
                        }
                    }));
                } else {
                    Toast.makeText(requireContext(), "No se pudieron cargar las ofertas", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OfertaResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Sin conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void responder(long ofertaId, boolean aceptar) {
        Call<Void> call = aceptar ? apiService.aceptarOferta(ofertaId) : apiService.rechazarOferta(ofertaId);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            aceptar ? "Oferta aceptada" : "Oferta rechazada",
                            Toast.LENGTH_SHORT).show();
                    cargarOfertas();
                } else {
                    Toast.makeText(requireContext(), "No se pudo actualizar la oferta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDialogoContraoferta(OfertaResponseDto oferta) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_contraoferta, null);
        EditText etMonto = dialogView.findViewById(R.id.etMontoContraoferta);
        EditText etMensaje = dialogView.findViewById(R.id.etMensajeContraoferta);

        new AlertDialog.Builder(requireContext())
                .setTitle("Hacer contraoferta")
                .setView(dialogView)
                .setPositiveButton("Enviar", (dialog, which) -> {
                    String montoTexto = etMonto.getText().toString().trim();
                    if (montoTexto.isEmpty()) {
                        Toast.makeText(requireContext(), "Ingresá un monto", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    double monto = Double.parseDouble(montoTexto);
                    String mensaje = etMensaje.getText().toString().trim();
                    enviarContraoferta(oferta.id, monto, mensaje);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void enviarContraoferta(long ofertaId, double monto, String mensaje) {
        OfertaRequestDto body = new OfertaRequestDto(new java.math.BigDecimal(monto), mensaje);

        apiService.contraofertar(ofertaId, body).enqueue(new Callback<OfertaResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<OfertaResponseDto> call, @NonNull Response<OfertaResponseDto> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Contraoferta enviada", Toast.LENGTH_SHORT).show();
                    cargarOfertas();
                } else {
                    Toast.makeText(requireContext(), "No se pudo enviar la contraoferta", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<OfertaResponseDto> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}