package com.example.myapplication.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.annotation.Nullable;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.example.myapplication.R;
import com.example.myapplication.network.ApiService;
import com.google.android.material.button.MaterialButton;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Punto 12: cierre de la operación escaneando un código QR.
 *
 * Flujo:
 *  1. Pedimos el permiso CAMERA en tiempo de ejecución.
 *  2. Con permiso, CameraX muestra la cámara (Preview) y analiza cada frame (ImageAnalysis).
 *  3. ML Kit detecta el QR y nos da el texto. El QR contiene un JSON: {"operacionId": 12}
 *  4. Validamos que sea el QR de ESTA operación. Si lo es, llamamos a
 *     PUT /api/operaciones/{id}/entregada (operación ENTREGADA, publicación VENDIDA).
 */
@AndroidEntryPoint
public class QrScannerFragment extends Fragment {

    private static final String TAG = "QrScannerFragment";

    @Inject ApiService apiService;

    private long operacionId;

    private PreviewView previewView;
    private View marcoQr, tvInstruccion, panelPermiso, panelResultado;
    private ProgressBar progreso;
    private TextView tvIcono, tvResultadoTitulo, tvResultadoMensaje;
    private MaterialButton btnResultadoPrincipal, btnResultadoSecundario;

    private ExecutorService cameraExecutor;
    private BarcodeScanner barcodeScanner;
    private ProcessCameraProvider cameraProvider;

    /** Evita procesar el mismo QR muchas veces: la cámara detecta ~30 veces por segundo. */
    private final AtomicBoolean procesando = new AtomicBoolean(false);

    /** Lanzador del diálogo de permiso. Se registra al crear el fragment. */
    private final ActivityResultLauncher<String> pedirPermisoCamara =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), concedido -> {
                if (concedido) {
                    iniciarCamara();
                } else {
                    mostrarPanelPermiso();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_qr_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (getArguments() != null) {
            operacionId = getArguments().getLong("operacionId");
        }

        previewView = view.findViewById(R.id.previewView);
        marcoQr = view.findViewById(R.id.marcoQr);
        tvInstruccion = view.findViewById(R.id.tvInstruccion);
        panelPermiso = view.findViewById(R.id.panelPermiso);
        panelResultado = view.findViewById(R.id.panelResultado);
        progreso = view.findViewById(R.id.progreso);
        tvIcono = view.findViewById(R.id.tvIcono);
        tvResultadoTitulo = view.findViewById(R.id.tvResultadoTitulo);
        tvResultadoMensaje = view.findViewById(R.id.tvResultadoMensaje);
        btnResultadoPrincipal = view.findViewById(R.id.btnResultadoPrincipal);
        btnResultadoSecundario = view.findViewById(R.id.btnResultadoSecundario);

        view.<MaterialButton>findViewById(R.id.btnPermiso)
                .setOnClickListener(v -> pedirPermisoCamara.launch(Manifest.permission.CAMERA));

        cameraExecutor = Executors.newSingleThreadExecutor();

        // Solo QR: más rápido y evita falsos positivos con otros códigos.
        barcodeScanner = BarcodeScanning.getClient(
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                        .build());

        if (operacionId <= 0) {
            mostrarError("Operación inválida", "No se pudo identificar la operación a cerrar.");
            return;
        }

        if (tienePermisoCamara()) {
            iniciarCamara();
        } else {
            pedirPermisoCamara.launch(Manifest.permission.CAMERA);
        }
    }

    private boolean tienePermisoCamara() {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    // ---------------------------------------------------------------- Cámara

    private void iniciarCamara() {
        panelPermiso.setVisibility(View.GONE);

        ListenableFuture<ProcessCameraProvider> futuro =
                ProcessCameraProvider.getInstance(requireContext());

        futuro.addListener(() -> {
            try {
                cameraProvider = futuro.get();
                if (isAdded()) enlazarCasosDeUso();
            } catch (Exception e) {
                Log.e(TAG, "No se pudo iniciar la cámara", e);
                if (isAdded()) {
                    mostrarError("Cámara no disponible", "No se pudo iniciar la cámara del dispositivo.");
                }
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private void enlazarCasosDeUso() {
        // 1) Preview: lo que se ve en pantalla.
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        // 2) ImageAnalysis: recibe cada frame para detectar el QR.
        ImageAnalysis analisis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        analisis.setAnalyzer(cameraExecutor, this::analizarFrame);

        try {
            cameraProvider.unbindAll();
            // bindToLifecycle: la cámara se abre/cierra sola con el ciclo de vida del fragment.
            cameraProvider.bindToLifecycle(getViewLifecycleOwner(),
                    CameraSelector.DEFAULT_BACK_CAMERA, preview, analisis);
        } catch (Exception e) {
            Log.e(TAG, "Fallo al enlazar la cámara", e);
            mostrarError("Cámara no disponible", "No se pudo abrir la cámara trasera.");
        }
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analizarFrame(@NonNull ImageProxy imageProxy) {
        if (imageProxy.getImage() == null || procesando.get()) {
            imageProxy.close();
            return;
        }

        InputImage imagen = InputImage.fromMediaImage(
                imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());

        barcodeScanner.process(imagen)
                .addOnSuccessListener(codigos -> {
                    // compareAndSet: solo el primer QR detectado pasa; el resto se ignora.
                    if (!codigos.isEmpty() && procesando.compareAndSet(false, true)) {
                        String valor = codigos.get(0).getRawValue();
                        requireActivity().runOnUiThread(() -> procesarQr(valor));
                    }
                })
                .addOnFailureListener(e -> Log.e(TAG, "ML Kit falló", e))
                // IMPORTANTE: sin close() CameraX no entrega más frames.
                .addOnCompleteListener(t -> imageProxy.close());
    }

    // ------------------------------------------------------------ Validación

    private void procesarQr(@Nullable String contenido) {
        if (!isAdded()) return;

        try {
            JSONObject json = new JSONObject(contenido == null ? "" : contenido);
            long idDelQr = json.getLong("operacionId");

            if (idDelQr != operacionId) {
                mostrarError("Código incorrecto",
                        "Este QR pertenece a otra operación. Pedile al vendedor el QR de este artículo.");
                return;
            }
            confirmarEntrega();
        } catch (JSONException e) {
            mostrarError("Código no válido",
                    "El QR escaneado no es un código de entrega de Ronda.");
        }
    }

    // ----------------------------------------------------------------- Backend

    private void confirmarEntrega() {
        mostrarCargando();

        apiService.marcarEntregada(operacionId).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(@NonNull Call<Void> call, @NonNull Response<Void> response) {
                if (!isAdded()) return;

                if (response.isSuccessful()) {
                    mostrarExito("¡Operación confirmada!",
                            "La entrega quedó registrada y la publicación pasó a vendida. "
                                    + "Ya podés calificar desde tu Historial.");
                } else if (response.code() == 409) {
                    mostrarExito("Entrega ya registrada",
                            "Esta operación ya estaba cerrada.");
                } else {
                    mostrarErrorConReintento("No se pudo confirmar",
                            "El servidor respondió con el código " + response.code() + ".");
                }
            }

            @Override
            public void onFailure(@NonNull Call<Void> call, @NonNull Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error de red: " + t.getMessage());
                mostrarErrorConReintento("Sin conexión",
                        "No pudimos comunicarnos con el servidor. Revisá tu internet e intentá de nuevo.");
            }
        });
    }

    // ---------------------------------------------------------- Estados de UI

    private void mostrarPanelPermiso() {
        panelPermiso.setVisibility(View.VISIBLE);
        panelResultado.setVisibility(View.GONE);
    }

    private void mostrarCargando() {
        panelResultado.setVisibility(View.VISIBLE);
        progreso.setVisibility(View.VISIBLE);
        tvIcono.setVisibility(View.GONE);
        tvResultadoTitulo.setText("Confirmando entrega...");
        tvResultadoMensaje.setText("");
        btnResultadoPrincipal.setVisibility(View.GONE);
        btnResultadoSecundario.setVisibility(View.GONE);
    }

    private void mostrarExito(String titulo, String mensaje) {
        prepararResultado(true, titulo, mensaje);
        btnResultadoPrincipal.setText("Listo");
        btnResultadoPrincipal.setOnClickListener(v -> volver());
        btnResultadoSecundario.setVisibility(View.GONE);
    }

    /** QR inválido o de otra operación: se puede volver a escanear. */
    private void mostrarError(String titulo, String mensaje) {
        prepararResultado(false, titulo, mensaje);
        btnResultadoPrincipal.setText("Escanear de nuevo");
        btnResultadoPrincipal.setOnClickListener(v -> reanudarEscaneo());
        btnResultadoSecundario.setText("Cancelar");
        btnResultadoSecundario.setVisibility(View.VISIBLE);
        btnResultadoSecundario.setOnClickListener(v -> volver());
    }

    /** El QR era correcto pero falló la red/servidor: reintenta la confirmación, sin re-escanear. */
    private void mostrarErrorConReintento(String titulo, String mensaje) {
        prepararResultado(false, titulo, mensaje);
        btnResultadoPrincipal.setText("Reintentar");
        btnResultadoPrincipal.setOnClickListener(v -> confirmarEntrega());
        btnResultadoSecundario.setText("Cancelar");
        btnResultadoSecundario.setVisibility(View.VISIBLE);
        btnResultadoSecundario.setOnClickListener(v -> volver());
    }

    private void prepararResultado(boolean ok, String titulo, String mensaje) {
        panelResultado.setVisibility(View.VISIBLE);
        progreso.setVisibility(View.GONE);
        tvIcono.setVisibility(View.VISIBLE);
        tvIcono.setText(ok ? "✓" : "✕");
        tvIcono.setTextColor(ContextCompat.getColor(requireContext(),
                ok ? R.color.chip_publicacion_activa_text : R.color.on_error_container));
        tvIcono.setBackgroundResource(ok ? R.drawable.bg_qr_circulo_ok : R.drawable.bg_qr_circulo_error);
        tvResultadoTitulo.setText(titulo);
        tvResultadoMensaje.setText(mensaje);
        btnResultadoPrincipal.setVisibility(View.VISIBLE);
    }

    private void reanudarEscaneo() {
        panelResultado.setVisibility(View.GONE);
        procesando.set(false);
    }

    private void volver() {
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) cameraProvider.unbindAll();
        if (barcodeScanner != null) barcodeScanner.close();
        if (cameraExecutor != null) cameraExecutor.shutdown();
    }
}