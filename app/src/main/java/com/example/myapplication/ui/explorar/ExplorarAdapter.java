package com.example.myapplication.ui.explorar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.util.ImageLoader;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ExplorarAdapter extends RecyclerView.Adapter<ExplorarAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(PublicacionResumen publicacion);
    }

    public interface OnFavoritoClickListener {
        void onToggle(PublicacionResumen publicacion, boolean esFavorito);
    }

    private List<PublicacionResumen> lista;
    private final OnItemClickListener listener;
    private final OnFavoritoClickListener favoritoListener;
    private Set<Long> favoritoIds = new HashSet<>();
    private Set<Long> publicacionPropiaIds = new HashSet<>();
    private boolean favoritosHabilitados;

    public ExplorarAdapter(List<PublicacionResumen> lista, OnItemClickListener listener) {
        this(lista, listener, null);
    }

    public ExplorarAdapter(List<PublicacionResumen> lista,
                           OnItemClickListener listener,
                           @Nullable OnFavoritoClickListener favoritoListener) {
        this.lista = lista;
        this.listener = listener;
        this.favoritoListener = favoritoListener;
    }

    public void actualizarEstadoFavoritos(@Nullable Set<Long> favoritos,
                                          @Nullable Set<Long> propias,
                                          boolean habilitados) {
        this.favoritoIds = favoritos != null ? favoritos : new HashSet<>();
        this.publicacionPropiaIds = propias != null ? propias : new HashSet<>();
        this.favoritosHabilitados = habilitados;
        notifyDataSetChanged();
    }

    public void actualizarLista(List<PublicacionResumen> nueva) {
        this.lista = nueva != null ? new ArrayList<>(nueva) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void agregarItems(List<PublicacionResumen> nuevos) {
        if (nuevos == null || nuevos.isEmpty()) return;
        int desde = lista.size();
        lista.addAll(nuevos);
        notifyItemRangeInserted(desde, nuevos.size());
    }

    public int indicePorId(long publicacionId) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id == publicacionId) {
                return i;
            }
        }
        return -1;
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
        holder.tvPrecio.setText(p.precio != null
                ? String.format(Locale.getDefault(), "$ %.2f", p.precio)
                : "");
        holder.tvEstadoArticulo.setText(p.estadoArticulo != null ? p.estadoArticulo : "");
        holder.tvZona.setText(p.zonaEntrega != null ? "Zona: " + p.zonaEntrega : "");
        holder.tvVendedor.setText(p.vendedorNombre != null ? "Vendedor: " + p.vendedorNombre : "");

        holder.ivFoto.setImageDrawable(null);
        holder.ivFoto.setTag(null);
        ImageLoader.cargar(p.fotoPrincipal, holder.ivFoto);

        boolean esPropia = publicacionPropiaIds.contains(p.id);
        if (!favoritosHabilitados || esPropia) {
            holder.btnFavorito.setVisibility(View.GONE);
            holder.btnFavorito.setOnClickListener(null);
        } else {
            boolean esFavorito = favoritoIds.contains(p.id);
            holder.btnFavorito.setVisibility(View.VISIBLE);
            holder.btnFavorito.setImageResource(esFavorito
                    ? R.drawable.ic_favorito_lleno
                    : R.drawable.ic_favorito_borde);
            holder.btnFavorito.setContentDescription(esFavorito
                    ? "Quitar de favoritos"
                    : "Agregar a favoritos");
            holder.btnFavorito.setOnClickListener(v -> {
                if (favoritoListener != null) {
                    favoritoListener.onToggle(p, esFavorito);
                }
            });
        }

        holder.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        ImageButton btnFavorito;
        TextView tvTitulo, tvPrecio, tvEstadoArticulo, tvZona, tvVendedor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            btnFavorito = itemView.findViewById(R.id.btnFavorito);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstadoArticulo = itemView.findViewById(R.id.tvEstadoArticulo);
            tvZona = itemView.findViewById(R.id.tvZona);
            tvVendedor = itemView.findViewById(R.id.tvVendedor);
        }
    }
}
