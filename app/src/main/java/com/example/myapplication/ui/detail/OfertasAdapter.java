package com.example.myapplication.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OfertaResponseDto;

import java.util.List;
import java.util.Locale;

public class OfertasAdapter extends RecyclerView.Adapter<OfertasAdapter.OfertaViewHolder> {

    private final List<OfertaResponseDto> ofertas;

    public OfertasAdapter(List<OfertaResponseDto> ofertas) {
        this.ofertas = ofertas;
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
    }

    @Override
    public int getItemCount() {
        return ofertas.size();
    }

    static class OfertaViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutor, tvMonto, tvMensaje, tvEstado;

        OfertaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutor = itemView.findViewById(R.id.tvOfertaAutor);
            tvMonto = itemView.findViewById(R.id.tvOfertaMonto);
            tvMensaje = itemView.findViewById(R.id.tvOfertaMensaje);
            tvEstado = itemView.findViewById(R.id.tvOfertaEstado);
        }
    }
}
