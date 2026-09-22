package com.example.myapplication.ui.favoritos;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.FavoritoResponseDto;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FavoritosAdapter extends RecyclerView.Adapter<FavoritosAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(PublicacionResumen publicacion);
    }

    private List<FavoritoResponseDto> lista;
    private final OnItemClickListener listener;

    public FavoritosAdapter(List<FavoritoResponseDto> lista, OnItemClickListener listener) {
        this.lista = lista != null ? lista : new ArrayList<>();
        this.listener = listener;
    }

    public void actualizarLista(List<FavoritoResponseDto> nueva) {
        this.lista = nueva != null ? new ArrayList<>(nueva) : new ArrayList<>();
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
        FavoritoResponseDto favorito = lista.get(position);
        PublicacionResumen p = favorito.publicacion;

        holder.tvTitulo.setText(p != null && p.titulo != null ? p.titulo : "(sin titulo)");
        holder.tvPrecio.setText(p != null && p.precio != null
                ? String.format(Locale.getDefault(), "$ %.2f", p.precio)
                : "");
        holder.tvEstadoArticulo.setText(p != null && p.estadoArticulo != null ? p.estadoArticulo : "");
        holder.tvZona.setText(p != null && p.zonaEntrega != null ? "Zona: " + p.zonaEntrega : "");
        holder.tvVendedor.setText(p != null && p.vendedorNombre != null ? "Vendedor: " + p.vendedorNombre : "");

        holder.tvCambioPrecio.setVisibility(favorito.cambioPrecio ? View.VISIBLE : View.GONE);

        holder.ivFoto.setImageDrawable(null);
        holder.ivFoto.setTag(null);
        ImageLoader.cargar(p != null ? p.fotoPrincipal : null, holder.ivFoto);

        holder.itemView.setOnClickListener(v -> {
            if (p != null) listener.onClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvTitulo, tvPrecio, tvEstadoArticulo, tvZona, tvVendedor, tvCambioPrecio;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstadoArticulo = itemView.findViewById(R.id.tvEstadoArticulo);
            tvZona = itemView.findViewById(R.id.tvZona);
            tvVendedor = itemView.findViewById(R.id.tvVendedor);
            tvCambioPrecio = itemView.findViewById(R.id.tvCambioPrecio);
        }
    }
}
