package com.example.myapplication.ui.busquedas;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavOptions;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.BusquedaGuardadaResponseDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.session.ExplorarCriteriosPendiente;
import com.example.myapplication.ui.explorar.ExplorarFragment;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class BusquedasGuardadasFragment extends Fragment {

    @Inject ApiService apiService;
    private BusquedasGuardadasAdapter adapter;
    private Call<List<BusquedaGuardadaResponseDto>> llamadaEnCurso;
    private Call<Void> llamadaEliminar;
    private boolean eliminando = false;

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

        rvBusquedasGuardadas = view.findViewById(R.id.rvBusquedasGuardadas);
        progressBar = view.findViewById(R.id.progressBar);
        tvVacio = view.findViewById(R.id.tvVacio);
        contenedorError = view.findViewById(R.id.contenedorError);

        rvBusquedasGuardadas.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BusquedasGuardadasAdapter(new ArrayList<>(), new BusquedasGuardadasAdapter.OnItemClickListener() {
            @Override
            public void onClick(BusquedaGuardadaResponseDto busqueda) {
                aplicarBusquedaGuardada(busqueda);
            }

            @Override
            public void onEliminar(BusquedaGuardadaResponseDto busqueda) {
                confirmarEliminar(busqueda);
            }
        });
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
        if (llamadaEliminar != null) {
            llamadaEliminar.cancel();
            llamadaEliminar = null;
        }
        super.onDestroyView();
    }

    private void aplicarBusquedaGuardada(BusquedaGuardadaResponseDto busqueda) {
        if (!busqueda.tieneCriteriosAplicables()) {
            Toast.makeText(requireContext(),
                    "Esta búsqueda no tiene criterios para aplicar",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        apiService.marcarBusquedaRevisada(busqueda.id).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                // Si falla, igual reaplicamos la búsqueda en Explorar.
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                // Igual: el PATCH no debe bloquear la navegación.
            }
        });

        ExplorarCriteriosPendiente.establecerDesde(busqueda);
        NavOptions options = new NavOptions.Builder()
                .setPopUpTo(R.id.explorarFragment, true)
                .build();
        Navigation.findNavController(requireView()).navigate(
                R.id.action_busquedasGuardadasFragment_to_explorarFragment,
                ExplorarFragment.crearArgumentosBusquedaGuardada(busqueda),
                options);
    }

    private void confirmarEliminar(BusquedaGuardadaResponseDto busqueda) {
        if (eliminando) return;

        new AlertDialog.Builder(requireContext())
                .setMessage("¿Eliminar esta búsqueda guardada?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarBusqueda(busqueda))
                .show();
    }

    private void eliminarBusqueda(BusquedaGuardadaResponseDto busqueda) {
        if (eliminando) return;

        eliminando = true;
        if (llamadaEliminar != null) {
            llamadaEliminar.cancel();
        }

        llamadaEliminar = apiService.eliminarBusquedaGuardada(busqueda.id);
        llamadaEliminar.enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded() || call.isCanceled()) return;
                eliminando = false;
                llamadaEliminar = null;
                if (response.isSuccessful()) {
                    adapter.eliminarPorId(busqueda.id);
                    if (adapter.getItemCount() == 0) {
                        mostrarVacio();
                    }
                } else {
                    Toast.makeText(requireContext(), "No se pudo eliminar la búsqueda", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded() || call.isCanceled()) return;
                eliminando = false;
                llamadaEliminar = null;
                Toast.makeText(requireContext(), "No se pudo eliminar la búsqueda", Toast.LENGTH_SHORT).show();
            }
        });
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
