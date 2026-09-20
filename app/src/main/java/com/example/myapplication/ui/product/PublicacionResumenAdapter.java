package com.example.myapplication.ui.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;

import java.util.List;

public class PublicacionResumenAdapter extends RecyclerView.Adapter<PublicacionResumenAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onPausar(PublicacionResumen p);
        void onReactivar(PublicacionResumen p);
    }

    private List<PublicacionResumen> lista;
    private final OnAccionListener listener;

    public PublicacionResumenAdapter(List<PublicacionResumen> lista, OnAccionListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    public void actualizarLista(List<PublicacionResumen> nueva) {
        this.lista = nueva;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_publicacion_resumen, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PublicacionResumen p = lista.get(position);

        holder.tvTitulo.setText(p.titulo != null ? p.titulo : "(sin titulo)");
        holder.tvPrecio.setText(p.precio != null ? "$" + p.precio : "");

        String estado = p.estado != null ? p.estado : "";
        holder.tvEstado.setText(estado);

        switch (estado) {
            case "ACTIVA":
                holder.tvEstado.setBackgroundResource(android.R.color.holo_green_light);
                holder.btnAccion.setVisibility(View.VISIBLE);
                holder.btnAccion.setText("Pausar");
                holder.btnAccion.setOnClickListener(v -> listener.onPausar(p));
                break;
            case "PAUSADA":
                holder.tvEstado.setBackgroundResource(android.R.color.holo_orange_light);
                holder.btnAccion.setVisibility(View.VISIBLE);
                holder.btnAccion.setText("Reactivar");
                holder.btnAccion.setOnClickListener(v -> listener.onReactivar(p));
                break;
            case "VENDIDA":
            default:
                holder.tvEstado.setBackgroundResource(android.R.color.darker_gray);
                holder.btnAccion.setVisibility(View.GONE);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvPrecio, tvEstado;
        Button btnAccion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnAccion = itemView.findViewById(R.id.btnAccion);
        }
    }
}