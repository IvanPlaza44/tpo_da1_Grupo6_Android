package com.example.myapplication.ui.explorar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.PublicacionEntity;
import com.example.myapplication.model.Publicacion.PaginaDto;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ExplorarFragment extends Fragment {

    private RecyclerView rvExplorar;
    private TextView tvSinConexion;
    private TextView tvVacio;
    private ExplorarAdapter adapter;

    // Room no permite operaciones en el hilo principal. Como el proyecto es
    // Java puro (sin coroutines), usamos el mismo patron que el demo de
    // Storage del profesor: un ExecutorService para todo lo que toca Room.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_explorar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvExplorar = view.findViewById(R.id.rvExplorar);
        tvSinConexion = view.findViewById(R.id.tvSinConexion);
        tvVacio = view.findViewById(R.id.tvVacio);

        rvExplorar.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExplorarAdapter(new ArrayList<>(), publicacion -> {
            Bundle args = new Bundle();
            args.putLong("publicacionId", publicacion.id);
            // Navegamos directo por id de destino (sin <action> en el XML):
            // publicacionDetalleFragment vive en product_nav_graph, un grafo
            // distinto al de este fragment (home_nav_graph). Definir una
            // <action> apuntando a un id interno de OTRO grafo incluido nos
            // rompio la app antes; navegar por id directo desde codigo es la
            // forma segura de cruzar grafos que ya validamos en este proyecto.
            Navigation.findNavController(view)
                    .navigate(R.id.publicacionDetalleFragment, args);
        });
        rvExplorar.setAdapter(adapter);

        cargarDesdeServidor();
    }

    private void cargarDesdeServidor() {
        ApiService apiService = RetrofitClient.getApiService(requireContext());
        apiService.explorar(0, 20).enqueue(new Callback<PaginaDto<PublicacionResumen>>() {
            @Override
            public void onResponse(@NonNull Call<PaginaDto<PublicacionResumen>> call,
                                   @NonNull Response<PaginaDto<PublicacionResumen>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<PublicacionResumen> lista = response.body().contenido;
                    tvSinConexion.setVisibility(View.GONE);
                    mostrarLista(lista);
                    guardarEnCache(lista);
                } else {
                    cargarDesdeCache();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginaDto<PublicacionResumen>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                cargarDesdeCache();
            }
        });
    }

    private void cargarDesdeCache() {
        executor.execute(() -> {
            List<PublicacionEntity> cache = AppDatabase.getInstance(requireContext())
                    .publicacionDao().obtenerTodas();

            List<PublicacionResumen> lista = new ArrayList<>();
            for (PublicacionEntity e : cache) {
                PublicacionResumen p = new PublicacionResumen();
                p.id = e.id;
                p.titulo = e.titulo;
                p.precio = e.precio;
                p.estado = e.estado;
                p.estadoArticulo = e.estadoArticulo;
                p.zonaEntrega = e.zonaEntrega;
                p.fotoPrincipal = e.fotoPrincipal;
                p.vendedorNombre = e.vendedorNombre;
                lista.add(p);
            }

            if (!isAdded()) return;
            requireActivity().runOnUiThread(() -> {
                tvSinConexion.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
                mostrarLista(lista);
            });
        });
    }

    private void guardarEnCache(List<PublicacionResumen> lista) {
        executor.execute(() -> {
            List<PublicacionEntity> entities = new ArrayList<>();
            int orden = 0;
            for (PublicacionResumen p : lista) {
                PublicacionEntity e = new PublicacionEntity();
                e.id = p.id;
                e.titulo = p.titulo;
                e.precio = p.precio;
                e.estado = p.estado;
                e.estadoArticulo = p.estadoArticulo;
                e.zonaEntrega = p.zonaEntrega;
                e.fotoPrincipal = p.fotoPrincipal;
                e.vendedorNombre = p.vendedorNombre;
                e.orden = orden++;
                entities.add(e);
            }

            AppDatabase db = AppDatabase.getInstance(requireContext());
            db.publicacionDao().borrarTodo();
            db.publicacionDao().insertarTodas(entities);
        });
    }

    private void mostrarLista(List<PublicacionResumen> lista) {
        adapter.actualizarLista(lista);
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        rvExplorar.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}