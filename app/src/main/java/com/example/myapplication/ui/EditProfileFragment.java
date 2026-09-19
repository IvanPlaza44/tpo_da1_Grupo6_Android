package com.example.myapplication.ui;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.Usuario;
import com.example.myapplication.model.UsuarioUpdateRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.session.SessionManager;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * EditProfileFragment: formulario para editar nombre, teléfono, zona
 * y cambiar la foto de perfil.
 *
 * Trae los datos actuales con GET (para no mostrar campos vacíos) y
 * al guardar hace un PUT con solo los campos editables.
 */
public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private ImageView ivFotoPerfil;
    private Button btnCambiarFoto;
    private EditText etNombre;
    private EditText etTelefono;
    private EditText etZona;
    private Button btnGuardar;

    private ApiService apiService;
    private SessionManager sessionManager;
    private long usuarioId;

    // Uri de la foto elegida en la galería. La guardamos acá porque el
    // usuario puede cambiar la foto y recién después tocar "Guardar".
    private Uri fotoSeleccionadaUri;

    /**
     * ActivityResultLauncher es el reemplazo moderno de "onActivityResult()".
     * Acá registramos QUÉ hacer cuando vuelva el resultado de abrir la galería
     * (un Intent implícito, tal como se vio en la Clase 2 con ACTION_VIEW,
     * ACTION_SEND, etc. — en este caso el sistema abre la app de Galería
     * porque le pedimos ACTION_PICK sobre imágenes).
     */
    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    fotoSeleccionadaUri = result.getData().getData();
                    // Mostramos la foto elegida inmediatamente en el ImageView,
                    // aunque todavía no se haya subido al servidor.
                    ivFotoPerfil.setImageURI(fotoSeleccionadaUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                              @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ivFotoPerfil = view.findViewById(R.id.ivFotoPerfil);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);
        etNombre = view.findViewById(R.id.etNombre);
        etTelefono = view.findViewById(R.id.etTelefono);
        etZona = view.findViewById(R.id.etZona);
        btnGuardar = view.findViewById(R.id.btnGuardar);

        sessionManager = new SessionManager(requireContext());
        usuarioId = sessionManager.getUsuarioId();
        apiService = RetrofitClient.getApiService(requireContext());

        cargarDatosActuales();

        btnCambiarFoto.setOnClickListener(v -> abrirGaleria());
        btnGuardar.setOnClickListener(v -> guardarCambios(view));
    }

    private void abrirGaleria() {
        // Intent implícito: le pedimos al sistema "abrí algo que sepa
        // elegir una imagen", sin decir qué app específica. Android busca
        // una app que pueda manejar ACTION_PICK sobre EXTERNAL_CONTENT_URI.
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void cargarDatosActuales() {
        apiService.obtenerUsuario(usuarioId).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    etNombre.setText(usuario.getNombre());
                    etTelefono.setText(usuario.getTelefono());
                    etZona.setText(usuario.getZona());
                    // TODO: cargar usuario.getFotoUrl() en ivFotoPerfil con Glide/Picasso si lo usan
                } else {
                    Toast.makeText(getContext(), "No se pudieron cargar tus datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                Toast.makeText(getContext(), "Sin conexión", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarCambios(View view) {
        String nombre = etNombre.getText().toString().trim();
        String telefono = etTelefono.getText().toString().trim();
        String zona = etZona.getText().toString().trim();

        // Validación simple en el cliente antes de gastar un request de red.
        // El servidor igual debería validar (400 Bad Request si algo está mal).
        if (nombre.isEmpty()) {
            etNombre.setError("El nombre es obligatorio");
            return;
        }

        UsuarioUpdateRequest body = new UsuarioUpdateRequest(nombre, telefono, zona);

        apiService.actualizarUsuario(usuarioId, body).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();

                    // TODO: si fotoSeleccionadaUri != null, acá se debería subir
                    // la imagen (multipart/form-data) en un request aparte, ya que
                    // JSON no transporta archivos binarios directamente.

                    // Volvemos a la pantalla anterior (ProfileFragment) usando
                    // el NavController, respetando el back stack que vimos en
                    // Single Activity Architecture.
                    Navigation.findNavController(view).popBackStack();
                } else if (response.code() == 400) {
                    Toast.makeText(getContext(), "Revisá los datos ingresados", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401) {
                    Toast.makeText(getContext(), "Tu sesión expiró", Toast.LENGTH_SHORT).show();
                } else {
                    Log.e(TAG, "Error HTTP: " + response.code());
                    Toast.makeText(getContext(), "No se pudo guardar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                Toast.makeText(getContext(), "Sin conexión, intentá de nuevo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        ivFotoPerfil = null;
        btnCambiarFoto = null;
        etNombre = null;
        etTelefono = null;
        etZona = null;
        btnGuardar = null;
    }
}