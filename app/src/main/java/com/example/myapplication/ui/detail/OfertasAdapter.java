package com.example.myapplication.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OfertaResponseDto;

import java.util.List;
import java.util.Locale;

public class OfertasAdapter extends RecyclerView.Adapter<OfertasAdapter.OfertaViewHolder> {

    public interface OnAccionOfertaListener {
        void onAceptar(OfertaResponseDto oferta);
        void onRechazar(OfertaResponseDto oferta);
    }

    private final List<OfertaResponseDto> ofertas;
    private final OnOfertaAceptadaListener listener;
    private final OnAccionOfertaListener listener;

    // Creamos una interfaz para escuchar los clics
    public interface OnOfertaAceptadaListener {
        void onAceptarClick(OfertaResponseDto oferta);
    }

    public OfertasAdapter(List<OfertaResponseDto> ofertas, OnOfertaAceptadaListener listener) {
    public OfertasAdapter(List<OfertaResponseDto> ofertas, OnAccionOfertaListener listener) {
        this.ofertas = ofertas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_oferta, parent, false);
        return new OfertaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfertaViewHolder holder, int position) {
        OfertaResponseDto oferta = ofertas.get(position);

        holder.tvAutor.setText(oferta.autorNombre);
        holder.tvMonto.setText(String.format(Locale.getDefault(), "$ %.2f", oferta.monto));
        holder.tvEstado.setText(oferta.estado);

        if (oferta.mensaje != null && !oferta.mensaje.isEmpty()) {
            holder.tvMensaje.setText(oferta.mensaje);
            holder.tvMensaje.setVisibility(View.VISIBLE);
        } else {
            holder.tvMensaje.setVisibility(View.GONE);
        }

        // Le avisamos al Fragment que se tocó este botón
        holder.btnAceptar.setOnClickListener(v -> listener.onAceptarClick(oferta));

        boolean esPendiente = "PENDIENTE".equals(oferta.estado);
        holder.layoutAcciones.setVisibility(esPendiente ? View.VISIBLE : View.GONE);

        if (esPendiente) {
            holder.btnAceptar.setOnClickListener(v -> listener.onAceptar(oferta));
            holder.btnRechazar.setOnClickListener(v -> listener.onRechazar(oferta));
        }
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    static class OfertaViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutor, tvMonto, tvMensaje, tvEstado;
        LinearLayout layoutAcciones;
        Button btnAceptar, btnRechazar;
        Button btnAceptar;

        OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutor = itemView.findViewById(R.id.tvOfertaAutor);
            tvMonto = itemView.findViewById(R.id.tvOfertaMonto);
            tvMensaje = itemView.findViewById(R.id.tvOfertaMensaje);
            tvEstado = itemView.findViewById(R.id.tvOfertaEstado);
            layoutAcciones = itemView.findViewById(R.id.layoutAccionesOferta);
            btnAceptar = itemView.findViewById(R.id.btnAceptarOferta);
            btnRechazar = itemView.findViewById(R.id.btnRechazarOferta);
            btnAceptar = itemView.findViewById(R.id.btnAceptarOferta); // Enlazamos el botón
        }
    }
}