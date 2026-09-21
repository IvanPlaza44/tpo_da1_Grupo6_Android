package com.example.myapplication.ui.busquedas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.BusquedaGuardadaResponseDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class BusquedasGuardadasFragment extends Fragment {

    private ApiService apiService;
    private BusquedasGuardadasAdapter adapter;
    private Call<List<BusquedaGuardadaResponseDto>> llamadaEnCurso;

    private RecyclerView rvBusquedasGuardadas;
    private ProgressBar progressBar;
    private TextView tvVacio;
    private View contenedorError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_busquedas_guardadas, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getApiService(requireContext());

        rvBusquedasGuardadas = view.findViewById(R.id.rvBusquedasGuardadas);
        progressBar = view.findViewById(R.id.progressBar);
        tvVacio = view.findViewById(R.id.tvVacio);
        contenedorError = view.findViewById(R.id.contenedorError);

        rvBusquedasGuardadas.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BusquedasGuardadasAdapter(new ArrayList<>());
        rvBusquedasGuardadas.setAdapter(adapter);

        view.findViewById(R.id.btnReintentar).setOnClickListener(v -> cargarBusquedas(true));
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarBusquedas(adapter == null || adapter.getItemCount() == 0);
    }

    @Override
    public void onDestroyView() {
        if (llamadaEnCurso != null) {
            llamadaEnCurso.cancel();
            llamadaEnCurso = null;
        }
        super.onDestroyView();
    }

    private void cargarBusquedas(boolean mostrarProgress) {
        if (llamadaEnCurso != null) {
            llamadaEnCurso.cancel();
        }
        if (mostrarProgress) {
            mostrarCargando();
        }

        llamadaEnCurso = apiService.listarBusquedasGuardadas();
        llamadaEnCurso.enqueue(new Callback<List<BusquedaGuardadaResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<BusquedaGuardadaResponseDto>> call,
                                   @NonNull Response<List<BusquedaGuardadaResponseDto>> response) {
                if (!isAdded() || call.isCanceled()) return;
                llamadaEnCurso = null;
                if (response.isSuccessful() && response.body() != null) {
                    List<BusquedaGuardadaResponseDto> lista = response.body();
                    adapter.actualizarLista(lista);
                    if (lista.isEmpty()) {
                        mostrarVacio();
                    } else {
                        mostrarLista();
                    }
                } else {
                    mostrarError();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<BusquedaGuardadaResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                llamadaEnCurso = null;
                mostrarError();
            }
        });
    }

    private void mostrarCargando() {
        progressBar.setVisibility(View.VISIBLE);
        rvBusquedasGuardadas.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarLista() {
        progressBar.setVisibility(View.GONE);
        rvBusquedasGuardadas.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarVacio() {
        progressBar.setVisibility(View.GONE);
        rvBusquedasGuardadas.setVisibility(View.GONE);
        tvVacio.setVisibility(View.VISIBLE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarError() {
        progressBar.setVisibility(View.GONE);
        rvBusquedasGuardadas.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.VISIBLE);
    }
}
