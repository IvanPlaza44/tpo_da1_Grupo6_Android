package com.example.myapplication.ui.explorar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ProgressBar;
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

    private static final int TAMANIO_PAGINA = 20;
    private static final int UMBRAL_PAGINACION = 3;

    private RecyclerView rvExplorar;
    private TextView tvSinConexion;
    private TextView tvVacio;
    private ProgressBar progressBar;
    private ProgressBar progressBarSiguiente;
    private EditText etBuscar;
    private ExplorarAdapter adapter;

    // totalPaginas solo se setea con una respuesta valida del backend.
    // 0 = todavia no hay dato (o el backend dijo 0): no se piden mas paginas.
    private int paginaActual = -1;
    private int totalPaginas = 0;
    private boolean cargandoInicial = false;
    private boolean cargandoSiguiente = false;
    // null = listado general; texto = busqueda activa (se reusa en cada pagina).
    private String queryActual = null;
    private Call<PaginaDto<PublicacionResumen>> callEnVuelo;

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
        progressBar = view.findViewById(R.id.progressBar);
        progressBarSiguiente = view.findViewById(R.id.progressBarSiguiente);
        etBuscar = view.findViewById(R.id.etBuscar);
        etBuscar.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                aplicarBusqueda(etBuscar.getText().toString());
                return true;
            }
            return false;
        });

        rvExplorar.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExplorarAdapter(new ArrayList<>(), publicacion -> {
            Bundle args = new Bundle();
            args.putLong("publicacionId", publicacion.id);
            // publicacionDetalleFragment también está declarado en este grafo
            // (home_nav_graph). No se puede navegar al id interno de
            // product_nav_graph: con <include> ese destino no es visible
            // desde explorarFragment.
            Navigation.findNavController(view)
                    .navigate(R.id.action_explorarFragment_to_publicacionDetalleFragment, args);
        });
        rvExplorar.setAdapter(adapter);
        rvExplorar.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                intentarCargarSiCercaDelFinal();
            }
        });

        cargarPaginaInicial();
    }

    private void aplicarBusqueda(String texto) {
        String trimmed = texto == null ? "" : texto.trim();
        queryActual = trimmed.isEmpty() ? null : trimmed;
        cargarPaginaInicial();
    }

    private void cargarPaginaInicial() {
        cargandoInicial = true;
        cargandoSiguiente = false;
        paginaActual = -1;
        totalPaginas = 0;
        mostrarCarga();
        pedirPagina(0, true);
    }

    private void intentarCargarSiCercaDelFinal() {
        LinearLayoutManager lm = (LinearLayoutManager) rvExplorar.getLayoutManager();
        if (lm == null) return;
        int ultimoVisible = lm.findLastVisibleItemPosition();
        int total = lm.getItemCount();
        if (total > 0 && ultimoVisible >= total - UMBRAL_PAGINACION) {
            cargarSiguientePagina();
        }
    }

    private void cargarSiguientePagina() {
        if (cargandoInicial || cargandoSiguiente) return;
        if (paginaActual + 1 >= totalPaginas) return;

        cargandoSiguiente = true;
        progressBarSiguiente.setVisibility(View.VISIBLE);
        pedirPagina(paginaActual + 1, false);
    }

    private void pedirPagina(int pagina, boolean esInicial) {
        if (callEnVuelo != null) {
            callEnVuelo.cancel();
        }

        ApiService apiService = RetrofitClient.getApiService(requireContext());
        callEnVuelo = apiService.explorar(pagina, TAMANIO_PAGINA, queryActual);
        callEnVuelo.enqueue(new Callback<PaginaDto<PublicacionResumen>>() {
            @Override
            public void onResponse(@NonNull Call<PaginaDto<PublicacionResumen>> call,
                                   @NonNull Response<PaginaDto<PublicacionResumen>> response) {
                if (!isAdded() || call.isCanceled()) return;

                if (response.isSuccessful() && response.body() != null) {
                    PaginaDto<PublicacionResumen> body = response.body();
                    List<PublicacionResumen> lista = body.contenido != null
                            ? body.contenido
                            : new ArrayList<>();

                    totalPaginas = body.totalPaginas;
                    paginaActual = pagina;
                    tvSinConexion.setVisibility(View.GONE);

                    if (esInicial) {
                        mostrarLista(lista);
                        // El cache es del catalogo general, no de una busqueda.
                        if (queryActual == null) {
                            guardarEnCache(lista);
                        }
                    } else {
                        adapter.agregarItems(lista);
                    }
                    finalizarCarga(esInicial);
                    rvExplorar.post(ExplorarFragment.this::intentarCargarSiCercaDelFinal);
                } else if (esInicial) {
                    manejarFalloCargaInicial();
                } else {
                    finalizarCarga(false);
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginaDto<PublicacionResumen>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                if (esInicial) {
                    manejarFalloCargaInicial();
                } else {
                    finalizarCarga(false);
                }
            }
        });
    }

    private void manejarFalloCargaInicial() {
        if (queryActual != null) {
            tvSinConexion.setVisibility(View.GONE);
            mostrarLista(new ArrayList<>());
            finalizarCarga(true);
            return;
        }
        cargarDesdeCache();
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
                finalizarCarga(true);
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

    private void mostrarCarga() {
        progressBar.setVisibility(View.VISIBLE);
        progressBarSiguiente.setVisibility(View.GONE);
        rvExplorar.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
    }

    private void mostrarLista(List<PublicacionResumen> lista) {
        progressBar.setVisibility(View.GONE);
        adapter.actualizarLista(lista);
        tvVacio.setText(queryActual != null
                ? "No hay resultados para esta busqueda"
                : "No hay publicaciones para mostrar");
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        rvExplorar.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void finalizarCarga(boolean esInicial) {
        if (esInicial) {
            cargandoInicial = false;
            progressBar.setVisibility(View.GONE);
        } else {
            cargandoSiguiente = false;
            progressBarSiguiente.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (callEnVuelo != null) {
            callEnVuelo.cancel();
        }
        executor.shutdown();
    }
}