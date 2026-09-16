package com.example.myapplication.session;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {

    private static final String PREFS_NAME = "ronda_session";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_USUARIO_ID = "usuarioId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_USERNAME = "username";

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
        prefs.edit().clear().apply();
    }
}