package com.example.myapplication.avisos;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.myapplication.MainActivity;
import com.example.myapplication.R;
import com.example.myapplication.model.NovedadDto;

public class NotificationHelper {

    public static final String CANAL_AVISOS = "avisos_ronda";
    public static final String CANAL_SERVICIO = "servicio_ronda";

    // Claves de los datos que viajan dentro del Intent al tocar el aviso
    public static final String EXTRA_TIPO = "aviso_tipo";
    public static final String EXTRA_PUBLICACION_ID = "aviso_publicacion_id";

    public static void crearCanales(Context context) {
        NotificationManager nm = context.getSystemService(NotificationManager.class);

        NotificationChannel avisos = new NotificationChannel(
                CANAL_AVISOS, "Avisos de Ronda", NotificationManager.IMPORTANCE_DEFAULT);
        avisos.setDescription("Preguntas y ofertas sobre tus publicaciones");

        NotificationChannel servicio = new NotificationChannel(
                CANAL_SERVICIO, "Escuchando avisos", NotificationManager.IMPORTANCE_MIN);

        nm.createNotificationChannel(avisos);
        nm.createNotificationChannel(servicio);
    }

    public static Notification crearNotificacionServicio(Context context) {
        return new NotificationCompat.Builder(context, CANAL_SERVICIO)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("Ronda")
                .setContentText("Esperando avisos…")
                .setOngoing(true)
                .build();
    }

    @SuppressLint("MissingPermission")
    public static void mostrarAviso(Context context, NovedadDto n) {
        NotificationManagerCompat manager = NotificationManagerCompat.from(context);
        if (!manager.areNotificationsEnabled()) return;

        Intent abrir = new Intent(context, MainActivity.class);
        abrir.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        abrir.putExtra(EXTRA_TIPO, n.tipo);
        abrir.putExtra(EXTRA_PUBLICACION_ID, n.publicacionId != null ? n.publicacionId : -1L);

        PendingIntent pendiente = PendingIntent.getActivity(
                context,
                (int) n.id,
                abrir,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Notification notif = new NotificationCompat.Builder(context, CANAL_AVISOS)
                .setSmallIcon(R.drawable.ic_notificacion)
                .setContentTitle("Ronda")
                .setContentText(n.mensaje)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(n.mensaje))
                .setContentIntent(pendiente)
                .setAutoCancel(true)
                .build();

        manager.notify((int) n.id, notif);
    }
}