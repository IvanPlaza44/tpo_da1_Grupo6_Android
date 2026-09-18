package com.example.myapplication;

import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.example.myapplication.ui.detail.PublicacionDetalleFragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // --- SOLO PARA PROBAR EL DETALLE, BORRAR DESPUÉS ---
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.main_container, PublicacionDetalleFragment.newInstance(3))
                .commit();
        // --- FIN BLOQUE DE PRUEBA ---

        /* VERSIÓN VIEJA (con Navigation Component) - COMENTADA TEMPORALMENTE
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        NavController navController = navHostFragment.getNavController();
        Bundle args = new Bundle();
        args.putLong("publicacionId", 3);
        navController.navigate(R.id.publicacionDetalleFragment, args);
        */
    }
}