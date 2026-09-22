package com.example.myapplication.ui.historial;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.CalificacionOperacionRequestDto;
import com.example.myapplication.model.Publicacion.OperacionResponseDto;
import com.example.myapplication.network.ApiService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class HistorialFragment extends Fragment {

    @Inject ApiService apiService;
    private List<OperacionResponseDto> todasLasOperaciones = new ArrayList<>();

    private RadioGroup rgTipo;
    private Button btnFechaDesde, btnFechaHasta, btnLimpiarFiltros;
    private ProgressBar progressBar;
    private TextView tvVacio;
    private RecyclerView rvOperaciones;

    private String fechaDesde = null; // "yyyy-MM-dd"
    private String fechaHasta = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_historial, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rgTipo = view.findViewById(R.id.rgTipo);
        btnFechaDesde = view.findViewById(R.id.btnFechaDesde);
        btnFechaHasta = view.findViewById(R.id.btnFechaHasta);
        btnLimpiarFiltros = view.findViewById(R.id.btnLimpiarFiltros);
        progressBar = view.findViewById(R.id.progressBar);
        tvVacio = view.findViewById(R.id.tvVacio);
        rvOperaciones = view.findViewById(R.id.rvOperaciones);

        rvOperaciones.setLayoutManager(new LinearLayoutManager(requireContext()));

        rgTipo.setOnCheckedChangeListener((group, checkedId) -> aplicarFiltros());
        btnFechaDesde.setOnClickListener(v -> elegirFecha(true));
        btnFechaHasta.setOnClickListener(v -> elegirFecha(false));
        btnLimpiarFiltros.setOnClickListener(v -> {
            fechaDesde = null;
            fechaHasta = null;
            btnFechaDesde.setText("Desde");
            btnFechaHasta.setText("Hasta");
            rgTipo.check(R.id.rbTodas);
            aplicarFiltros();
        });

        cargarOperaciones();
    }

    private void elegirFecha(boolean esDesde) {
        Calendar cal = Calendar.getInstance();
        new DatePickerDialog(requireContext(), (view12, year, month, dayOfMonth) -> {
            String fecha = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            if (esDesde) {
                fechaDesde = fecha;
                btnFechaDesde.setText("Desde: " + fecha);
            } else {
                fechaHasta = fecha;
                btnFechaHasta.setText("Hasta: " + fecha);
            }
            aplicarFiltros();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void cargarOperaciones() {
        progressBar.setVisibility(View.VISIBLE);
        apiService.misOperaciones().enqueue(new Callback<List<OperacionResponseDto>>() {
            @Override
            public void onResponse(@NonNull Call<List<OperacionResponseDto>> call,
                                   @NonNull Response<List<OperacionResponseDto>> response) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    todasLasOperaciones = response.body();
                    aplicarFiltros();
                } else {
                    Toast.makeText(requireContext(), "No se pudo cargar el historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<List<OperacionResponseDto>> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                progressBar.setVisibility(View.GONE);
                Toast.makeText(requireContext(), "Sin conexión: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void aplicarFiltros() {
        List<OperacionResponseDto> filtradas = new ArrayList<>();

        String tipoSeleccionado = null;
        int checkedId = rgTipo.getCheckedRadioButtonId();
        if (checkedId == R.id.rbCompras) tipoSeleccionado = "COMPRA";
        else if (checkedId == R.id.rbVentas) tipoSeleccionado = "VENTA";

        for (OperacionResponseDto op : todasLasOperaciones) {
            if (tipoSeleccionado != null && !tipoSeleccionado.equals(op.tipo)) continue;

            String fechaOp = op.fechaEntrega != null ? op.fechaEntrega : op.fechaAcordada;
            String fechaOpCorta = fechaOp != null && fechaOp.length() >= 10 ? fechaOp.substring(0, 10) : null;

            if (fechaDesde != null && (fechaOpCorta == null || fechaOpCorta.compareTo(fechaDesde) < 0)) continue;
            if (fechaHasta != null && (fechaOpCorta == null || fechaOpCorta.compareTo(fechaHasta) > 0)) continue;

            filtradas.add(op);
        }

        tvVacio.setVisibility(filtradas.isEmpty() ? View.VISIBLE : View.GONE);
        rvOperaciones.setVisibility(filtradas.isEmpty() ? View.GONE : View.VISIBLE);
        rvOperaciones.setAdapter(new OperacionAdapter(filtradas, this::mostrarDialogoCalificar));
    }

    private void mostrarDialogoCalificar(OperacionResponseDto operacion) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_calificar, null);
        RatingBar ratingBar = dialogView.findViewById(R.id.ratingBar);
        EditText etComentario = dialogView.findViewById(R.id.etComentario);

        new AlertDialog.Builder(requireContext())
                .setTitle("Calificar operación")
                .setView(dialogView)
                .setPositiveButton("Enviar", (dialog, which) -> {
                    int estrellas = (int) ratingBar.getRating();
                    String comentario = etComentario.getText().toString().trim();
                    enviarCalificacion(operacion.id, estrellas, comentario);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void enviarCalificacion(long operacionId, int estrellas, String comentario) {
        CalificacionOperacionRequestDto body = new CalificacionOperacionRequestDto(estrellas, comentario);
        apiService.calificarOperacion(operacionId, body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "¡Gracias por calificar!", Toast.LENGTH_SHORT).show();
                    cargarOperaciones();
                } else {
                    Toast.makeText(requireContext(), "No se pudo enviar la calificación", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(requireContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }
}