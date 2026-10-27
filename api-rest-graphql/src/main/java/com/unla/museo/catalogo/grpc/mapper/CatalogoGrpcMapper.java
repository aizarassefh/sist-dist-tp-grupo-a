package com.unla.museo.catalogo.grpc.mapper;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.grpc.Artista;
import com.unla.museo.catalogo.grpc.Obra;

public final class CatalogoGrpcMapper {

    private CatalogoGrpcMapper() {
    }

    // protobuf no admite null: los textos nulos viajan como "" y los enteros
    // nulos como 0 (el contrato documenta "0 si no se conoce").
    public static Obra aObra(ObraEntity obra) {
        return Obra.newBuilder()
                .setId(obra.getId())
                .setTitulo(texto(obra.getTitulo()))
                .setDescripcion(texto(obra.getDescripcion()))
                .setArtista(aArtista(obra.getArtista(), 0))
                .setAnioCreacion(entero(obra.getAnioCreacion()))
                .setTecnica(texto(obra.getTecnica()))
                .setTematica(texto(obra.getTematica()))
                .setMovimiento(texto(obra.getEpoca()))
                .setDimensiones(texto(obra.getDimensiones()))
                .setUbicacion(texto(obra.getUbicacion()))
                .setEnExhibicion(obra.isEnExhibicion())
                .setImagenUrl(texto(obra.getImagenUrl()))
                .build();
    }

    public static Artista aArtista(ArtistaEntity artista, int cantidadObras) {
        return Artista.newBuilder()
                .setId(artista.getId())
                .setNombre(texto(artista.getNombre()))
                .setBiografia(texto(artista.getBiografia()))
                .setNacionalidad(texto(artista.getNacionalidad()))
                .setMovimiento(texto(artista.getMovimiento()))
                .setAnioNacimiento(entero(artista.getAnioNacimiento()))
                .setAnioFallecimiento(entero(artista.getAnioFallecimiento()))
                .setCantidadObras(cantidadObras)
                .build();
    }

    private static String texto(String valor) {
        return valor != null ? valor : "";
    }

    private static int entero(Integer valor) {
        return valor != null ? valor : 0;
    }
}
