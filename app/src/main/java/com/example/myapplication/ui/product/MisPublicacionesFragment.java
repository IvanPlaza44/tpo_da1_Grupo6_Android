package com.example.myapplication.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class MisPublicacionesFragment extends Fragment {

    @Inject ApiService apiService;

    private RecyclerView rvMisPublicaciones;
    private View tvVacio;
    private PublicacionResumenAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mis_publicaciones, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvMisPublicaciones = view.findViewById(R.id.rvMisPublicaciones);
        tvVacio = view.findViewById(R.id.tvVacio);

        rvMisPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new PublicacionResumenAdapter(new ArrayList<>(), new PublicacionResumenAdapter.OnAccionListener() {
            @Override
            public void onPausar(PublicacionResumen p) {
                cambiarEstado(p.id, "pausar");
            }

            @Override
            public void onReactivar(PublicacionResumen p) {
                cambiarEstado(p.id, "reactivar");
            }

            @Override
            public void onVerDetalle(PublicacionResumen p) {
                Bundle args = new Bundle();
                args.putLong("publicacionId", p.id);
                androidx.navigation.Navigation.findNavController(requireView())
                        .navigate(R.id.publicacionDetalleFragment, args);
            }
        });
        rvMisPublicaciones.setAdapter(adapter);

        cargarMisPublicaciones();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) cargarMisPublicaciones();
    }

    private void cargarMisPublicaciones() {
        apiService.misPublicaciones().enqueue(new Callback<List<PublicacionResumen>>() {
            @Override
            public void onResponse(@NonNull Call<List<PublicacionResumen>> call,
                                   @NonNull Response<List<PublicacionResumen>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<PublicacionResumen> lista = response.body();
                    adapter.actualizarLista(lista);
                    tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
                    rvMisPublicaciones.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
                } else {
                    Toast.makeText(requireContext(), "No se pudieron cargar tus publicaciones", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<PublicacionResumen>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cambiarEstado(long id, String accion) {
        Call<Void> call = accion.equals("pausar") ? apiService.pausar(id) : apiService.reactivar(id);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(),
                            accion.equals("pausar") ? "Publicacion pausada" : "Publicacion reactivada",
                            Toast.LENGTH_SHORT).show();
                    cargarMisPublicaciones();
                } else {
                    Toast.makeText(requireContext(), "No se pudo actualizar el estado", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Error de conexion: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}