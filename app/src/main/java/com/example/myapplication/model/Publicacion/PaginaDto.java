package com.example.myapplication.model.Publicacion;

import java.util.List;

public class PaginaDto<T> {
    public List<T> contenido;
    public int pagina;
    public int tamanio;
    public long totalElementos;
    public int totalPaginas;
}