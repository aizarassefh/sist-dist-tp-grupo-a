package com.unla.museo.catalogo.config;

import com.unla.museo.catalogo.entity.ArtistaEntity;
import com.unla.museo.catalogo.entity.ComentarioEntity;
import com.unla.museo.catalogo.entity.ObraEntity;
import com.unla.museo.catalogo.repository.ArtistaRepository;
import com.unla.museo.catalogo.repository.ComentarioRepository;
import com.unla.museo.catalogo.repository.ObraRepository;
import com.unla.museo.seguridad.entity.UsuarioEntity;
import com.unla.museo.seguridad.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Las mismas 9 obras que antes vivían escritas a mano en ObraData, ahora como
 * datos de ejemplo en base. Solo corre si la tabla de obras está vacía, y
 * solo con app.datos-de-ejemplo=true. @Order(4): después de los usuarios (2)
 * y de los eventos (3), para poder asignar los comentarios a visitantes que
 * ya existen.
 */
@Configuration
public class CatalogoDataInitializer {

    private record DatoComentario(String texto, String fecha) {
    }

    private record DatoObra(String titulo, String artista, String biografiaArtista, String imagenUrl,
                             int anioCreacion, String tecnica, String dimensiones, String epoca, String descripcion,
                             String ubicacion, boolean enExhibicion, List<DatoComentario> comentarios) {
    }

    // Mismos textos, autores ficticios reemplazados por visitantes de ejemplo
    // reales (asignados en orden de aparición) y mismas fechas que ObraData.
    private static final List<DatoObra> OBRAS = List.of(
            new DatoObra("La Gioconda", "Leonardo da Vinci",
                    "Pintor, inventor y científico del Renacimiento italiano.", "/obras/gioconda.jpg", 1503,
                    "Óleo sobre tabla", "77 cm x 53 cm", "Renacimiento",
                    "Retrato de una mujer realizado durante el Renacimiento italiano.", "Sala de Arte Europeo", true,
                    List.of(new DatoComentario("Una de las obras más reconocidas del museo.", "2026-09-01"),
                            new DatoComentario("La expresión es fascinante.", "2026-09-03"))),
            new DatoObra("La noche estrellada", "Vincent van Gogh",
                    "Pintor neerlandés asociado al postimpresionismo.", "/obras/noche-estrellada.jpg", 1889,
                    "Óleo sobre lienzo", "73,7 cm x 92,1 cm", "Postimpresionismo",
                    "Paisaje nocturno representado desde la ventana del sanatorio de Saint-Rémy.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("Los colores destacan muchísimo.", "2026-09-05"))),
            new DatoObra("El grito", "Edvard Munch", "Pintor noruego vinculado al expresionismo.",
                    "/obras/el-grito.jpg", 1893, "Óleo y pastel sobre cartón", "91 cm x 73,5 cm", "Expresionismo",
                    "Obra que representa una figura en un paisaje atravesado por una intensa sensación de angustia.",
                    "Depósito de conservación", false, List.of()),
            new DatoObra("La persistencia de la memoria", "Salvador Dalí",
                    "Artista español y una de las figuras del surrealismo.", "/obras/persistencia-memoria.jpg", 1931,
                    "Óleo sobre lienzo", "24 cm x 33 cm", "Surrealismo",
                    "Paisaje onírico donde aparecen relojes blandos sobre un espacio desértico.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("Los relojes son el elemento que más llama la atención.", "2026-09-07"),
                            new DatoComentario("Una obra muy interesante para analizar.", "2026-09-08"))),
            new DatoObra("Las meninas", "Diego Velázquez", "Pintor español del período barroco.",
                    "/obras/las-meninas.jpg", 1656, "Óleo sobre lienzo", "318 cm x 276 cm", "Barroco",
                    "Escena cortesana que representa a la infanta Margarita y su séquito.", "Sala de Arte Español",
                    true, List.of(new DatoComentario("La composición tiene muchos detalles.", "2026-09-09"))),
            new DatoObra("El nacimiento de Venus", "Sandro Botticelli",
                    "Pintor italiano del Renacimiento florentino.", "/obras/nacimiento-venus.jpg", 1485,
                    "Temple sobre lienzo", "172,5 cm x 278,5 cm", "Renacimiento",
                    "Representación de Venus emergiendo del mar sobre una concha.", "Sala de Arte Europeo", true,
                    List.of()),
            new DatoObra("Guernica", "Pablo Picasso",
                    "Artista español y una de las figuras centrales del arte del siglo XX.", "/obras/guernica.jpg",
                    1937, "Óleo sobre lienzo", "349,3 cm x 776,6 cm", "Cubismo",
                    "Pintura monumental relacionada con el bombardeo de Guernica durante la Guerra Civil Española.",
                    "Sala de Arte Contemporáneo", false,
                    List.of(new DatoComentario("La composición transmite una escena muy intensa.", "2026-09-10"))),
            new DatoObra("El beso", "Gustav Klimt",
                    "Pintor austríaco asociado al modernismo y la Secesión de Viena.", "/obras/el-beso.jpg", 1908,
                    "Óleo y pan de oro sobre lienzo", "180 cm x 180 cm", "Modernismo",
                    "Representación de una pareja abrazada dentro de una composición decorativa.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("El uso del dorado es muy característico.", "2026-09-11"))),
            new DatoObra("La joven de la perla", "Johannes Vermeer", "Pintor neerlandés del período barroco.",
                    "/obras/joven-perla.jpg", 1665, "Óleo sobre lienzo", "44,5 cm x 39 cm", "Barroco",
                    "Retrato de una joven con un pendiente de perla.", "Sala de Arte Europeo", true, List.of())
    );

    private static final List<String> EMAILS_VISITANTES = List.of(
            "visitante@test.com", "maria.gonzalez@test.com", "lucia.fernandez@test.com",
            "martin.rodriguez@test.com", "sofia.lopez@test.com", "nicolas.diaz@test.com",
            "valentina.martinez@test.com", "federico.sanchez@test.com", "camila.romero@test.com"
    );

    @Bean
    @Order(4)
    @ConditionalOnProperty(name = "app.datos-de-ejemplo", havingValue = "true", matchIfMissing = true)
    CommandLineRunner initObrasDeEjemplo(
            ObraRepository obraRepository,
            ArtistaRepository artistaRepository,
            ComentarioRepository comentarioRepository,
            @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository) {

        return args -> {
            if (obraRepository.count() > 0) {
                return;
            }

            List<UsuarioEntity> visitantes = EMAILS_VISITANTES.stream()
                    .map(email -> usuarioRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalStateException("Falta el usuario " + email)))
                    .toList();

            // Ronda entre los visitantes de ejemplo para repartir la autoría de
            // los comentarios: no hay forma de saber a qué usuario real
            // correspondía cada nombre ficticio de ObraData.
            int indiceComentario = 0;
            for (DatoObra dato : OBRAS) {
                ArtistaEntity artistaEntity = new ArtistaEntity();
                artistaEntity.setNombre(dato.artista());
                artistaEntity.setBiografia(dato.biografiaArtista());
                artistaEntity = artistaRepository.save(artistaEntity);

                ObraEntity obraEntity = new ObraEntity();
                obraEntity.setTitulo(dato.titulo());
                obraEntity.setDescripcion(dato.descripcion());
                obraEntity.setArtista(artistaEntity);
                obraEntity.setImagenUrl(dato.imagenUrl());
                obraEntity.setAnioCreacion(dato.anioCreacion());
                obraEntity.setTecnica(dato.tecnica());
                obraEntity.setDimensiones(dato.dimensiones());
                obraEntity.setEpoca(dato.epoca());
                obraEntity.setUbicacion(dato.ubicacion());
                obraEntity.setEnExhibicion(dato.enExhibicion());
                obraEntity = obraRepository.save(obraEntity);

                for (DatoComentario datoComentario : dato.comentarios()) {
                    ComentarioEntity comentarioEntity = new ComentarioEntity();
                    comentarioEntity.setObra(obraEntity);
                    comentarioEntity.setUsuario(visitantes.get(indiceComentario % visitantes.size()));
                    comentarioEntity.setTexto(datoComentario.texto());
                    comentarioEntity.setFecha(LocalDateTime.parse(datoComentario.fecha() + "T00:00:00"));
                    comentarioRepository.save(comentarioEntity);
                    indiceComentario++;
                }
            }
        };
    }
}
