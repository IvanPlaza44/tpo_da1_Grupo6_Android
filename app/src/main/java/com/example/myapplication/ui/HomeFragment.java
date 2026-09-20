package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.session.SessionManager;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class HomeFragment extends Fragment {

    private SessionManager sessionManager;

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

        sessionManager = new SessionManager(requireContext());

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        tvWelcome.setText("¡Login exitoso! Token guardado: " + sessionManager.getToken());

        SwitchMaterial switchBiometria = view.findViewById(R.id.switchBiometria);
        switchBiometria.setChecked(sessionManager.isBiometriaActivada());
        switchBiometria.setOnCheckedChangeListener((buttonView, isChecked) ->
                sessionManager.setBiometriaActivada(isChecked));

        view.findViewById(R.id.btnPublicar).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_publicarFragment));

        view.findViewById(R.id.btnMisPublicaciones).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_misPublicacionesFragment));

        Button btnLogout = view.findViewById(R.id.btnLogout);
        btnLogout.setOnClickListener(v -> {
            sessionManager.cerrarSesion();
            Navigation.findNavController(view)
                    .navigate(R.id.action_homeFragment_to_loginFragment);
        });
    }
}