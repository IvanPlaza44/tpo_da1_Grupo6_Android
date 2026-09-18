package com.example.myapplication.ui.auth;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.biometric.BiometricAuthManager;
import com.example.myapplication.model.ApiError;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.example.myapplication.session.SessionManager;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginFragment extends Fragment {

    private EditText etUsuario, etPassword;
    private Button btnLogin;
    private View btnBiometric;
    // Agregamos la variable del nuevo botón de registro acá:
    private TextView tvIngresarConCodigo, tvError, tvIrARegistro;
    private ProgressBar progressBar;
    private BiometricAuthManager biometricManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUsuario = view.findViewById(R.id.etUsuario);
        etPassword = view.findViewById(R.id.etPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        tvIngresarConCodigo = view.findViewById(R.id.tvIngresarConCodigo);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.progressBar);
        btnBiometric = view.findViewById(R.id.btnBiometric);

        // Enlazamos el botón del XML con esta clase:
        tvIrARegistro = view.findViewById(R.id.tvIrARegistro);

        biometricManager = new BiometricAuthManager(requireActivity(), this::irAHome);
        SessionManager sessionManager = new SessionManager(requireContext());

        if (btnBiometric != null) {
            if (biometricManager.canAuthenticate() && sessionManager.isBiometriaActivada()) {
                btnBiometric.setVisibility(View.VISIBLE);
                etUsuario.setText(sessionManager.getEmail());
                btnBiometric.setOnClickListener(v -> biometricManager.showBiometricPrompt());
            } else {
                btnBiometric.setVisibility(View.GONE);
            }
        }

        btnLogin.setOnClickListener(v -> intentarLogin());

        tvIngresarConCodigo.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_requestOtpFragment));

        // Le decimos que viaje a la pantalla de registro al tocar el botón nuevo:
        tvIrARegistro.setOnClickListener(v ->
                Navigation.findNavController(view).navigate(R.id.action_loginFragment_to_registerFragment));
    }

    private void intentarLogin() {
        String usuario = etUsuario.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        ocultarError();

        if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(password)) {
            mostrarError("Completá usuario y contraseña");
            return;
        }

        setLoading(true);

        ApiService api = RetrofitClient.getApiService(requireContext());
        api.login(new LoginRequest(usuario, password)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    new SessionManager(requireContext())
                            .guardarSesion(body.getToken(), body.getUsuarioId(), body.getEmail(), body.getUsername());
                    irAHome();
                } else if (response.code() == 401 || response.code() == 404) {
                    mostrarError("Usuario o contraseña incorrectos");
                } else {
                    mostrarError(extraerMensajeError(response, "Error del servidor"));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                mostrarError("Sin conexión: " + t.getMessage());
            }
        });
    }

    private void irAHome() {
        Navigation.findNavController(requireView())
                .navigate(R.id.action_loginFragment_to_homeFragment);
    }

    private String extraerMensajeError(Response<?> response, String fallback) {
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                ApiError error = new Gson().fromJson(errorBody.string(), ApiError.class);
                if (error != null && error.getMensaje() != null) {
                    return error.getMensaje();
                }
            }
        } catch (Exception ignored) { }
        return fallback + " (" + response.code() + ")";
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }

    private void ocultarError() {
        tvError.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
    }
}