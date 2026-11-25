package com.example.myapplication;

import android.os.Bundle;
import android.view.View;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.example.myapplication.avisos.AvisosService;
import com.example.myapplication.avisos.NotificationHelper;
import com.example.myapplication.session.SessionManager;

import javax.inject.Inject;
import com.google.android.material.appbar.MaterialToolbar;

import java.util.HashSet;
import java.util.Set;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private MaterialToolbar toolbar;
    private View toolbarContainer;

    @Inject SessionManager sessionManager;

    private final ActivityResultLauncher<String> pedirPermisoNotificaciones =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    concedido -> {  });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        toolbar = findViewById(R.id.toolbar);
        toolbarContainer = findViewById(R.id.toolbar_container);
        View mainContainer = findViewById(R.id.main_container);
        View navHost = findViewById(R.id.nav_host_fragment);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        navController = navHostFragment.getNavController();

        Set<Integer> topLevelDestinations = new HashSet<>();
        topLevelDestinations.add(R.id.loginFragment);
        topLevelDestinations.add(R.id.homeFragment);
        appBarConfiguration = new AppBarConfiguration.Builder(topLevelDestinations).build();

        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
            boolean hideToolbar = destination.getId() == R.id.loginFragment
                    || destination.getId() == R.id.homeFragment;
            toolbarContainer.setVisibility(hideToolbar ? View.GONE : View.VISIBLE);
            if (destination.getId() == R.id.homeFragment && sessionManager.isLoggedIn()) {
                pedirPermisoNotificacionesSiHaceFalta();
                AvisosService.iniciar(this);
            }
        });

        ViewCompat.setOnApplyWindowInsetsListener(mainContainer, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0);
            navHost.setPadding(0, 0, 0, systemBars.bottom);
            return insets;
        });

        manejarAviso(getIntent());
    }

    @Override
    public boolean onSupportNavigateUp() {
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        manejarAviso(intent);
    }

    private void pedirPermisoNotificacionesSiHaceFalta() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            pedirPermisoNotificaciones.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void manejarAviso(Intent intent) {
        if (intent == null || !intent.hasExtra(NotificationHelper.EXTRA_TIPO)) return;
        if (!sessionManager.isLoggedIn()) return;

        String tipo = intent.getStringExtra(NotificationHelper.EXTRA_TIPO);
        long publicacionId = intent.getLongExtra(NotificationHelper.EXTRA_PUBLICACION_ID, -1);

        intent.removeExtra(NotificationHelper.EXTRA_TIPO);

        if (tipo != null && tipo.startsWith("OFERTA")) {
            navController.navigate(R.id.misOfertasFragment);
        } else if (publicacionId != -1) {
            Bundle args = new Bundle();
            args.putLong("publicacionId", publicacionId);
            navController.navigate(R.id.publicacionDetalleFragment, args);
        }
    }
}
