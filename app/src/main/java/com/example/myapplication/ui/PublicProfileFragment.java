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

import com.example.myapplication.R;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PublicProfileFragment: perfil público de OTRO usuario (no el propio).
 *
 * A diferencia de ProfileFragment, acá el id del usuario a mostrar NO sale
 * de la sesión (SessionManager), sino que llega como argumento de navegación
 * a través del Bundle (visto en "argument" del nav_graph.xml y en cómo
 * DetailFragment recibe "itemId" en la Clase 4).
 *
 * Se usa, por ejemplo, cuando alguien toca el nombre del vendedor
 * en el detalle de una publicación, para ver "su reputación, su antigüedad
 * en la plataforma y sus publicaciones activas" (según el enunciado del TPO).
 */
public class PublicProfileFragment extends Fragment {

    private static final String TAG = "PublicProfileFragment";

    // Clave del argumento: debe coincidir con la que se define en el
    // <argument android:name="userId" .../> del nav_graph.xml
    public static final String ARG_USER_ID = "userId";

    private TextView tvNombre;
    private TextView tvZona;
    private TextView tvFechaAlta;
    private TextView tvEstrellas;
    private TextView tvOperaciones;
    private ImageView ivFotoPerfil;

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

        // getArguments() devuelve el Bundle que se mandó al navegar hacia acá.
        // OJO: no usamos la sesión del usuario logueado, sino el id que
        // nos pasaron (el del OTRO usuario que queremos consultar).
        if (getArguments() != null) {
            userId = getArguments().getLong(ARG_USER_ID, -1);
        }

        apiService = RetrofitClient.getApiService(requireContext());

        if (userId != -1) {
            cargarPerfilPublico();
            // TODO: no hay endpoint en el backend para reputación separada por rol
            // (comprador/vendedor) de OTRO usuario — solo existe para el propio
            // perfil (/api/usuarios/me). Avisar al equipo.
            // cargarReputacion();
        } else {
            Toast.makeText(getContext(), "No se pudo identificar al usuario", Toast.LENGTH_SHORT).show();
        }
    }

    private void cargarPerfilPublico() {
        apiService.obtenerUsuario(userId).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    tvNombre.setText(usuario.getNombre());
                    tvZona.setText(usuario.getZona());
                    tvFechaAlta.setText("En Ronda desde " + usuario.getFechaAlta());
                    // TODO: cargar usuario.getFotoUrl() con Glide/Picasso
                } else if (response.code() == 404) {
                    Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error HTTP: " + response.code());
                    Toast.makeText(getContext(), "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                Toast.makeText(getContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /*
    private void cargarReputacion() {
        apiService.obtenerReputacion(userId).enqueue(new Callback<Reputacion>() {
            @Override
            public void onResponse(Call<Reputacion> call, Response<Reputacion> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Reputacion rep = response.body();
                    tvEstrellas.setText(String.format("⭐ %.1f / 5", rep.getPromedioEstrellas()));
                    tvOperaciones.setText(String.format(
                            "%d operaciones (%d como comprador, %d como vendedor)",
                            rep.getTotalOperaciones(),
                            rep.getCantidadComoComprador(),
                            rep.getCantidadComoVendedor()));
                } else {
                    tvEstrellas.setText("Sin calificaciones todavía");
                }
            }

            @Override
            public void onFailure(Call<Reputacion> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
            }
        });
    }
    */

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivFotoPerfil = null;
        tvNombre = null;
        tvZona = null;
        tvFechaAlta = null;
        tvEstrellas = null;
        tvOperaciones = null;
    }
}