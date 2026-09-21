package com.example.myapplication.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.PerfilPublicoResponseDto;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.ui.explorar.ExplorarAdapter;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PublicProfileFragment: perfil público de OTRO usuario (no el propio).
 *
 * El id del usuario a mostrar NO sale de la sesión, sino que llega como
 * argumento de navegación ("userId") cuando se toca "Ver perfil del vendedor"
 * en el detalle de una publicación.
 *
 * Una sola llamada (GET /api/usuarios/{id}) trae todo lo que pide el TP:
 * reputación, antigüedad y publicaciones activas.
 */
public class PublicProfileFragment extends Fragment {

    private static final String TAG = "PublicProfileFragment";

    // Debe coincidir con el <argument android:name="userId"/> del nav_graph.xml
    public static final String ARG_USER_ID = "userId";

    private TextView tvNombre;
    private TextView tvZona;
    private TextView tvFechaAlta;
    private TextView tvEstrellas;
    private TextView tvOperaciones;
    private TextView tvSinPublicaciones;
    private ImageView ivFotoPerfil;
    private RecyclerView rvPublicaciones;
    private ExplorarAdapter adapter;

    private ApiService apiService;
    private long userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_public_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvZona = view.findViewById(R.id.tvZona);
        tvFechaAlta = view.findViewById(R.id.tvFechaAlta);
        tvEstrellas = view.findViewById(R.id.tvEstrellas);
        tvOperaciones = view.findViewById(R.id.tvOperaciones);
        tvSinPublicaciones = view.findViewById(R.id.tvSinPublicaciones);
        rvPublicaciones = view.findViewById(R.id.rvPublicaciones);

        // Al tocar una publicación activa del vendedor, abrimos su detalle
        // (igual que se hace desde Explorar).
        rvPublicaciones.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ExplorarAdapter(new ArrayList<>(), publicacion -> {
            Bundle args = new Bundle();
            args.putLong("publicacionId", publicacion.id);
            Navigation.findNavController(requireView())
                    .navigate(R.id.publicacionDetalleFragment, args);
        });
        rvPublicaciones.setAdapter(adapter);

        // getArguments() devuelve el Bundle que se mandó al navegar hacia acá.
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID, -1);
        }

        apiService = RetrofitClient.getApiService(requireContext());

        if (userId != -1) {
            cargarPerfilPublico();
        } else {
            Toast.makeText(getContext(), "No se pudo identificar al usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarPerfilPublico() {
        apiService.obtenerPerfilPublico(userId).enqueue(new Callback<PerfilPublicoResponseDto>() {
            @Override
            public void onResponse(@NonNull Call<PerfilPublicoResponseDto> call,
                                   @NonNull Response<PerfilPublicoResponseDto> response) {
                if (!isAdded() || tvNombre == null) return; // la pantalla ya se cerró

                if (response.isSuccessful() && response.body() != null) {
                    mostrarPerfil(response.body());
                } else if (response.code() == 404) {
                    Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error HTTP: " + response.code());
                    Toast.makeText(getContext(), "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(@NonNull Call<PerfilPublicoResponseDto> call, @NonNull Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (!isAdded() || tvNombre == null) return;
                Toast.makeText(getContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarPerfil(PerfilPublicoResponseDto perfil) {
        // Nombre y zona: los usuarios nuevos pueden no haberlos completado todavía.
        String nombre = perfil.nombre;
        tvNombre.setText(nombre != null && !nombre.isEmpty() ? nombre : "Usuario sin nombre");

        String zona = perfil.zona;
        tvZona.setText(zona != null && !zona.isEmpty() ? zona : "Zona no indicada");

        // La fecha llega como "2026-09-15T20:09:13.185121": nos quedamos con el día.
        String fecha = perfil.fechaAlta;
        if (fecha != null && fecha.length() >= 10) {
            fecha = fecha.substring(0, 10);
        }
        tvFechaAlta.setText("En Ronda desde " + fecha);

        // Reputación (viene en la misma respuesta).
        if (perfil.promedioEstrellas > 0) {
            tvEstrellas.setText(String.format("⭐ %.1f / 5", perfil.promedioEstrellas));
            tvOperaciones.setText(String.format("%d calificaciones", perfil.totalCalificaciones));
        } else {
            tvEstrellas.setText("Sin calificaciones todavía");
            tvOperaciones.setText("");
        }

        // Publicaciones activas del vendedor.
        List<PublicacionResumen> activas = perfil.publicacionesActivas;
        if (activas == null || activas.isEmpty()) {
            tvSinPublicaciones.setVisibility(View.VISIBLE);
            rvPublicaciones.setVisibility(View.GONE);
        } else {
            tvSinPublicaciones.setVisibility(View.GONE);
            rvPublicaciones.setVisibility(View.VISIBLE);
            adapter.actualizarLista(activas);
        }
        // TODO: cargar la foto de perfil cuando el backend la incluya en el DTO.
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivFotoPerfil = null;
        tvNombre = null;
        tvZona = null;
        tvFechaAlta = null;
        tvEstrellas = null;
        tvOperaciones = null;
        tvSinPublicaciones = null;
        rvPublicaciones = null;
        adapter = null;
    }
}