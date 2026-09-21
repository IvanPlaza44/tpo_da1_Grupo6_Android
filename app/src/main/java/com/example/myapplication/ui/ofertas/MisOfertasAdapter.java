package com.example.myapplication.ui.ofertas;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OfertaResponseDto;

import java.util.List;
import java.util.Locale;

public class MisOfertasAdapter extends RecyclerView.Adapter<MisOfertasAdapter.OfertaViewHolder> {

    public interface OnAccionListener {
        void onAceptar(OfertaResponseDto oferta);
        void onRechazar(OfertaResponseDto oferta);
        void onContraofertar(OfertaResponseDto oferta);
    }

    private final List<OfertaResponseDto> ofertas;
    private final boolean esRecibidas; // true = vendedor viendo lo que le ofrecieron
    private final OnAccionListener listener;

    public MisOfertasAdapter(List<OfertaResponseDto> ofertas, boolean esRecibidas, OnAccionListener listener) {
        this.ofertas = ofertas;
        this.esRecibidas = esRecibidas;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OfertaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mi_oferta, parent, false);
        return new OfertaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OfertaViewHolder holder, int position) {
        OfertaResponseDto oferta = ofertas.get(position);

        holder.tvPublicacion.setText(oferta.publicacionTitulo);
        holder.tvPersona.setText((esRecibidas ? "De: " : "A: ") + oferta.autorNombre);
        holder.tvMonto.setText(String.format(Locale.getDefault(), "$ %.2f", oferta.monto));
        holder.tvEstado.setText(oferta.estado);

        if (oferta.mensaje != null && !oferta.mensaje.isEmpty()) {
            holder.tvMensaje.setText(oferta.mensaje);
            holder.tvMensaje.setVisibility(View.VISIBLE);
        } else {
            holder.tvMensaje.setVisibility(View.GONE);
        }

        boolean esPendiente = "PENDIENTE".equals(oferta.estado);

        if (esPendiente && oferta.fechaVencimiento != null) {
            holder.tvVencimiento.setText("Vence: " + oferta.fechaVencimiento.substring(0, Math.min(10, oferta.fechaVencimiento.length())));
            holder.tvVencimiento.setVisibility(View.VISIBLE);
        } else {
            holder.tvVencimiento.setVisibility(View.GONE);
        }

        // Solo el que RECIBE la oferta (vendedor) puede aceptar/rechazar/contraofertar,
        // y solo si sigue pendiente.
        boolean puedeAccionar = esRecibidas && esPendiente;
        holder.layoutAcciones.setVisibility(puedeAccionar ? View.VISIBLE : View.GONE);

        if (puedeAccionar) {
            holder.btnAceptar.setOnClickListener(v -> listener.onAceptar(oferta));
            holder.btnRechazar.setOnClickListener(v -> listener.onRechazar(oferta));
            holder.btnContraofertar.setOnClickListener(v -> listener.onContraofertar(oferta));
        }
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    static class OfertaViewHolder extends RecyclerView.ViewHolder {
        TextView tvPublicacion, tvPersona, tvMonto, tvMensaje, tvEstado, tvVencimiento;
        LinearLayout layoutAcciones;
        android.widget.Button btnAceptar, btnRechazar, btnContraofertar;

        OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvPublicacion = itemView.findViewById(R.id.tvOfertaPublicacion);
            tvPersona = itemView.findViewById(R.id.tvOfertaPersona);
            tvMonto = itemView.findViewById(R.id.tvOfertaMonto);
            tvMensaje = itemView.findViewById(R.id.tvOfertaMensaje);
            tvEstado = itemView.findViewById(R.id.tvOfertaEstado);
            tvVencimiento = itemView.findViewById(R.id.tvOfertaVencimiento);
            layoutAcciones = itemView.findViewById(R.id.layoutAccionesOferta);
            btnAceptar = itemView.findViewById(R.id.btnAceptarOferta);
            btnRechazar = itemView.findViewById(R.id.btnRechazarOferta);
            btnContraofertar = itemView.findViewById(R.id.btnContraofertarOferta);
        }
    }
}