package com.example.myapplication.network;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import com.example.myapplication.session.SessionManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    private static final String BASE_URL = "https://ronda-api-aut4.onrender.com/";
    private static Retrofit retrofit;

    public static ApiService getApiService(Context context) {
        if (retrofit == null) {
            SessionManager sessionManager = new SessionManager(context);

            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Agrega el header Authorization automáticamente, EXCEPTO en login/otp.
            Interceptor authInterceptor = chain -> {
                Request original = chain.request();

                // Si la ruta contiene "/auth/", pasamos de largo sin agregar Token
                if (original.url().encodedPath().contains("/auth/")) {
                    return chain.proceed(original);
                }

                String token = sessionManager.getToken();
                if (token == null) {
                    return chain.proceed(original);
                }

                Request withAuth = original.newBuilder()
                        .header("Authorization", "Bearer " + token)
                        .build();
                return chain.proceed(withAuth);
            };

            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(90, TimeUnit.SECONDS) // Subir a 90s
                    .readTimeout(90, TimeUnit.SECONDS)
                    .writeTimeout(90, TimeUnit.SECONDS)
                    .addInterceptor(authInterceptor)
                    .addInterceptor(logging)
                    .build();

            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit.create(ApiService.class);
    }
}