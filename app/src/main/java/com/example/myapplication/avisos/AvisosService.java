package com.example.myapplication.avisos;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.example.myapplication.model.NovedadDto;
import com.example.myapplication.network.ApiService;
import com.example.myapplication.session.SessionManager;

import java.io.IOException;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Response;

@AndroidEntryPoint
public class AvisosService extends Service {

    private static final String TAG = "AvisosService";
    private static final int ID_NOTIFICACION_SERVICIO = 1;

    @Inject ApiService apiService;
    @Inject SessionManager sessionManager;

    private volatile boolean activo = false;
    private volatile Call<List<NovedadDto>> llamadaActual;
    private Thread hilo;

    public static void iniciar(Context context) {
        ContextCompat.startForegroundService(context, new Intent(context, AvisosService.class));
    }

    public static void detener(Context context) {
        context.stopService(new Intent(context, AvisosService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        NotificationHelper.crearCanales(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Un Foreground Service tiene que mostrar su notificación fija enseguida.
        startForeground(
                ID_NOTIFICACION_SERVICIO,
                NotificationHelper.crearNotificacionServicio(this),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);

        if (!sessionManager.isLoggedIn()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (!activo) {
            activo = true;
            hilo = new Thread(this::escucharAvisos, "avisos-long-polling");
            hilo.start();
        }
        return START_STICKY;
    }

    private void escucharAvisos() {
        Log.d(TAG, "Escuchando avisos...");

        while (activo && sessionManager.isLoggedIn()) {
            long inicio = System.currentTimeMillis();
            try {
                Call<List<NovedadDto>> call = apiService.esperarNovedades();
                llamadaActual = call;

                Response<List<NovedadDto>> resp = call.execute();

                if (resp.isSuccessful() && resp.body() != null) {
                    List<NovedadDto> novedades = resp.body();
                    Log.d(TAG, "Avisos recibidos: " + novedades.size());

                    for (NovedadDto n : novedades) {
                        NotificationHelper.mostrarAviso(this, n);
                    }

                    if (novedades.isEmpty() && System.currentTimeMillis() - inicio < 1000) {
                        dormir(2000);
                    }
                } else if (resp.code() == 401 || resp.code() == 403) {
                    Log.d(TAG, "Sesión inválida, se detiene el servicio");
                    break;
                } else {
                    dormir(5000);
                }

            } catch (IOException e) {
                if (!activo) break;
                Log.d(TAG, "Error de red: " + e.getMessage());
                dormir(5000);
            } catch (RuntimeException e) {
                Log.e(TAG, "Error inesperado", e);
                dormir(5000);
            }
        }

        if (activo) {
            activo = false;
            stopSelf();
        }
    }

    private void dormir(long milisegundos) {
        try {
            Thread.sleep(milisegundos);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            activo = false;
        }
    }

    @Override
    public void onDestroy() {
        activo = false;
        if (llamadaActual != null) llamadaActual.cancel();
        if (hilo != null) hilo.interrupt();
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}