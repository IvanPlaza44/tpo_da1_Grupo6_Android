package com.example.myapplication.ui.busquedas;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.BusquedaGuardadaResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class BusquedasGuardadasAdapter extends RecyclerView.Adapter<BusquedasGuardadasAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onClick(BusquedaGuardadaResponseDto busqueda);
        void onEliminar(BusquedaGuardadaResponseDto busqueda);
    }

    private List<BusquedaGuardadaResponseDto> lista;
    private final OnItemClickListener listener;

    public BusquedasGuardadasAdapter(List<BusquedaGuardadaResponseDto> lista, OnItemClickListener listener) {
        this.lista = lista != null ? lista : new ArrayList<>();
        this.listener = listener;
    }

    public void actualizarLista(List<BusquedaGuardadaResponseDto> nueva) {
        this.lista = nueva != null ? new ArrayList<>(nueva) : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void eliminarPorId(long id) {
        for (int i = 0; i < lista.size(); i++) {
            if (lista.get(i).id == id) {
                lista.remove(i);
                notifyItemRemoved(i);
                return;
            }
        }
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_busqueda_guardada, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        BusquedaGuardadaResponseDto item = lista.get(position);
        holder.tvNombre.setText(item.nombre != null ? item.nombre : "(sin nombre)");
        bindResumen(holder, item);
        holder.tvHayNovedades.setVisibility(item.hayNovedades ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(item));
    }

    private static void bindResumen(ViewHolder holder, BusquedaGuardadaResponseDto item) {
        List<String> partes = new ArrayList<>();
        if (item.query != null && !item.query.trim().isEmpty()) {
            partes.add(item.query.trim());
        }
        if (item.estadoArticulo != null && !item.estadoArticulo.trim().isEmpty()) {
            partes.add(item.estadoArticulo.trim());
        }
        if (item.zona != null && !item.zona.trim().isEmpty()) {
            partes.add(item.zona.trim());
        }

        if (partes.isEmpty()) {
            holder.tvResumenCriterios.setVisibility(View.GONE);
        } else {
            holder.tvResumenCriterios.setVisibility(View.VISIBLE);
            holder.tvResumenCriterios.setText(TextUtils.join(" · ", partes));
        }

        String lineaPrecio = formatearPrecio(item);
        if (lineaPrecio == null) {
            holder.tvResumenPrecio.setVisibility(View.GONE);
        } else {
            holder.tvResumenPrecio.setVisibility(View.VISIBLE);
            holder.tvResumenPrecio.setText(lineaPrecio);
        }
    }

    private static String formatearPrecio(BusquedaGuardadaResponseDto item) {
        Locale locale = Locale.getDefault();
        if (item.precioMin != null && item.precioMax != null) {
            return String.format(locale, "Desde $%,.2f · Hasta $%,.2f", item.precioMin, item.precioMax);
        }
        if (item.precioMax != null) {
            return String.format(locale, "Hasta $%,.2f", item.precioMax);
        }
        if (item.precioMin != null) {
            return String.format(locale, "Desde $%,.2f", item.precioMin);
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvResumenCriterios;
        TextView tvResumenPrecio;
        TextView tvHayNovedades;
        Button btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvResumenCriterios = itemView.findViewById(R.id.tvResumenCriterios);
            tvResumenPrecio = itemView.findViewById(R.id.tvResumenPrecio);
            tvHayNovedades = itemView.findViewById(R.id.tvHayNovedades);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
