package com.example.myapplication.ui;

import android.app.Activity;
import android.content.ContentResolver;
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
import com.example.myapplication.util.ImageLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * EditProfileFragment: formulario para editar nombre, teléfono, zona
 * y cambiar la foto de perfil.
 *
 * Trae los datos actuales con GET /me (para no mostrar campos vacíos).
 * Al guardar hace un PUT /me con los campos editables y, si se eligió una
 * foto nueva, la sube aparte con POST /me/foto (multipart/form-data), porque
 * JSON no transporta archivos binarios.
 */
@AndroidEntryPoint
public class EditProfileFragment extends Fragment {

    private static final String TAG = "EditProfileFragment";

    private ImageView ivFotoPerfil;
    private Button btnCambiarFoto;
    private EditText etNombre;
    private EditText etTelefono;
    private EditText etZona;
    private Button btnGuardar;

    @Inject ApiService apiService;

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
                    if (ivFotoPerfil != null && fotoSeleccionadaUri != null) {
                        ivFotoPerfil.setImageURI(fotoSeleccionadaUri);
                    }
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
        // GET /api/usuarios/me: perfil propio con teléfono, email y foto.
        // (GET /api/usuarios/{id} devuelve solo el perfil público.)
        apiService.obtenerMiPerfil().enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Usuario usuario = response.body();
                    etNombre.setText(usuario.getNombre());
                    etTelefono.setText(usuario.getTelefono());
                    etZona.setText(usuario.getZona());
                    // Mostramos la foto actual, salvo que el usuario ya haya elegido otra.
                    if (fotoSeleccionadaUri == null) {
                        ImageLoader.cargar(usuario.getFotoUrl(), ivFotoPerfil);
                    }
                } else {
                    Toast.makeText(getContext(), "No se pudieron cargar tus datos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (!isAdded()) return;
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

        // Evitamos que se toque "Guardar" dos veces mientras se envía.
        btnGuardar.setEnabled(false);

        // PUT /api/usuarios/me: el backend identifica al usuario por el token,
        // por eso no se manda el id.
        apiService.actualizarUsuario(body).enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    if (fotoSeleccionadaUri != null) {
                        // Datos guardados: ahora subimos la foto elegida.
                        subirFotoPerfil(view);
                    } else {
                        Toast.makeText(getContext(), "Perfil actualizado", Toast.LENGTH_SHORT).show();
                        volver(view);
                    }
                } else if (response.code() == 400) {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(getContext(), "Revisá los datos ingresados", Toast.LENGTH_SHORT).show();
                } else if (response.code() == 401) {
                    btnGuardar.setEnabled(true);
                    Toast.makeText(getContext(), "Tu sesión expiró", Toast.LENGTH_SHORT).show();
                } else {
                    btnGuardar.setEnabled(true);
                    Log.e(TAG, "Error HTTP: " + response.code());
                    Toast.makeText(getContext(), "No se pudo guardar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                Log.e(TAG, "Error de red: " + t.getMessage());
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Sin conexión, intentá de nuevo", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Sube la foto elegida con POST /api/usuarios/me/foto (multipart/form-data).
     * El backend la sube a Cloudinary y guarda la URL en el usuario.
     */
    private void subirFotoPerfil(View view) {
        MultipartBody.Part parte;
        try {
            parte = uriAParteMultipart(fotoSeleccionadaUri);
        } catch (IOException e) {
            btnGuardar.setEnabled(true);
            Toast.makeText(getContext(), "Se guardaron tus datos, pero no se pudo leer la foto elegida", Toast.LENGTH_LONG).show();
            return;
        }

        apiService.subirFotoPerfil(parte).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (!isAdded()) return;
                if (response.isSuccessful()) {
                    Toast.makeText(getContext(), "Perfil y foto actualizados", Toast.LENGTH_SHORT).show();
                    // Al volver, Mi perfil se recarga y muestra la foto nueva.
                    volver(view);
                } else {
                    Log.e(TAG, "Error HTTP al subir la foto: " + response.code());
                    btnGuardar.setEnabled(true);
                    Toast.makeText(getContext(),
                            "Se guardaron tus datos, pero no se pudo subir la foto (código " + response.code() + ")",
                            Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                Log.e(TAG, "Error de red al subir la foto: " + t.getMessage());
                if (!isAdded()) return;
                btnGuardar.setEnabled(true);
                Toast.makeText(getContext(), "Se guardaron tus datos, pero no hubo conexión para subir la foto", Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Convierte la imagen elegida de la galería (una Uri) en la "parte" multipart
     * que Retrofit envía. El nombre de la parte, "archivo", debe coincidir con el
     * @RequestParam("archivo") del backend.
     */
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
        String nombreArchivo = "perfil_" + System.currentTimeMillis() + ".jpg";
        return MultipartBody.Part.createFormData("archivo", nombreArchivo, body);
    }

    private void volver(View view) {
        // Volvemos a la pantalla anterior (ProfileFragment) usando
        // el NavController, respetando el back stack que vimos en
        // Single Activity Architecture.
        Navigation.findNavController(view).popBackStack();
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