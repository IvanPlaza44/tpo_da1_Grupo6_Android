package com.example.myapplication.session;

import androidx.annotation.Nullable;

import com.example.myapplication.model.BusquedaGuardadaResponseDto;

/**
 * Criterios de Explorar a aplicar en la próxima apertura del fragment (desde Búsquedas guardadas).
 */
public final class ExplorarCriteriosPendiente {

    private static boolean activo;
    private static String query;
    private static Long categoriaId;
    private static Double precioMin;
    private static Double precioMax;
    private static String estadoArticulo;
    private static String zona;

    private ExplorarCriteriosPendiente() {
    }

    public static void establecerDesde(@Nullable BusquedaGuardadaResponseDto busqueda) {
        if (busqueda == null) {
            limpiar();
            return;
        }
        activo = true;
        query = busqueda.queryParaExplorar();
        categoriaId = busqueda.categoriaId;
        precioMin = busqueda.precioMin;
        precioMax = busqueda.precioMax;
        estadoArticulo = busqueda.estadoArticulo;
        zona = busqueda.zona;
    }

    public static boolean hayPendiente() {
        return activo;
    }

    @Nullable
    public static String getQuery() {
        return query;
    }

    @Nullable
    public static Long getCategoriaId() {
        return categoriaId;
    }

    @Nullable
    public static Double getPrecioMin() {
        return precioMin;
    }

    @Nullable
    public static Double getPrecioMax() {
        return precioMax;
    }

    @Nullable
    public static String getEstadoArticulo() {
        return estadoArticulo;
    }

    @Nullable
    public static String getZona() {
        return zona;
    }

    public static void limpiar() {
        activo = false;
        query = null;
        categoriaId = null;
        precioMin = null;
        precioMax = null;
        estadoArticulo = null;
        zona = null;
    }
}
