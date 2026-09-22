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
import com.example.myapplication.model.ApiError;
import com.example.myapplication.model.AuthResponse;
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.model.OtpVerifyRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.session.SessionManager;
import com.google.gson.Gson;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

@AndroidEntryPoint
public class VerifyOtpFragment extends Fragment {

    @Inject ApiService apiService;
    @Inject SessionManager sessionManager;

    private String email;

    private TextView tvEmail, tvError, tvReenviar;
    private EditText etCodigo;
    private Button btnVerificar;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_verify_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        email = getArguments() != null ? getArguments().getString("email") : null;

        tvEmail = view.findViewById(R.id.tvEmail);
        etCodigo = view.findViewById(R.id.etCodigo);
        btnVerificar = view.findViewById(R.id.btnVerificar);
        tvReenviar = view.findViewById(R.id.tvReenviar);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.progressBar);

        tvEmail.setText("Código enviado a " + email);

        btnVerificar.setOnClickListener(v -> verificarCodigo());
        tvReenviar.setOnClickListener(v -> reenviarCodigo());
    }

    private void verificarCodigo() {
        String codigo = etCodigo.getText().toString().trim();
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(codigo)) {
            mostrarError("Ingresá el código");
            return;
        }

        setLoading(true);

        apiService.verificarOtp(new OtpVerifyRequest(email, codigo)).enqueue(new Callback<AuthResponse>() {
            @Override
            public void onResponse(@NonNull Call<AuthResponse> call, @NonNull Response<AuthResponse> response) {
                setLoading(false);

                if (response.isSuccessful() && response.body() != null) {
                    AuthResponse body = response.body();
                    sessionManager.guardarSesion(body.getToken(), body.getUsuarioId(), body.getEmail(), body.getUsername());

                    // TODO: reemplazar homeFragment por el destino real una vez mergeado.
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_verifyOtpFragment_to_homeFragment);
                } else if (response.code() == 400 || response.code() == 401) {
                    mostrarError("Código incorrecto o expirado");
                } else {
                    mostrarError(extraerMensajeError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<AuthResponse> call, @NonNull Throwable t) {
                setLoading(false);
                mostrarError("Sin conexión: " + t.getMessage());
            }
        });
    }

    private void reenviarCodigo() {
        apiService.reenviarOtp(new EmailRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (response.isSuccessful()) {
                    mostrarError("Código reenviado ✓");
                } else {
                    mostrarError(extraerMensajeError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                mostrarError("Sin conexión: " + t.getMessage());
            }
        });
    }

    private String extraerMensajeError(Response<?> response) {
        try {
            ResponseBody errorBody = response.errorBody();
            if (errorBody != null) {
                ApiError error = new Gson().fromJson(errorBody.string(), ApiError.class);
                if (error != null && error.getMensaje() != null) return error.getMensaje();
            }
        } catch (Exception ignored) { }
        return "Error del servidor (" + response.code() + ")";
    }

    private void mostrarError(String mensaje) {
        tvError.setText(mensaje);
        tvError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnVerificar.setEnabled(!loading);
    }
}