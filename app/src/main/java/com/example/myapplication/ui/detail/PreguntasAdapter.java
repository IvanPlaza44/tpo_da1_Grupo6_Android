package com.example.myapplication.ui.detail;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.model.Publicacion.PreguntaResponseDto;

import java.util.List;

public class PreguntasAdapter extends RecyclerView.Adapter<PreguntasAdapter.PreguntaViewHolder> {

    public interface OnResponderListener {
        void onResponder(PreguntaResponseDto pregunta, String respuesta);
    }

    private final List<PreguntaResponseDto> preguntas;
    private final boolean esVendedor;
    private final OnResponderListener listener;

    public PreguntasAdapter(List<PreguntaResponseDto> preguntas, boolean esVendedor, OnResponderListener listener) {
        this.preguntas = preguntas;
        this.esVendedor = esVendedor;
        this.listener = listener;
    }

    @NonNull
    @Override
    public PreguntaViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pregunta, parent, false);
        return new PreguntaViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreguntaViewHolder holder, int position) {
        PreguntaResponseDto pregunta = preguntas.get(position);

        holder.tvAutorNombre.setText(pregunta.autorNombre);
        holder.tvMensaje.setText(pregunta.mensaje);

        if (pregunta.tieneRespuesta()) {
            holder.tvRespuesta.setText("Respuesta: " + pregunta.respuesta);
            holder.tvRespuesta.setVisibility(View.VISIBLE);
            holder.layoutResponder.setVisibility(View.GONE);
        } else if (esVendedor) {
            holder.tvRespuesta.setVisibility(View.GONE);
            holder.layoutResponder.setVisibility(View.VISIBLE);
            holder.btnResponder.setOnClickListener(v -> {
                String texto = holder.etRespuesta.getText().toString().trim();
                if (!texto.isEmpty() && listener != null) {
                    listener.onResponder(pregunta, texto);
                }
            });
        } else {
            holder.tvRespuesta.setVisibility(View.GONE);
            holder.layoutResponder.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return preguntas.size();
    }

    static class PreguntaViewHolder extends RecyclerView.ViewHolder {
        TextView tvAutorNombre, tvMensaje, tvRespuesta;
        LinearLayout layoutResponder;
        EditText etRespuesta;
        Button btnResponder;

        PreguntaViewHolder(@NonNull View itemView) {
            super(itemView);
            tvAutorNombre = itemView.findViewById(R.id.tvAutorNombre);
            tvMensaje = itemView.findViewById(R.id.tvMensaje);
            tvRespuesta = itemView.findViewById(R.id.tvRespuesta);
            layoutResponder = itemView.findViewById(R.id.layoutResponder);
            etRespuesta = itemView.findViewById(R.id.etRespuesta);
            btnResponder = itemView.findViewById(R.id.btnResponder);
        }
    }
}