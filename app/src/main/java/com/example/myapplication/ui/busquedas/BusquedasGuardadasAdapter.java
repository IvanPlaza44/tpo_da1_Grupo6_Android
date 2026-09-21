package com.example.myapplication.ui.busquedas;

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
        holder.tvHayNovedades.setVisibility(item.hayNovedades ? View.VISIBLE : View.GONE);
        holder.itemView.setOnClickListener(v -> listener.onClick(item));
        holder.btnEliminar.setOnClickListener(v -> listener.onEliminar(item));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre;
        TextView tvHayNovedades;
        Button btnEliminar;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvHayNovedades = itemView.findViewById(R.id.tvHayNovedades);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }
    }
}
