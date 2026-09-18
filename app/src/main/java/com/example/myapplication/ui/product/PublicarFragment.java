package com.example.myapplication.ui.product;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.CategoriaDto;
import com.example.myapplication.model.Publicacion.PublicacionDetalle;
import com.example.myapplication.model.Publicacion.PublicacionRequest;
import com.example.myapplication.network.RetrofitClient;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PublicarFragment extends Fragment {

    private int pasoActual = 1;
    private long publicacionId = -1;

    private final List<CategoriaDto> categorias = new ArrayList<>();
    private final String[] estadosArticulo = {"NUEVO", "COMO_NUEVO", "USADO"};

    private TextView tvPaso, tvResumen;
    private View stepUno, stepDos, stepTres;
    private EditText etTitulo, etDescripcion, etPrecio, etZona;
    private Spinner spCategoria, spEstadoArticulo;
    private Button btnAnterior, btnSiguiente;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_publicar, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvPaso = view.findViewById(R.id.tvPaso);
        tvResumen = view.findViewById(R.id.tvResumen);
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

        ArrayAdapter<String> estadoAdapter = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, estadosArticulo);
        spEstadoArticulo.setAdapter(estadoAdapter);

        btnAnterior.setOnClickListener(v -> irAPaso(pasoActual - 1));
        btnSiguiente.setOnClickListener(v -> guardarPasoActualYAvanzar());

        cargarCategorias();
        cargarBorrador();
    }

    private void cargarCategorias() {
        RetrofitClient.getApiService(requireContext()).obtenerCategorias().enqueue(new Callback<List<CategoriaDto>>() {
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
        RetrofitClient.getApiService(requireContext()).obtenerBorrador().enqueue(new Callback<PublicacionDetalle>() {
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

        RetrofitClient.getApiService(requireContext()).guardarPaso(publicacionId, req).enqueue(new Callback<PublicacionDetalle>() {
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
        RetrofitClient.getApiService(requireContext()).publicar(publicacionId).enqueue(new Callback<Void>() {
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

    private void irAPaso(int paso) {
        pasoActual = paso;
        stepUno.setVisibility(paso == 1 ? View.VISIBLE : View.GONE);
        stepDos.setVisibility(paso == 2 ? View.VISIBLE : View.GONE);
        stepTres.setVisibility(paso == 3 ? View.VISIBLE : View.GONE);
        tvPaso.setText("Paso " + paso + " de 3");
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