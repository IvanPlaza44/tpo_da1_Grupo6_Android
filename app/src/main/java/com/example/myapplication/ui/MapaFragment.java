package com.example.myapplication.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapplication.R;
import com.example.myapplication.model.PublicacionDetalleDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MapaFragment extends Fragment {

    private String direccionReal = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnComoLlegar = view.findViewById(R.id.btnComoLlegar);
        btnComoLlegar.setEnabled(false);
        btnComoLlegar.setText("Cargando ubicación...");

        Long idPublicacionPrueba = 1L;
        ApiService apiService = RetrofitClient.getApiService(getContext());

        apiService.getDetallePublicacion(idPublicacionPrueba).enqueue(new Callback<PublicacionDetalleDto>() {
            @Override
            public void onResponse(@NonNull Call<PublicacionDetalleDto> call, @NonNull Response<PublicacionDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    direccionReal = response.body().getZonaEntrega();
                    btnComoLlegar.setEnabled(true);
                    btnComoLlegar.setText("Cómo llegar (Abrir Maps)");
                } else {
                    Toast.makeText(getContext(), "Error al cargar la ubicación", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PublicacionDetalleDto> call, @NonNull Throwable t) {
                Log.e("MapaFragment", "Fallo la red: " + t.getMessage());
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });

        btnComoLlegar.setOnClickListener(v -> {
            if (direccionReal == null || direccionReal.isEmpty()) return;

            // Intent universal para abrir mapas o navegación
            Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(direccionReal));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);

            // Eliminamos el setPackage estricto para que Android elija la app de mapas instalada
            try {
                startActivity(mapIntent);
            } catch (Exception e) {
                Toast.makeText(getContext(), "No se encontró una aplicación de mapas instalada", Toast.LENGTH_SHORT).show();
            }
        });
    }
}