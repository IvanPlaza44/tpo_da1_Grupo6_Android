package com.example.myapplication.ui.favoritos;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.FavoritoResponseDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class FavoritosFragment extends Fragment {

    private ApiService apiService;
    private FavoritosAdapter adapter;
    private Call<List<FavoritoResponseDto>> llamadaEnCurso;

    private RecyclerView rvFavoritos;
    private ProgressBar progressBar;
    private TextView tvVacio;
    private View contenedorError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_favoritos, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        apiService = RetrofitClient.getApiService(requireContext());

        rvFavoritos = view.findViewById(R.id.rvFavoritos);
        progressBar = view.findViewById(R.id.progressBar);
        tvVacio = view.findViewById(R.id.tvVacio);
        contenedorError = view.findViewById(R.id.contenedorError);

        rvFavoritos.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new FavoritosAdapter(new ArrayList<>(), publicacion -> {
            Bundle args = new Bundle();
            args.putLong("publicacionId", publicacion.id);
            Navigation.findNavController(view)
                    .navigate(R.id.action_favoritosFragment_to_publicacionDetalleFragment, args);
        });
        rvFavoritos.setAdapter(adapter);

        view.findViewById(R.id.btnReintentar).setOnClickListener(v -> cargarFavoritos(true));
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarFavoritos(adapter == null || adapter.getItemCount() == 0);
    }

    @Override
    public void onDestroyView() {
        if (llamadaEnCurso != null) {
            llamadaEnCurso.cancel();
            llamadaEnCurso = null;
        }
        super.onDestroyView();
    }

    private void cargarFavoritos(boolean mostrarProgress) {
        if (llamadaEnCurso != null) {
            llamadaEnCurso.cancel();
        }
        if (mostrarProgress) {
            mostrarCargando();
        }

        llamadaEnCurso = apiService.listarFavoritos();
        llamadaEnCurso.enqueue(new Callback<List<FavoritoResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<FavoritoResponseDto>> call,
                                   @NonNull Response<List<FavoritoResponseDto>> response) {
                if (!isAdded() || call.isCanceled()) return;
                llamadaEnCurso = null;
                if (response.isSuccessful() && response.body() != null) {
                    List<FavoritoResponseDto> lista = response.body();
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
            public void onFailure(@NonNull Call<List<FavoritoResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                llamadaEnCurso = null;
                mostrarError();
            }
        });
    }

    private void mostrarCargando() {
        progressBar.setVisibility(View.VISIBLE);
        rvFavoritos.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarLista() {
        progressBar.setVisibility(View.GONE);
        rvFavoritos.setVisibility(View.VISIBLE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarVacio() {
        progressBar.setVisibility(View.GONE);
        rvFavoritos.setVisibility(View.GONE);
        tvVacio.setVisibility(View.VISIBLE);
        contenedorError.setVisibility(View.GONE);
    }

    private void mostrarError() {
        progressBar.setVisibility(View.GONE);
        rvFavoritos.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.VISIBLE);
    }
}
