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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.model.LoginRequest;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.network.RetrofitClient;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterFragment extends Fragment {

    // Acá están TODAS las variables declaradas correctamente
    private EditText etRegUsuario, etRegPassword;
    private Button btnRegistrar;
    private TextView tvRegError, tvIrALogin;
    private ProgressBar pbRegLoading;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Enlaces con el XML
        etRegUsuario = view.findViewById(R.id.etRegUsuario);
        etRegPassword = view.findViewById(R.id.etRegPassword);
        btnRegistrar = view.findViewById(R.id.btnRegistrar);
        tvRegError = view.findViewById(R.id.tvRegError);
        tvIrALogin = view.findViewById(R.id.tvIrALogin);
        pbRegLoading = view.findViewById(R.id.pbRegLoading);

        btnRegistrar.setOnClickListener(v -> intentarRegistro());

        tvIrALogin.setOnClickListener(v ->
                Navigation.findNavController(view).popBackStack()
        );
    }

    private void intentarRegistro() {
        String usuario = etRegUsuario.getText().toString().trim();
        String password = etRegPassword.getText().toString().trim();

        tvRegError.setVisibility(View.GONE);

        if (TextUtils.isEmpty(usuario) || TextUtils.isEmpty(password)) {
            mostrarError("Completá todos los campos");
            return;
        }

        if (password.length() < 6) {
            mostrarError("La contraseña debe tener al menos 6 caracteres");
            return;
        }

        setLoading(true);

        ApiService api = RetrofitClient.getApiService(requireContext());
        api.registrar(new LoginRequest(usuario, password)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                setLoading(false);

                if (response.isSuccessful()) {
                    Toast.makeText(requireContext(), "Cuenta creada con éxito. Iniciá sesión.", Toast.LENGTH_LONG).show();
                    Navigation.findNavController(requireView()).popBackStack();
                } else if (response.code() == 400 || response.code() == 409) {
                    mostrarError("El usuario o email ya está registrado");
                } else {
                    mostrarError("Error al registrar (" + response.code() + ")");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                setLoading(false);
                mostrarError("Sin conexión: " + t.getMessage());
            }
        });
    }

    private void mostrarError(String mensaje) {
        tvRegError.setText(mensaje);
        tvRegError.setVisibility(View.VISIBLE);
    }

    private void setLoading(boolean loading) {
        pbRegLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegistrar.setEnabled(!loading);
    }
}