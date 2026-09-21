package com.example.myapplication.ui.explorar;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PublicacionResumen;
import com.example.myapplication.util.ImageLoader;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
        this.lista = nueva != null ? new ArrayList<>(nueva) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void agregarItems(List<PublicacionResumen> nuevos) {
        if (nuevos == null || nuevos.isEmpty()) return;
        int desde = lista.size();
        lista.addAll(nuevos);
        notifyItemRangeInserted(desde, nuevos.size());
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

        // ImageLoader no limpia la vista si la URL es vacia; hay que resetear
        // para que el RecyclerView no recicle la foto de otro item.
        holder.ivFoto.setImageDrawable(null);
        holder.ivFoto.setTag(null);
        ImageLoader.cargar(p.fotoPrincipal, holder.ivFoto);

        holder.itemView.setOnClickListener(v -> listener.onClick(p));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivFoto;
        TextView tvTitulo, tvPrecio, tvEstadoArticulo, tvZona, tvVendedor;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivFoto = itemView.findViewById(R.id.ivFoto);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvPrecio = itemView.findViewById(R.id.tvPrecio);
            tvEstadoArticulo = itemView.findViewById(R.id.tvEstadoArticulo);
            tvZona = itemView.findViewById(R.id.tvZona);
            tvVendedor = itemView.findViewById(R.id.tvVendedor);
        }
    }
}
