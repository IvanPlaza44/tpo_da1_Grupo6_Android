package com.example.myapplication.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.Reputacion;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ProfileFragment: pantalla de "Mi perfil".
 *
 * Es un Fragment (no una Activity) porque en la Single Activity Architecture
 * vista en clase, toda la app vive dentro de una única MainActivity con un
 * NavHostFragment, y cada pantalla es un Fragment que el NavController
 * intercambia dentro de ese contenedor.
 *
 * Ciclo de vida que usamos acá (de la Clase 4 - Fragments):
 * 1) onCreateView()  -> solo inflamos el layout, todavía no hay vistas listas.
 * 2) onViewCreated()  -> acá SÍ existen las vistas: hacemos findViewById(),
 *    seteamos listeners, y disparamos las llamadas a la API.
 * 3) onDestroyView()  -> liberamos referencias a vistas para evitar memory leaks.
 */
public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";

    private ImageView ivFotoPerfil;
    private TextView tvNombre;
    private TextView tvEmail;
    private TextView tvTelefono;
    private TextView tvZona;
    private TextView tvEstrellas;
    private TextView tvOperaciones;
    private Button btnEditar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private long usuarioId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        // Solo inflamos (convertimos el XML en objetos View en memoria).
        // No tocar vistas todavía: recién existen "en papel", no las agarramos acá.
        return inflater.inflate(R.layout.fragment_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Acá el layout YA existe de verdad: es el lugar correcto para
        // los findViewById() y toda la lógica de UI (visto en Componentes de un Fragment).
        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvTelefono = view.findViewById(R.id.tvTelefono);
        tvZona = view.findViewById(R.id.tvZona);
        tvEstrellas = view.findViewById(R.id.tvEstrellas);
        tvOperaciones = view.findViewById(R.id.tvOperaciones);
        btnEditar = view.findViewById(R.id.btnEditar);

        // SessionManager guarda el token y el id del usuario logueado
        // (se completó en el login, con guardarSesion()).
        sessionManager = new SessionManager(requireContext());
        usuarioId = sessionManager.getUsuarioId();

        // RetrofitClient arma el cliente HTTP UNA sola vez (patrón singleton)
        // con el interceptor que agrega "Authorization: Bearer <token>"
        // automáticamente en cada request (visto en la clase de JWT).
        apiService = RetrofitClient.getApiService(requireContext());

        cargarPerfil();
        cargarReputacion();

        // Navegamos a la pantalla de edición usando el NavController,
        // tal como se vio en "Navegar entre Fragments" de Navigation Component.
        btnEditar.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_profile_to_editProfile)
        );
    }

    private void cargarPerfil() {
        // enqueue() ejecuta la llamada en un hilo separado, de forma asíncrona.
        // NUNCA usamos execute(): eso bloquearía el Main Thread y Android
        // tiraría NetworkOnMainThreadException (Consideración 1 de la clase de Retrofit).
        apiService.obtenerUsuario(usuarioId).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                // onResponse() se llama SIEMPRE que el servidor contestó algo,
                // pero eso no significa que salió bien: puede ser un 401 o 404.
                // Por eso SIEMPRE validamos isSuccessful() antes de usar el body
                // (Consideración 2: Manejo de errores HTTP).
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    tvNombre.setText(usuario.getNombre());
                    tvEmail.setText(usuario.getEmail());
                    tvTelefono.setText(usuario.getTelefono());
                    tvZona.setText(usuario.getZona());
                    // TODO: si usan Glide/Picasso, acá cargarían usuario.getFotoUrl() en ivFotoPerfil
                } else if (response.code() == 401) {
                    // 401 Unauthorized: el token venció o es inválido.
                    Toast.makeText(getContext(), "Tu sesión expiró, volvé a iniciar sesión", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 404) {
                    // 404 Not Found: el recurso (usuario) no existe.
                    Toast.makeText(getContext(), "Usuario no encontrado", Toast.LENGTH_SHORT).show();
                } else {
                    // Otro código de error del servidor (4xx o 5xx)
                    Log.e(TAG, "Error HTTP: " + response.code());
                    Toast.makeText(getContext(), "No se pudo cargar el perfil", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                // onFailure() se dispara cuando NO hubo respuesta del servidor:
                // sin internet, timeout, DNS caído, etc. (error de red, no de HTTP).
                Log.e(TAG, "Error de red: " + t.getMessage());
                Toast.makeText(getContext(), "Sin conexión, intentá de nuevo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void cargarReputacion() {
        apiService.obtenerReputacion(usuarioId).enqueue(new Callback<Reputacion>() {
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
                    // Si el usuario todavía no tiene calificaciones, mostramos un mensaje neutro
                    // en vez de un error (no es realmente una falla).
                    tvEstrellas.setText("Sin calificaciones todavía");
                }
            }

            @Override
            public void onFailure(Call<Reputacion> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Limpiamos las referencias a las vistas: cuando el Fragment se destruye
        // (por ejemplo al navegar a otra pantalla) el layout se libera de memoria,
        // pero el objeto Fragment puede seguir vivo un rato más. Si dejáramos las
        // referencias, apuntarían a vistas que ya no existen -> memory leak.
        ivFotoPerfil = null;
        tvNombre = null;
        tvEmail = null;
        tvTelefono = null;
        tvZona = null;
        tvEstrellas = null;
        tvOperaciones = null;
        btnEditar = null;
    }
}