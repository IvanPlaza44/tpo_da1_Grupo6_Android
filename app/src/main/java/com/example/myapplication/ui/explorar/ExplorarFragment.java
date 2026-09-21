package com.example.myapplication.ui.explorar;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.local.AppDatabase;
import com.example.myapplication.data.local.PublicacionEntity;
import com.example.myapplication.model.Publicacion.CategoriaDto;
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

    private static final int TAMANIO_PAGINA = 5;
    private static final int UMBRAL_PAGINACION = 3;
    private static final String[] ESTADOS_ARTICULO_FILTRO = {"Todos", "NUEVO", "COMO_NUEVO", "USADO"};
    private static final String[] ORDENES_VISIBLE = {"Más recientes", "Menor precio", "Mayor precio"};
    private static final String[] ORDENES_BACKEND = {"RECIENTES", "MENOR_PRECIO", "MAYOR_PRECIO"};

    private RecyclerView rvExplorar;
    private TextView tvSinConexion;
    private TextView tvVacio;
    private View contenedorError;
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
    private Long categoriaId = null;
    private Double precioMin = null;
    private Double precioMax = null;
    private String estadoArticulo = null;
    private String zona = null;
    private String ordenActual = "RECIENTES";
    private boolean ignorarPrimerOrden = true;
    private final List<CategoriaDto> categorias = new ArrayList<>();
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
        contenedorError = view.findViewById(R.id.contenedorError);
        view.findViewById(R.id.btnReintentar).setOnClickListener(v -> cargarPaginaInicial());
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
        view.findViewById(R.id.btnFiltros).setOnClickListener(v -> mostrarDialogFiltros());

        Spinner spOrden = view.findViewById(R.id.spOrden);
        spOrden.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ORDENES_VISIBLE));
        spOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                if (ignorarPrimerOrden) {
                    ignorarPrimerOrden = false;
                    return;
                }
                ordenActual = ORDENES_BACKEND[position];
                cargarPaginaInicial();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                ordenActual = "RECIENTES";
            }
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

        cargarCategorias();
        cargarPaginaInicial();
    }

    private void aplicarBusqueda(String texto) {
        String trimmed = texto == null ? "" : texto.trim();
        queryActual = trimmed.isEmpty() ? null : trimmed;
        cargarPaginaInicial();
    }

    private void cargarCategorias() {
        RetrofitClient.getApiService(requireContext()).obtenerCategorias()
                .enqueue(new Callback<List<CategoriaDto>>() {
                    @Override
                    public void onResponse(@NonNull Call<List<CategoriaDto>> call,
                                           @NonNull Response<List<CategoriaDto>> response) {
                        if (!isAdded() || !response.isSuccessful() || response.body() == null) return;
                        categorias.clear();
                        categorias.addAll(response.body());
                    }

                    @Override
                    public void onFailure(@NonNull Call<List<CategoriaDto>> call, @NonNull Throwable t) {
                        // El dialog puede seguir usandose con "Todas" y el resto de filtros.
                    }
                });
    }

    private void mostrarDialogFiltros() {
        View contenido = getLayoutInflater().inflate(R.layout.dialog_filtros_explorar, null);
        Spinner spCategoria = contenido.findViewById(R.id.spCategoriaFiltro);
        EditText etPrecioMin = contenido.findViewById(R.id.etPrecioMin);
        EditText etPrecioMax = contenido.findViewById(R.id.etPrecioMax);
        Spinner spEstado = contenido.findViewById(R.id.spEstadoArticuloFiltro);
        EditText etZona = contenido.findViewById(R.id.etZonaFiltro);

        List<String> nombresCategoria = new ArrayList<>();
        nombresCategoria.add("Todas");
        for (CategoriaDto c : categorias) {
            nombresCategoria.add(c.nombre);
        }
        spCategoria.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, nombresCategoria));
        spCategoria.setSelection(indiceCategoriaAplicada());

        if (precioMin != null) etPrecioMin.setText(String.valueOf(precioMin));
        if (precioMax != null) etPrecioMax.setText(String.valueOf(precioMax));

        spEstado.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ESTADOS_ARTICULO_FILTRO));
        spEstado.setSelection(indiceEstadoAplicado());

        if (zona != null) etZona.setText(zona);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Filtros")
                .setView(contenido)
                .create();

        contenido.findViewById(R.id.btnAplicarFiltros).setOnClickListener(v -> {
            if (aplicarFiltrosDesdeDialog(spCategoria, etPrecioMin, etPrecioMax, spEstado, etZona)) {
                dialog.dismiss();
                cargarPaginaInicial();
            }
        });
        contenido.findViewById(R.id.btnLimpiarFiltros).setOnClickListener(v -> {
            categoriaId = null;
            precioMin = null;
            precioMax = null;
            estadoArticulo = null;
            zona = null;
            dialog.dismiss();
            cargarPaginaInicial();
        });

        dialog.show();
    }

    private boolean aplicarFiltrosDesdeDialog(Spinner spCategoria, EditText etPrecioMin,
                                              EditText etPrecioMax, Spinner spEstado, EditText etZona) {
        Double min;
        Double max;
        try {
            min = parsePrecio(etPrecioMin.getText().toString());
            max = parsePrecio(etPrecioMax.getText().toString());
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Precio invalido", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (min != null && max != null && min > max) {
            Toast.makeText(requireContext(),
                    "El precio minimo no puede ser mayor al maximo",
                    Toast.LENGTH_SHORT).show();
            return false;
        }

        int catPos = spCategoria.getSelectedItemPosition();
        categoriaId = catPos <= 0 ? null : categorias.get(catPos - 1).id;

        int estadoPos = spEstado.getSelectedItemPosition();
        estadoArticulo = estadoPos <= 0 ? null : ESTADOS_ARTICULO_FILTRO[estadoPos];

        String zonaTexto = etZona.getText().toString().trim();
        zona = zonaTexto.isEmpty() ? null : zonaTexto;
        precioMin = min;
        precioMax = max;
        return true;
    }

    private int indiceCategoriaAplicada() {
        if (categoriaId == null) return 0;
        for (int i = 0; i < categorias.size(); i++) {
            if (categorias.get(i).id == categoriaId) return i + 1;
        }
        return 0;
    }

    private int indiceEstadoAplicado() {
        if (estadoArticulo == null) return 0;
        for (int i = 1; i < ESTADOS_ARTICULO_FILTRO.length; i++) {
            if (ESTADOS_ARTICULO_FILTRO[i].equals(estadoArticulo)) return i;
        }
        return 0;
    }

    private Double parsePrecio(String texto) {
        if (texto == null) return null;
        String trimmed = texto.trim();
        if (trimmed.isEmpty()) return null;
        return Double.parseDouble(trimmed.replace(',', '.'));
    }

    private boolean esCatalogoGeneral() {
        return queryActual == null
                && categoriaId == null
                && precioMin == null
                && precioMax == null
                && estadoArticulo == null
                && zona == null
                && "RECIENTES".equals(ordenActual);
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
        callEnVuelo = apiService.explorar(
                pagina,
                TAMANIO_PAGINA,
                queryActual,
                categoriaId,
                precioMin,
                precioMax,
                estadoArticulo,
                zona,
                ordenActual
        );
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
                        if (esCatalogoGeneral()) {
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
                    manejarFalloPaginaSiguiente();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PaginaDto<PublicacionResumen>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                if (esInicial) {
                    manejarFalloCargaInicial();
                } else {
                    manejarFalloPaginaSiguiente();
                }
            }
        });
    }

    private void manejarFalloCargaInicial() {
        if (!esCatalogoGeneral()) {
            mostrarErrorConsulta();
            return;
        }
        cargarDesdeCache();
    }

    private void manejarFalloPaginaSiguiente() {
        finalizarCarga(false);
        Toast.makeText(requireContext(),
                "No se pudo cargar mas publicaciones",
                Toast.LENGTH_SHORT).show();
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
                if (lista.isEmpty()) {
                    mostrarErrorConsulta();
                    return;
                }
                tvSinConexion.setVisibility(View.VISIBLE);
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
        contenedorError.setVisibility(View.GONE);
        tvSinConexion.setVisibility(View.GONE);
    }

    private void mostrarLista(List<PublicacionResumen> lista) {
        progressBar.setVisibility(View.GONE);
        contenedorError.setVisibility(View.GONE);
        adapter.actualizarLista(lista);
        tvVacio.setText(esCatalogoGeneral()
                ? "Todavía no hay publicaciones."
                : "No encontramos publicaciones con esos criterios.");
        tvVacio.setVisibility(lista.isEmpty() ? View.VISIBLE : View.GONE);
        rvExplorar.setVisibility(lista.isEmpty() ? View.GONE : View.VISIBLE);
    }

    private void mostrarErrorConsulta() {
        tvSinConexion.setVisibility(View.GONE);
        progressBar.setVisibility(View.GONE);
        progressBarSiguiente.setVisibility(View.GONE);
        rvExplorar.setVisibility(View.GONE);
        tvVacio.setVisibility(View.GONE);
        contenedorError.setVisibility(View.VISIBLE);
        cargandoInicial = false;
        cargandoSiguiente = false;
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
