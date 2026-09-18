package com.example.myapplication.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "ronda_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USUARIO_ID = "usuarioId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_BIOMETRIC = "biometric_enabled"; // NUEVO

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String token, Long usuarioId, String email, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USUARIO_ID, usuarioId != null ? usuarioId : -1)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    // --- NUEVOS MÉTODOS PARA LA HUELLA ---
    public void setBiometriaActivada(boolean activada) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, activada).apply();
    }

    public boolean isBiometriaActivada() {
        return prefs.getBoolean(KEY_BIOMETRIC, false);
    }
    // -------------------------------------

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public long getUsuarioId() {
        return prefs.getLong(KEY_USUARIO_ID, -1);
    }

    public String getEmail() {
        return prefs.getString(KEY_EMAIL, null);
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void cerrarSesion() {
        // 1. Guardamos temporalmente si tenía la huella activa y su email
        boolean biometria = isBiometriaActivada();
        String email = getEmail();

        // 2. Borramos TODA la sesión (incluido el token)
        prefs.edit().clear().apply();

        // 3. Restauramos la huella y el email para que el LoginFragment lo recuerde
        if (biometria && email != null) {
            prefs.edit()
                    .putBoolean(KEY_BIOMETRIC, true)
                    .putString(KEY_EMAIL, email)
                    .apply();
        }
    }
}