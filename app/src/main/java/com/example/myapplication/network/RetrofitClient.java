//package com.example.myapplication.network;
//
//import java.util.concurrent.TimeUnit;
//
//import okhttp3.OkHttpClient;
//import retrofit2.Retrofit;
//import retrofit2.converter.gson.GsonConverterFactory;
//
//public class RetrofitClient {
//
//    // TODO: URL API Rest Ronda.
//    private static final String BASE_URL = "https://api.ritmofit.com/";
//
//    //ACA SERA LA URL LOCAL
////    private static final String BASE_URL = "https://localhost:8080";
//
//
//    private static Retrofit retrofit;
//    public static ApiService getApiService() {
//        if (retrofit == null) {
//            OkHttpClient client = new OkHttpClient.Builder()
//                    .connectTimeout(30, TimeUnit.SECONDS)
//                    .readTimeout(30, TimeUnit.SECONDS)
//                    .writeTimeout(30, TimeUnit.SECONDS)
//                    .build();
//
//            retrofit = new Retrofit.Builder()
//                    .baseUrl(BASE_URL)
//                    .client(client)
//                    .addConverterFactory(
//                            GsonConverterFactory.create()
//                    )
//                    .build();
//        }
//        return retrofit.create(ApiService.class);
//    }
//}