package com.example.myapplication.model;

import com.google.gson.annotations.SerializedName;

public class BusquedaGuardadaResponseDto {
    public long id;
    public String nombre;
    public boolean hayNovedades;
    @SerializedName("query")
    public String query;
    public Long categoriaId;
    public Double precioMin;
    public Double precioMax;
    public String estadoArticulo;
    public String zona;

    /** Texto de búsqueda para GET /api/publicaciones (incluye búsquedas viejas sin `query` en BD). */
    public String queryParaExplorar() {
        String q = trimToNull(query);
        if (q != null) {
            return q;
        }
        if (!tieneFiltrosAdemasDeTexto()) {
            return trimToNull(nombre);
        }
        return null;
    }

    public boolean tieneCriteriosAplicables() {
        return queryParaExplorar() != null || tieneFiltrosAdemasDeTexto();
    }

    private boolean tieneFiltrosAdemasDeTexto() {
        return categoriaId != null
                || precioMin != null
                || precioMax != null
                || trimToNull(estadoArticulo) != null
                || trimToNull(zona) != null;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String t = value.trim();
        return t.isEmpty() ? null : t;
    }
}
