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

public class PlaceholderHomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Ahora sí inflamos tu diseño XML
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView tvWelcome = view.findViewById(R.id.tvWelcome);
        Button btnLogout = view.findViewById(R.id.btnLogout);

        SessionManager sessionManager = new SessionManager(requireContext());

        tvWelcome.setText("✅ Login exitoso\n" + sessionManager.getEmail());

        btnLogout.setOnClickListener(v -> {
            // 1. borra el token
            sessionManager.cerrarSesion();

            // 2. Volvemos al Login limpiando el historial para que no pueda volver con la flecha de atrás
            Navigation.findNavController(view).navigate(R.id.loginFragment);
        });
    }
}