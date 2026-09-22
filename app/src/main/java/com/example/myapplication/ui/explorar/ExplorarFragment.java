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
import com.example.myapplication.model.BusquedaGuardadaRequestDto;
import com.example.myapplication.model.BusquedaGuardadaResponseDto;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.Publicacion.FavoritoResponseDto;
import com.example.myapplication.model.Publicacion.PaginaDto;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.session.SessionManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.inject.Inject;

import com.google.android.material.switchmaterial.SwitchMaterial;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class ExplorarFragment extends Fragment {

    private static final int TAMANIO_PAGINA = 5;
    private static final int UMBRAL_PAGINACION = 3;
    private static final String[] ESTADOS_ARTICULO_FILTRO = {"Todos", "NUEVO", "COMO_NUEVO", "USADO"};
    private static final String[] ORDENES_VISIBLE = {"Más recientes", "Menor precio", "Mayor precio"};
    private static final String[] ORDENES_BACKEND = {"RECIENTES", "MENOR_PRECIO", "MAYOR_PRECIO"};

    public static final String ARG_APLICAR_BUSQUEDA = "aplicarBusquedaGuardada";
    public static final String ARG_QUERY = "query";
    public static final String ARG_CATEGORIA_ID = "categoriaId";
    public static final String ARG_PRECIO_MIN = "precioMin";
    public static final String ARG_PRECIO_MAX = "precioMax";
    public static final String ARG_ESTADO_ARTICULO = "estadoArticulo";
    public static final String ARG_ZONA = "zona";

    @Inject ApiService apiService;
    @Inject SessionManager sessionManager;

    private RecyclerView rvExplorar;
    private TextView tvSinConexion;
    private TextView tvVacio;
    private View contenedorError;
    private ProgressBar progressBar;
    private ProgressBar progressBarSiguiente;
    private EditText etBuscar;
    private Spinner spOrden;
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
    /** Filtro de cercanía: usa {@link #zona} obtenida de GET /me al aplicar. */
    private boolean usarMiZona = false;
    private String ordenActual = "RECIENTES";
    // true hasta terminar el setup (y setSelection programático). Evita un
    // segundo GET si el Spinner dispara onItemSelected al poner RECIENTES.
    private boolean silenciarCambioOrden = true;
    private final List<CategoriaDto> categorias = new ArrayList<>();
    private Call<PaginaDto<PublicacionResumen>> callEnVuelo;
    private Call<BusquedaGuardadaResponseDto> callGuardarBusqueda;
    private Call<List<FavoritoResponseDto>> callFavoritos;
    private Call<List<PublicacionResumen>> callMisPublicaciones;
    private boolean guardandoBusqueda = false;

    private final Set<Long> favoritoIds = new HashSet<>();
    private final Set<Long> publicacionPropiaIds = new HashSet<>();
    private final Set<Long> favoritosEnActualizacion = new HashSet<>();

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
        view.findViewById(R.id.btnGuardarBusqueda).setOnClickListener(v -> mostrarDialogGuardarBusqueda());

        spOrden = view.findViewById(R.id.spOrden);
        spOrden.setAdapter(new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, ORDENES_VISIBLE));
        spOrden.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View selectedView, int position, long id) {
                if (silenciarCambioOrden) {
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
            Navigation.findNavController(view)
                    .navigate(R.id.action_explorarFragment_to_publicacionDetalleFragment, args);
        }, this::alternarFavorito);
        rvExplorar.setAdapter(adapter);
        rvExplorar.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                if (dy <= 0) return;
                intentarCargarSiCercaDelFinal();
            }
        });

        Bundle args = getArguments();
        if (args != null && args.getBoolean(ARG_APLICAR_BUSQUEDA, false)) {
            aplicarCriteriosDesdeArgs(args);
        }

        cargarCategorias();
        cargarEstadoFavoritos();
        cargarPaginaInicial();
        spOrden.post(() -> silenciarCambioOrden = false);
    }

    @Override
    public void onResume() {
        super.onResume();
        cargarEstadoFavoritos();
    }

    private void cargarEstadoFavoritos() {
        if (!sessionManager.isLoggedIn()) {
            favoritoIds.clear();
            publicacionPropiaIds.clear();
            if (adapter != null) {
                adapter.actualizarEstadoFavoritos(favoritoIds, publicacionPropiaIds, false);
            }
            return;
        }

        if (callFavoritos != null) {
            callFavoritos.cancel();
        }
        callFavoritos = apiService.listarFavoritos();
        callFavoritos.enqueue(new Callback<List<FavoritoResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<FavoritoResponseDto>> call,
                                   @NonNull Response<List<FavoritoResponseDto>> response) {
                if (!isAdded() || call.isCanceled()) return;
                favoritoIds.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (FavoritoResponseDto favorito : response.body()) {
                        if (favorito.publicacion != null) {
                            favoritoIds.add(favorito.publicacion.id);
                        }
                    }
                }
                publicarEstadoFavoritosEnAdapter(true);
            }

            @Override
            public void onFailure(@NonNull Call<List<FavoritoResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                publicarEstadoFavoritosEnAdapter(true);
            }
        });

        if (callMisPublicaciones != null) {
            callMisPublicaciones.cancel();
        }
        callMisPublicaciones = apiService.misPublicaciones();
        callMisPublicaciones.enqueue(new Callback<List<PublicacionResumen>>() {
            @Override
            public void onResponse(@NonNull Call<List<PublicacionResumen>> call,
                                   @NonNull Response<List<PublicacionResumen>> response) {
                if (!isAdded() || call.isCanceled()) return;
                publicacionPropiaIds.clear();
                if (response.isSuccessful() && response.body() != null) {
                    for (PublicacionResumen p : response.body()) {
                        publicacionPropiaIds.add(p.id);
                    }
                }
                publicarEstadoFavoritosEnAdapter(true);
            }

            @Override
            public void onFailure(@NonNull Call<List<PublicacionResumen>> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                publicarEstadoFavoritosEnAdapter(true);
            }
        });
    }

    private void publicarEstadoFavoritosEnAdapter(boolean habilitados) {
        if (adapter != null) {
            adapter.actualizarEstadoFavoritos(favoritoIds, publicacionPropiaIds, habilitados);
        }
    }

    private void alternarFavorito(PublicacionResumen publicacion, boolean esFavorito) {
        if (publicacion == null || favoritosEnActualizacion.contains(publicacion.id)) {
            return;
        }

        favoritosEnActualizacion.add(publicacion.id);
        Call<Void> call = esFavorito
                ? apiService.quitarFavorito(publicacion.id)
                : apiService.agregarFavorito(publicacion.id);

        call.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded() || call.isCanceled()) return;
                favoritosEnActualizacion.remove(publicacion.id);
                if (response.isSuccessful()) {
                    if (esFavorito) {
                        favoritoIds.remove(publicacion.id);
                    } else {
                        favoritoIds.add(publicacion.id);
                    }
                    int indice = adapter.indicePorId(publicacion.id);
                    if (indice >= 0) {
                        adapter.notifyItemChanged(indice);
                    }
                } else {
                    Toast.makeText(requireContext(),
                            "No se pudo actualizar el favorito",
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                favoritosEnActualizacion.remove(publicacion.id);
                Toast.makeText(requireContext(),
                        "No se pudo actualizar el favorito",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarCriteriosDesdeArgs(Bundle args) {
        queryActual = textoONull(args.getString(ARG_QUERY));
        categoriaId = args.containsKey(ARG_CATEGORIA_ID) ? args.getLong(ARG_CATEGORIA_ID) : null;
        precioMin = args.containsKey(ARG_PRECIO_MIN) ? args.getDouble(ARG_PRECIO_MIN) : null;
        precioMax = args.containsKey(ARG_PRECIO_MAX) ? args.getDouble(ARG_PRECIO_MAX) : null;
        estadoArticulo = textoONull(args.getString(ARG_ESTADO_ARTICULO));
        zona = textoONull(args.getString(ARG_ZONA));
        usarMiZona = false;
        ordenActual = "RECIENTES";

        etBuscar.setText(queryActual != null ? queryActual : "");
        silenciarCambioOrden = true;
        spOrden.setSelection(0);
    }

    private static String textoONull(String valor) {
        if (valor == null) return null;
        String trimmed = valor.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void aplicarBusqueda(String texto) {
        String trimmed = texto == null ? "" : texto.trim();
        queryActual = trimmed.isEmpty() ? null : trimmed;
        cargarPaginaInicial();
    }

    private void mostrarDialogGuardarBusqueda() {
        if (guardandoBusqueda) return;

        EditText etNombre = new EditText(requireContext());
        etNombre.setHint("Nombre de la búsqueda");
        etNombre.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        etNombre.setMaxLines(1);
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etNombre.setPadding(padding, padding, padding, padding);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Guardar búsqueda")
                .setView(etNombre)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Guardar", null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String nombre = etNombre.getText() != null ? etNombre.getText().toString().trim() : "";
            if (nombre.isEmpty()) {
                Toast.makeText(requireContext(), "Ingresá un nombre", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            guardarBusquedaActual(nombre);
        }));

        dialog.show();
    }

    private void guardarBusquedaActual(String nombre) {
        if (guardandoBusqueda) return;

        guardandoBusqueda = true;
        if (callGuardarBusqueda != null) {
            callGuardarBusqueda.cancel();
        }

        BusquedaGuardadaRequestDto body = new BusquedaGuardadaRequestDto();
        body.nombre = nombre;
        body.query = queryActual;
        body.categoriaId = categoriaId;
        body.precioMin = precioMin;
        body.precioMax = precioMax;
        body.estadoArticulo = estadoArticulo;
        body.zona = zona;

        callGuardarBusqueda = apiService.crearBusquedaGuardada(body);
        callGuardarBusqueda.enqueue(new Callback<BusquedaGuardadaResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<BusquedaGuardadaResponseDto> call,
                                   @NonNull Response<BusquedaGuardadaResponseDto> response) {
                if (!isAdded() || call.isCanceled()) return;
                guardandoBusqueda = false;
                callGuardarBusqueda = null;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Búsqueda guardada", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(requireContext(), "No se pudo guardar la búsqueda", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<BusquedaGuardadaResponseDto> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                guardandoBusqueda = false;
                callGuardarBusqueda = null;
                Toast.makeText(requireContext(), "No se pudo guardar la búsqueda", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarCategorias() {
        apiService.obtenerCategorias()
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
        SwitchMaterial switchUsarMiZona = contenido.findViewById(R.id.switchUsarMiZona);
        View tilZonaFiltro = contenido.findViewById(R.id.tilZonaFiltro);

        boolean loggedIn = sessionManager.isLoggedIn();
        if (loggedIn) {
            switchUsarMiZona.setVisibility(View.VISIBLE);
            switchUsarMiZona.setChecked(usarMiZona);
        } else {
            switchUsarMiZona.setVisibility(View.GONE);
            switchUsarMiZona.setChecked(false);
        }

        actualizarCampoZonaManual(etZona, tilZonaFiltro, switchUsarMiZona.isChecked());
        switchUsarMiZona.setOnCheckedChangeListener((buttonView, isChecked) ->
                actualizarCampoZonaManual(etZona, tilZonaFiltro, isChecked));

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

        if (zona != null) {
            etZona.setText(zona);
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Filtros")
                .setView(contenido)
                .create();

        View btnAplicar = contenido.findViewById(R.id.btnAplicarFiltros);
        btnAplicar.setOnClickListener(v -> {
            if (loggedIn && switchUsarMiZona.isChecked()) {
                btnAplicar.setEnabled(false);
                aplicarFiltrosConMiZona(spCategoria, etPrecioMin, etPrecioMax, spEstado, dialog, btnAplicar);
            } else {
                usarMiZona = false;
                if (aplicarFiltrosDesdeDialog(spCategoria, etPrecioMin, etPrecioMax, spEstado, etZona)) {
                    dialog.dismiss();
                    cargarPaginaInicial();
                }
            }
        });
        contenido.findViewById(R.id.btnLimpiarFiltros).setOnClickListener(v -> {
            categoriaId = null;
            precioMin = null;
            precioMax = null;
            estadoArticulo = null;
            zona = null;
            usarMiZona = false;
            dialog.dismiss();
            cargarPaginaInicial();
        });

        dialog.show();
    }

    private void actualizarCampoZonaManual(EditText etZona, View tilZonaFiltro, boolean usarMiZonaActivo) {
        etZona.setEnabled(!usarMiZonaActivo);
        tilZonaFiltro.setEnabled(!usarMiZonaActivo);
        if (usarMiZonaActivo) {
            etZona.setAlpha(0.6f);
        } else {
            etZona.setAlpha(1f);
        }
    }

    private void aplicarFiltrosConMiZona(Spinner spCategoria, EditText etPrecioMin, EditText etPrecioMax,
                                         Spinner spEstado, AlertDialog dialog, View btnAplicar) {
        if (!validarPreciosDesdeDialog(etPrecioMin, etPrecioMax)) {
            btnAplicar.setEnabled(true);
            return;
        }

        apiService.obtenerMiPerfil().enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(@NonNull Call<Usuario> call, @NonNull Response<Usuario> response) {
                if (!isAdded()) return;
                btnAplicar.setEnabled(true);

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "No se pudo cargar tu perfil", Toast.LENGTH_SHORT).show();
                    return;
                }

                String zonaPerfil = response.body().getZona();
                if (zonaPerfil == null || zonaPerfil.trim().isEmpty()) {
                    Toast.makeText(requireContext(),
                            R.string.filter_zona_perfil_incompleto,
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                usarMiZona = true;
                zona = zonaPerfil.trim();
                aplicarCategoriaEstadoPrecioDesdeDialog(spCategoria, etPrecioMin, etPrecioMax, spEstado);
                dialog.dismiss();
                cargarPaginaInicial();
            }

            @Override
            public void onFailure(@NonNull Call<Usuario> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                btnAplicar.setEnabled(true);
                Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean validarPreciosDesdeDialog(EditText etPrecioMin, EditText etPrecioMax) {
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
        return true;
    }

    private void aplicarCategoriaEstadoPrecioDesdeDialog(Spinner spCategoria, EditText etPrecioMin,
                                                         EditText etPrecioMax, Spinner spEstado) {
        int catPos = spCategoria.getSelectedItemPosition();
        categoriaId = catPos <= 0 ? null : categorias.get(catPos - 1).id;

        int estadoPos = spEstado.getSelectedItemPosition();
        estadoArticulo = estadoPos <= 0 ? null : ESTADOS_ARTICULO_FILTRO[estadoPos];

        precioMin = parsePrecioSafe(etPrecioMin.getText().toString());
        precioMax = parsePrecioSafe(etPrecioMax.getText().toString());
    }

    private Double parsePrecioSafe(String texto) {
        try {
            return parsePrecio(texto);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean aplicarFiltrosDesdeDialog(Spinner spCategoria, EditText etPrecioMin,
                                              EditText etPrecioMax, Spinner spEstado, EditText etZona) {
        if (!validarPreciosDesdeDialog(etPrecioMin, etPrecioMax)) {
            return false;
        }

        aplicarCategoriaEstadoPrecioDesdeDialog(spCategoria, etPrecioMin, etPrecioMax, spEstado);

        String zonaTexto = etZona.getText().toString().trim();
        zona = zonaTexto.isEmpty() ? null : zonaTexto;
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
        if (callGuardarBusqueda != null) {
            callGuardarBusqueda.cancel();
        }
        if (callFavoritos != null) {
            callFavoritos.cancel();
        }
        if (callMisPublicaciones != null) {
            callMisPublicaciones.cancel();
        }
        executor.shutdown();
    }
}
