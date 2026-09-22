package com.example.myapplication.ui.product;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.google.android.material.button.MaterialButton;

import java.util.List;

public class PublicacionResumenAdapter extends RecyclerView.Adapter<PublicacionResumenAdapter.ViewHolder> {

    public interface OnAccionListener {
        void onPausar(PublicacionResumen p);
        void onReactivar(PublicacionResumen p);
        void onVerDetalle(PublicacionResumen p);
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
        var context = holder.itemView.getContext();

        holder.tvTitulo.setText(p.titulo != null ? p.titulo : "(sin titulo)");
        holder.tvPrecio.setText(p.precio != null ? "$" + p.precio : "");
        holder.itemView.setOnClickListener(v -> listener.onVerDetalle(p));

        String estado = p.estado != null ? p.estado : "";
        holder.tvEstado.setText(estado);

        switch (estado) {
            case "ACTIVA":
                holder.tvEstado.setBackgroundResource(R.drawable.bg_chip_publicacion_activa);
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.chip_publicacion_activa_text));
                holder.btnAccion.setVisibility(View.VISIBLE);
                holder.btnAccion.setText("Pausar");
                holder.btnAccion.setOnClickListener(v -> listener.onPausar(p));
                break;
            case "PAUSADA":
                holder.tvEstado.setBackgroundResource(R.drawable.bg_chip_publicacion_pausada);
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.chip_publicacion_pausada_text));
                holder.btnAccion.setVisibility(View.VISIBLE);
                holder.btnAccion.setText("Reactivar");
                holder.btnAccion.setOnClickListener(v -> listener.onReactivar(p));
                break;
            case "VENDIDA":
            default:
                holder.tvEstado.setBackgroundResource(R.drawable.bg_chip_publicacion_vendida);
                holder.tvEstado.setTextColor(ContextCompat.getColor(context, R.color.chip_publicacion_vendida_text));
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
        MaterialButton btnAccion;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstado = itemView.findViewById(R.id.tvEstado);
            btnAccion = itemView.findViewById(R.id.btnAccion);
        }
    }
}
