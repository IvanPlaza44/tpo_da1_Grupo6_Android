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
import com.example.myapplication.model.EmailRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;
import com.google.gson.Gson;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RequestOtpFragment extends Fragment {

    private EditText etEmail;
    private Button btnEnviar;
    private TextView tvError;
    private ProgressBar progressBar;

    private Button btnVolver;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_request_otp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etEmail = view.findViewById(R.id.etEmail);
        btnEnviar = view.findViewById(R.id.btnEnviar);
        tvError = view.findViewById(R.id.tvError);
        progressBar = view.findViewById(R.id.progressBar);

        btnVolver = view.findViewById(R.id.btnVolver);
        btnVolver.setOnClickListener(v -> {
            // Esto hace el mismo efecto que tocar la flecha física de "Atrás" en el celular
            Navigation.findNavController(v).popBackStack();
        });

        btnEnviar.setOnClickListener(v -> solicitarCodigo());
    }

    private void solicitarCodigo() {
        String email = etEmail.getText().toString().trim();
        tvError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(email)) {
            mostrarError("Ingresá tu email");
            return;
        }

        setLoading(true);

        ApiService api = RetrofitClient.getApiService(requireContext());
        api.solicitarOtp(new EmailRequest(email)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    Bundle args = new Bundle();
                    args.putString("email", email);
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_requestOtpFragment_to_verifyOtpFragment, args);
                } else {
                    mostrarError(extraerMensajeError(response));
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
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
        btnEnviar.setEnabled(!loading);
    }
}