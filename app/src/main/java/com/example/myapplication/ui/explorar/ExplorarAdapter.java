package com.example.myapplication.ui.explorar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;

import java.util.List;

public class ExplorarAdapter extends RecyclerView.Adapter<ExplorarAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(PublicacionResumen publicacion);
    }

    private List<PublicacionResumen> lista;
    private final OnItemClickListener listener;

    public ExplorarAdapter(List<PublicacionResumen> lista, OnItemClickListener listener) {
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
                .inflate(R.layout.item_publicacion_explorar, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        PublicacionResumen p = lista.get(position);

        holder.tvTitulo.setText(p.titulo != null ? p.titulo : "(sin titulo)");
        holder.tvPrecio.setText(p.precio != null ? "$" + p.precio : "");
        holder.tvZona.setText(p.zonaEntrega != null ? p.zonaEntrega : "");
        holder.tvVendedor.setText(p.vendedorNombre != null ? p.vendedorNombre : "");

        holder.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitulo, tvPrecio, tvZona, tvVendedor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvZona = itemView.findViewById(R.id.tvZona);
            tvVendedor = itemView.findViewById(R.id.tvVendedor);
        }
    }
}