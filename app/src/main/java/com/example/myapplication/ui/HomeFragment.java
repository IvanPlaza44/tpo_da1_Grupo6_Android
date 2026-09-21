package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.myapplication.R;
import com.example.myapplication.session.SessionManager;

public class HomeFragment extends Fragment {

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

        SessionManager sessionManager = new SessionManager(requireContext());
        String email = sessionManager.getEmail();

        TextView tvSubtitulo = view.findViewById(R.id.tvSubtitulo);
        tvSubtitulo.setText(email != null && !email.isEmpty() ? email : "Bienvenido");

        view.findViewById(R.id.btnExplorar).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_explorarFragment));

        view.findViewById(R.id.btnPublicar).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_publicarFragment));

        view.findViewById(R.id.btnMisPublicaciones).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_misPublicacionesFragment));

        view.findViewById(R.id.btnFavoritos).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_favoritosFragment));

        view.findViewById(R.id.btnBusquedasGuardadas).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_busquedasGuardadasFragment));

        view.findViewById(R.id.btnPerfil).setOnClickListener(v ->
                Navigation.findNavController(v).navigate(R.id.action_homeFragment_to_profileFragment));
    }
}
