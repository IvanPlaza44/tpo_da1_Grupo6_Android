package com.example.myapplication.ui.historial;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.OperacionResponseDto;

import java.util.List;
import java.util.Locale;

public class OperacionAdapter extends RecyclerView.Adapter<OperacionAdapter.OperacionViewHolder> {

    public interface OnCalificarListener {
        void onCalificar(OperacionResponseDto operacion);
    }

    private final List<OperacionResponseDto> operaciones;
    private final OnCalificarListener listener;

    public OperacionAdapter(List<OperacionResponseDto> operaciones, OnCalificarListener listener) {
        this.operaciones = operaciones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public OperacionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_operacion, parent, false);
        return new OperacionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OperacionViewHolder holder, int position) {
        OperacionResponseDto op = operaciones.get(position);

        boolean esCompra = "COMPRA".equals(op.tipo);
        holder.tvTipo.setText(esCompra ? "COMPRA" : "VENTA");
        holder.tvArticulo.setText(op.publicacionTitulo);
        holder.tvMonto.setText(String.format(Locale.getDefault(), "$ %.2f", op.montoFinal));

        String contraparte = esCompra ? op.vendedorNombre : op.compradorNombre;
        holder.tvContraparte.setText((esCompra ? "Vendedor: " : "Comprador: ") + contraparte);

        String fecha = op.fechaEntrega != null ? op.fechaEntrega : op.fechaAcordada;
        holder.tvFecha.setText(fecha != null && fecha.length() >= 10 ? fecha.substring(0, 10) : "");

        if (op.puedeCalificar) {
            holder.btnCalificar.setVisibility(View.VISIBLE);
            holder.btnCalificar.setOnClickListener(v -> listener.onCalificar(op));
        } else {
            holder.btnCalificar.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return operaciones.size();
    }

    static class OperacionViewHolder extends RecyclerView.ViewHolder {
        TextView tvTipo, tvArticulo, tvMonto, tvContraparte, tvFecha;
        Button btnCalificar;

        OperacionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTipo = itemView.findViewById(R.id.tvTipo);
            tvArticulo = itemView.findViewById(R.id.tvArticulo);
            tvMonto = itemView.findViewById(R.id.tvMonto);
            tvContraparte = itemView.findViewById(R.id.tvContraparte);
            tvFecha = itemView.findViewById(R.id.tvFecha);
            btnCalificar = itemView.findViewById(R.id.btnCalificar);
        }
    }
}