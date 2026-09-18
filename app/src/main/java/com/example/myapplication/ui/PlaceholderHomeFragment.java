package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.biometric.BiometricAuthManager;
import com.example.myapplication.session.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class PlaceholderHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        // Declarado correctamente con la clase de Material Design
        SwitchMaterial switchBiometria = view.findViewById(R.id.switchBiometria);

        SessionManager sessionManager = new SessionManager(requireContext());
        tvWelcome.setText("✅ Login exitoso\n" + sessionManager.getEmail());

        switchBiometria.setChecked(sessionManager.isBiometriaActivada());

        switchBiometria.setOnClickListener(v -> {
            if (switchBiometria.isChecked()) {
                BiometricAuthManager biometricManager = new BiometricAuthManager(requireActivity(), () -> {
                    sessionManager.setBiometriaActivada(true);
                    Toast.makeText(getContext(), "Huella vinculada con éxito", Toast.LENGTH_SHORT).show();
                });

                if (biometricManager.canAuthenticate()) {
                    biometricManager.showBiometricPrompt();
                } else {
                    Toast.makeText(getContext(), "El dispositivo no soporta huella", Toast.LENGTH_SHORT).show();
                    switchBiometria.setChecked(false);
                }
            } else {
                sessionManager.setBiometriaActivada(false);
                Toast.makeText(getContext(), "Ingreso con huella desactivado", Toast.LENGTH_SHORT).show();
            }
        });

        btnLogout.setOnClickListener(v -> {
            sessionManager.cerrarSesion();
            Navigation.findNavController(view).navigate(R.id.loginFragment);
        });
    }
}