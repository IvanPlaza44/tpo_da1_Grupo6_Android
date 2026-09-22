package com.example.myapplication.ui.product;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;
import com.example.myapplication.network.ApiService;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class PublicarFragment extends Fragment {

    @Inject ApiService apiService;

    private int pasoActual = 1;
    private long publicacionId = -1;

    private final List<CategoriaDto> categorias = new ArrayList<>();
    private final String[] estadosArticulo = {"NUEVO", "COMO_NUEVO", "USADO"};

    private TextView tvPaso, tvResumen, tvFotosEstado;
    private LinearProgressIndicator progressPasos;
    private View stepUno, stepDos, stepTres;
    private EditText etTitulo, etDescripcion, etPrecio, etZona;
    private Spinner spCategoria, spEstadoArticulo;
    private Button btnAnterior, btnSiguiente, btnAgregarFotos;
    private LinearLayout llFotos;

    private int fotosSubidas = 0;

    private final ActivityResultLauncher<String> seleccionarFotosLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                if (uris == null || uris.isEmpty()) return;
                for (Uri uri : uris) subirFotoIndividual(uri);
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPaso = view.findViewById(R.id.tvPaso);
        progressPasos = view.findViewById(R.id.progressPasos);
        tvResumen = view.findViewById(R.id.tvResumen);
        tvFotosEstado = view.findViewById(R.id.tvFotosEstado);
        stepUno = view.findViewById(R.id.stepUno);
        stepDos = view.findViewById(R.id.stepDos);
        stepTres = view.findViewById(R.id.stepTres);
        etTitulo = view.findViewById(R.id.etTitulo);
        etDescripcion = view.findViewById(R.id.etDescripcion);
        etPrecio = view.findViewById(R.id.etPrecio);
        etZona = view.findViewById(R.id.etZona);
        spCategoria = view.findViewById(R.id.spCategoria);
        spEstadoArticulo = view.findViewById(R.id.spEstadoArticulo);
        btnAnterior = view.findViewById(R.id.btnAnterior);
        btnSiguiente = view.findViewById(R.id.btnSiguiente);
        btnAgregarFotos = view.findViewById(R.id.btnAgregarFotos);
        llFotos = view.findViewById(R.id.llFotos);

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, estadosArticulo);
        spEstadoArticulo.setAdapter(estadoAdapter);

        btnAnterior.setOnClickListener(v -> irAPaso(pasoActual - 1));
        btnSiguiente.setOnClickListener(v -> guardarPasoActualYAvanzar());

        btnAgregarFotos.setOnClickListener(v -> {
            if (publicacionId == -1) {
                Toast.makeText(requireContext(), "Todavia se esta cargando el borrador, esperá un segundo", Toast.LENGTH_SHORT).show();
                return;
            }
            seleccionarFotosLauncher.launch("image/*");
        });

        cargarCategorias();
        cargarBorrador();
    }

    private void cargarCategorias() {
        apiService.obtenerCategorias().enqueue(new Callback<List<CategoriaDto>>() {
            @Override
            public void onResponse(Call<List<CategoriaDto>> call, Response<List<CategoriaDto>> response) {
                if (!response.isSuccessful() || response.body() == null) return;
                categorias.clear();
                categorias.addAll(response.body());
                List<String> nombres = new ArrayList<>();
                for (CategoriaDto c : categorias) nombres.add(c.nombre);
                ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(),
                        android.R.layout.simple_spinner_dropdown_item, nombres);
                spCategoria.setAdapter(adapter);
            }

            @Override
            public void onFailure(Call<List<CategoriaDto>> call, Throwable t) {
                Toast.makeText(requireContext(), "No se pudieron cargar las categorias", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Trae el borrador existente (o lo crea) y precarga los campos si ya habia algo cargado
    private void cargarBorrador() {
        apiService.obtenerBorrador().enqueue(new Callback<PublicacionDetalle>() {
            @Override
            public void onResponse(Call<PublicacionDetalle> call, Response<PublicacionDetalle> response) {
                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Error al obtener el borrador: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                PublicacionDetalle d = response.body();
                publicacionId = d.id;
                if (d.titulo != null) etTitulo.setText(d.titulo);
                if (d.descripcion != null) etDescripcion.setText(d.descripcion);
                if (d.precio != null) etPrecio.setText(String.valueOf(d.precio));
                if (d.zonaEntrega != null) etZona.setText(d.zonaEntrega);
                if (d.fotos != null) {
                    fotosSubidas = d.fotos.size();
                    actualizarEstadoFotos();
                }
            }

            @Override
            public void onFailure(Call<PublicacionDetalle> call, Throwable t) {
                Toast.makeText(requireContext(), "Sin conexion con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void guardarPasoActualYAvanzar() {
        if (publicacionId == -1) {
            Toast.makeText(requireContext(), "Todavia se esta cargando el borrador, esperá un segundo", Toast.LENGTH_SHORT).show();
            return;
        }

        PublicacionRequest req = new PublicacionRequest();

        if (pasoActual == 1) {
            req.titulo = etTitulo.getText().toString().trim();
            req.descripcion = etDescripcion.getText().toString().trim();
            if (!categorias.isEmpty()) {
                req.categoriaId = categorias.get(spCategoria.getSelectedItemPosition()).id;
            }
        } else if (pasoActual == 2) {
            String precioTexto = etPrecio.getText().toString().trim();
            req.precio = precioTexto.isEmpty() ? null : Double.parseDouble(precioTexto);
            req.estadoArticulo = estadosArticulo[spEstadoArticulo.getSelectedItemPosition()];
            req.zonaEntrega = etZona.getText().toString().trim();
        }
        // Importante: nunca seteamos req.fotosUrls aca - eso lo maneja el endpoint de fotos aparte,
        // si lo mandamos (aunque sea vacio) el backend borra las fotos ya subidas.

        apiService.guardarPaso(publicacionId, req).enqueue(new Callback<PublicacionDetalle>() {
            @Override
            public void onResponse(Call<PublicacionDetalle> call, Response<PublicacionDetalle> response) {
                if (!response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Error al guardar: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (pasoActual == 3) {
                    publicar();
                } else {
                    irAPaso(pasoActual + 1);
                }
            }

            @Override
            public void onFailure(Call<PublicacionDetalle> call, Throwable t) {
                Toast.makeText(requireContext(), "Sin conexion con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void publicar() {
        apiService.publicar(publicacionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Publicacion creada con exito", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else {
                    Toast.makeText(requireContext(), "Error al publicar: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Toast.makeText(requireContext(), "Sin conexion con el servidor", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void subirFotoIndividual(Uri uri) {
        MultipartBody.Part parte;
        try {
            parte = uriAParteMultipart(uri);
        } catch (IOException e) {
            if (isAdded()) Toast.makeText(requireContext(), "No se pudo leer la imagen elegida", Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.subirFoto(publicacionId, parte).enqueue(new Callback<PublicacionDetalle>() {
            @Override
            public void onResponse(Call<PublicacionDetalle> call, Response<PublicacionDetalle> response) {
                if (!isAdded()) return; // el fragment ya no esta en pantalla, no tocar la UI

                if (!response.isSuccessful() || response.body() == null) {
                    Toast.makeText(requireContext(), "Error al subir la foto: " + response.code(), Toast.LENGTH_LONG).show();
                    return;
                }
                agregarMiniatura(uri);
                fotosSubidas = response.body().fotos != null ? response.body().fotos.size() : fotosSubidas + 1;
                actualizarEstadoFotos();
            }

            @Override
            public void onFailure(Call<PublicacionDetalle> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Sin conexion al subir la foto", Toast.LENGTH_LONG).show();
            }
        });
    }

    private MultipartBody.Part uriAParteMultipart(Uri uri) throws IOException {
        ContentResolver resolver = requireContext().getContentResolver();
        String mimeType = resolver.getType(uri);
        if (mimeType == null) mimeType = "image/jpeg";

        InputStream inputStream = resolver.openInputStream(uri);
        if (inputStream == null) throw new IOException("No se pudo abrir la imagen");

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        int leidos;
        while ((leidos = inputStream.read(chunk)) != -1) {
            buffer.write(chunk, 0, leidos);
        }
        inputStream.close();

        RequestBody body = RequestBody.create(buffer.toByteArray(), MediaType.parse(mimeType));
        String nombreArchivo = "foto_" + System.currentTimeMillis() + ".jpg";
        return MultipartBody.Part.createFormData("archivo", nombreArchivo, body);
    }

    private void agregarMiniatura(Uri uri) {
        ImageView iv = new ImageView(requireContext());
        int tamanioPx = (int) (80 * getResources().getDisplayMetrics().density);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(tamanioPx, tamanioPx);
        lp.setMarginEnd((int) (8 * getResources().getDisplayMetrics().density));
        iv.setLayoutParams(lp);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv.setImageURI(uri);
        llFotos.addView(iv);
    }

    private void actualizarEstadoFotos() {
        tvFotosEstado.setText(fotosSubidas + " foto(s) subida(s)");
    }

    private void irAPaso(int paso) {
        pasoActual = paso;
        stepUno.setVisibility(paso == 1 ? View.VISIBLE : View.GONE);
        stepDos.setVisibility(paso == 2 ? View.VISIBLE : View.GONE);
        stepTres.setVisibility(paso == 3 ? View.VISIBLE : View.GONE);
        tvPaso.setText("Paso " + paso + " de 3");
        if (progressPasos != null) {
            progressPasos.setProgressCompat(paso, true);
        }
        btnAnterior.setEnabled(paso > 1);
        btnSiguiente.setText(paso == 3 ? "Publicar" : "Siguiente");

        if (paso == 3) {
            String resumen = "Titulo: " + etTitulo.getText().toString()
                    + "\nDescripcion: " + etDescripcion.getText().toString()
                    + "\nPrecio: " + etPrecio.getText().toString()
                    + "\nZona: " + etZona.getText().toString();
            tvResumen.setText(resumen);
        }
    }
}