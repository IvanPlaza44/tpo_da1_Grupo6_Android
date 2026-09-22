package com.example.myapplication.session;

import android.content.Context;
import android.content.SharedPreferences;

import javax.inject.Inject;
import javax.inject.Singleton;

import dagger.hilt.android.qualifiers.ApplicationContext;

@Singleton
public class SessionManager {

    private static final String PREFS_NAME = "ronda_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USUARIO_ID = "usuarioId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";
    private static final String KEY_BIOMETRIC = "biometric_enabled";
    private static final String KEY_PASSWORD = "password";

    private final SharedPreferences prefs;

    @Inject
    public SessionManager(@ApplicationContext Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public void guardarSesion(String token, Long usuarioId, String email, String username) {
        prefs.edit()
                .putString(KEY_TOKEN, token)
                .putLong(KEY_USUARIO_ID, usuarioId != null ? usuarioId : -1)
                .putString(KEY_EMAIL, email)
                .putString(KEY_USERNAME, username)
                .apply();
    }

    public void guardarPassword(String password) {
        prefs.edit().putString(KEY_PASSWORD, password).apply();
    }

    public String getPassword() {
        return prefs.getString(KEY_PASSWORD, null);
    }

    public void setBiometriaActivada(boolean activada) {
        prefs.edit().putBoolean(KEY_BIOMETRIC, activada).apply();
    }

    public boolean isBiometriaActivada() {
        return prefs.getBoolean(KEY_BIOMETRIC, false);
    }

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
        boolean biometria = isBiometriaActivada();
        String email = getEmail();
        String password = getPassword();

        prefs.edit().clear().apply();

        if (biometria && email != null && password != null) {
            prefs.edit()
                    .putBoolean(KEY_BIOMETRIC, true)
                    .putString(KEY_EMAIL, email)
                    .putString(KEY_PASSWORD, password)
                    .apply();
        }
    }
}