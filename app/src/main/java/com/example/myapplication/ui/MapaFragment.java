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

    private String direccionReal = ""; // Arranca vacío hasta que la API responda

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mapa, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Button btnComoLlegar = view.findViewById(R.id.btnComoLlegar);

        // Deshabilitamos el botón un segundo hasta que llegue el dato de la API
        btnComoLlegar.setEnabled(false);
        btnComoLlegar.setText("Cargando ubicación...");

        // Llamada a la API. Usamos el ID 1 de prueba, después esto te lo pasan por Argumentos
        Long idPublicacionPrueba = 1L;
        ApiService apiService = RetrofitClient.getApiService();

        apiService.getDetallePublicacion(idPublicacionPrueba).enqueue(new Callback<PublicacionDetalleDto>() {
            @Override
            public void onResponse(Call<PublicacionDetalleDto> call, Response<PublicacionDetalleDto> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // ¡Llegó el dato del backend de Iván!
                    direccionReal = response.body().getZonaEntrega();

                    btnComoLlegar.setEnabled(true);
                    btnComoLlegar.setText("Cómo llegar (Abrir Maps)");
                } else {
                    Toast.makeText(getContext(), "Error al cargar la ubicación", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<PublicacionDetalleDto> call, Throwable t) {
                Log.e("MapaFragment", "Fallo la red: " + t.getMessage());
                Toast.makeText(getContext(), "Error de red", Toast.LENGTH_SHORT).show();
            }
        });

        // Configuración del clic del botón
        btnComoLlegar.setOnClickListener(v -> {
            if (direccionReal.isEmpty()) return;

            // Le pasamos la dirección real (en texto o coordenadas) al intent de Google
            Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(direccionReal));
            Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
            mapIntent.setPackage("com.google.android.apps.maps");

            if (mapIntent.resolveActivity(requireActivity().getPackageManager()) != null) {
                startActivity(mapIntent);
            } else {
                Intent defaultMapIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(direccionReal)));
                startActivity(defaultMapIntent);
            }
        });
    }
}