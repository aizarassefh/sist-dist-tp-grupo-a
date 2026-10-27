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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Obras de ejemplo (las 9 originales de ObraData más otras 9, ampliadas, para que
 * cada movimiento tenga 2 o 3). Corre solo con app.datos-de-ejemplo=true. Se
 * resuelve obra por obra, por título: en una base que ya tenía las 9 originales
 * completa los campos nuevos que estén en null (temática, datos del artista)
 * y crea solo las que faltan. @Order(4): después de los usuarios (2) y de los
 * eventos (3), para poder asignar los comentarios a visitantes que ya existen.
 */
@Configuration
public class CatalogoDataInitializer {

    private record DatoComentario(String texto, String fecha) {
    }

    private record DatoArtista(String nombre, String biografia, String nacionalidad, String movimiento,
                                int anioNacimiento, int anioFallecimiento) {
    }

    private record DatoObra(String titulo, DatoArtista artista, String imagenUrl,
                             int anioCreacion, String tecnica, String dimensiones, String epoca, String tematica,
                             String descripcion, String ubicacion, boolean enExhibicion,
                             List<DatoComentario> comentarios) {
    }

    private static final DatoArtista LEONARDO = new DatoArtista("Leonardo da Vinci",
            "Pintor, inventor y científico del Renacimiento italiano.", "Italiana", "Renacimiento", 1452, 1519);
    private static final DatoArtista VAN_GOGH = new DatoArtista("Vincent van Gogh",
            "Pintor neerlandés asociado al postimpresionismo.", "Neerlandesa", "Postimpresionismo", 1853, 1890);
    private static final DatoArtista MUNCH = new DatoArtista("Edvard Munch",
            "Pintor noruego vinculado al expresionismo.", "Noruega", "Expresionismo", 1863, 1944);
    private static final DatoArtista DALI = new DatoArtista("Salvador Dalí",
            "Artista español y una de las figuras del surrealismo.", "Española", "Surrealismo", 1904, 1989);
    private static final DatoArtista VELAZQUEZ = new DatoArtista("Diego Velázquez",
            "Pintor español del período barroco.", "Española", "Barroco", 1599, 1660);
    private static final DatoArtista BOTTICELLI = new DatoArtista("Sandro Botticelli",
            "Pintor italiano del Renacimiento florentino.", "Italiana", "Renacimiento", 1445, 1510);
    private static final DatoArtista PICASSO = new DatoArtista("Pablo Picasso",
            "Artista español y una de las figuras centrales del arte del siglo XX.", "Española", "Cubismo", 1881, 1973);
    private static final DatoArtista KLIMT = new DatoArtista("Gustav Klimt",
            "Pintor austríaco asociado al modernismo y la Secesión de Viena.", "Austríaca", "Modernismo", 1862, 1918);
    private static final DatoArtista VERMEER = new DatoArtista("Johannes Vermeer",
            "Pintor neerlandés del período barroco.", "Neerlandesa", "Barroco", 1632, 1675);
    private static final DatoArtista MONET = new DatoArtista("Claude Monet",
            "Pintor francés, fundador del impresionismo.", "Francesa", "Impresionismo", 1840, 1926);
    private static final DatoArtista RENOIR = new DatoArtista("Pierre-Auguste Renoir",
            "Pintor francés del impresionismo, conocido por sus escenas de la vida parisina.", "Francesa",
            "Impresionismo", 1841, 1919);
    private static final DatoArtista DEGAS = new DatoArtista("Edgar Degas",
            "Pintor y escultor francés del impresionismo, célebre por sus bailarinas.", "Francesa", "Impresionismo",
            1834, 1917);
    private static final DatoArtista CARAVAGGIO = new DatoArtista("Caravaggio",
            "Pintor italiano del barroco, maestro del claroscuro.", "Italiana", "Barroco", 1571, 1610);
    private static final DatoArtista KIRCHNER = new DatoArtista("Ernst Ludwig Kirchner",
            "Pintor alemán, cofundador del grupo expresionista Die Brücke.", "Alemana", "Expresionismo", 1880, 1938);
    private static final DatoArtista MAGRITTE = new DatoArtista("René Magritte",
            "Pintor belga del surrealismo, conocido por sus imágenes enigmáticas.", "Belga", "Surrealismo", 1898,
            1967);

    // Mismos textos, autores ficticios reemplazados por visitantes de ejemplo
    // reales (asignados en orden de aparición) y mismas fechas que ObraData.
    private static final List<DatoObra> OBRAS = List.of(
            new DatoObra("La Gioconda", LEONARDO, "/obras/gioconda.jpg", 1503,
                    "Óleo sobre tabla", "77 cm x 53 cm", "Renacimiento", "Retrato",
                    "Retrato de una mujer realizado durante el Renacimiento italiano.", "Sala de Arte Europeo", true,
                    List.of(new DatoComentario("Una de las obras más reconocidas del museo.", "2026-09-01"),
                            new DatoComentario("La expresión es fascinante.", "2026-09-03"))),
            new DatoObra("La noche estrellada", VAN_GOGH, "/obras/noche-estrellada.jpg", 1889,
                    "Óleo sobre lienzo", "73,7 cm x 92,1 cm", "Postimpresionismo", "Paisaje",
                    "Paisaje nocturno representado desde la ventana del sanatorio de Saint-Rémy.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("Los colores destacan muchísimo.", "2026-09-05"))),
            new DatoObra("El grito", MUNCH, "/obras/el-grito.jpg", 1893,
                    "Óleo y pastel sobre cartón", "91 cm x 73,5 cm", "Expresionismo", "Paisaje",
                    "Obra que representa una figura en un paisaje atravesado por una intensa sensación de angustia.",
                    "Depósito de conservación", false, List.of()),
            new DatoObra("La persistencia de la memoria", DALI, "/obras/persistencia-memoria.jpg", 1931,
                    "Óleo sobre lienzo", "24 cm x 33 cm", "Surrealismo", "Fantasía",
                    "Paisaje onírico donde aparecen relojes blandos sobre un espacio desértico.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("Los relojes son el elemento que más llama la atención.", "2026-09-07"),
                            new DatoComentario("Una obra muy interesante para analizar.", "2026-09-08"))),
            new DatoObra("Las meninas", VELAZQUEZ, "/obras/las-meninas.jpg", 1656,
                    "Óleo sobre lienzo", "318 cm x 276 cm", "Barroco", "Escena cotidiana",
                    "Escena cortesana que representa a la infanta Margarita y su séquito.", "Sala de Arte Español",
                    true, List.of(new DatoComentario("La composición tiene muchos detalles.", "2026-09-09"))),
            new DatoObra("El nacimiento de Venus", BOTTICELLI, "/obras/nacimiento-venus.jpg", 1485,
                    "Temple sobre lienzo", "172,5 cm x 278,5 cm", "Renacimiento", "Mitología",
                    "Representación de Venus emergiendo del mar sobre una concha.", "Sala de Arte Europeo", true,
                    List.of()),
            new DatoObra("Guernica", PICASSO, "/obras/guernica.jpg", 1937,
                    "Óleo sobre lienzo", "349,3 cm x 776,6 cm", "Cubismo", "Historia",
                    "Pintura monumental relacionada con el bombardeo de Guernica durante la Guerra Civil Española.",
                    "Sala de Arte Contemporáneo", false,
                    List.of(new DatoComentario("La composición transmite una escena muy intensa.", "2026-09-10"))),
            new DatoObra("El beso", KLIMT, "/obras/el-beso.jpg", 1908,
                    "Óleo y pan de oro sobre lienzo", "180 cm x 180 cm", "Modernismo", "Escena cotidiana",
                    "Representación de una pareja abrazada dentro de una composición decorativa.",
                    "Sala de Arte Moderno", true,
                    List.of(new DatoComentario("El uso del dorado es muy característico.", "2026-09-11"))),
            new DatoObra("La joven de la perla", VERMEER, "/obras/joven-perla.jpg", 1665,
                    "Óleo sobre lienzo", "44,5 cm x 39 cm", "Barroco", "Retrato",
                    "Retrato de una joven con un pendiente de perla.", "Sala de Arte Europeo", true, List.of())
    );

    // Obras agregadas para la Entrega 2, sin imagen (el front muestra el marco
    // "Imagen no disponible"). Se cargan aparte porque los tests de la Entrega 1
    // asumen exactamente las 9 originales: app.datos-de-ejemplo-ampliados=false
    // en el perfil test las deja afuera.
    private static final List<DatoObra> OBRAS_AMPLIADAS = List.of(
            new DatoObra("Impresión, sol naciente", MONET, null, 1872,
                    "Óleo sobre lienzo", "48 cm x 63 cm", "Impresionismo", "Paisaje",
                    "Vista del puerto de Le Havre al amanecer que dio nombre al impresionismo.",
                    "Sala de Arte Moderno", true, List.of()),
            new DatoObra("Baile en el Moulin de la Galette", RENOIR, null, 1876,
                    "Óleo sobre lienzo", "131 cm x 175 cm", "Impresionismo", "Escena cotidiana",
                    "Fiesta al aire libre en una sala de baile de Montmartre, con juegos de luz entre los árboles.",
                    "Sala de Arte Europeo", true, List.of()),
            new DatoObra("La clase de danza", DEGAS, null, 1874,
                    "Óleo sobre lienzo", "85 cm x 75 cm", "Impresionismo", "Escena cotidiana",
                    "Bailarinas de la Ópera de París durante un ensayo con su maestro de danza.",
                    "Sala de Arte Europeo", true, List.of()),
            new DatoObra("Los girasoles", VAN_GOGH, null, 1888,
                    "Óleo sobre lienzo", "92,1 cm x 73 cm", "Postimpresionismo", "Naturaleza muerta",
                    "Jarrón con girasoles pintado en Arlés para decorar la habitación de Paul Gauguin.",
                    "Sala de Arte Moderno", true, List.of()),
            new DatoObra("La última cena", LEONARDO, null, 1498,
                    "Temple y óleo sobre yeso", "460 cm x 880 cm", "Renacimiento", "Religión",
                    "Mural del refectorio de Santa Maria delle Grazie, en Milán, con el anuncio de la traición.",
                    "Sala de Arte Europeo", false, List.of()),
            new DatoObra("La vocación de San Mateo", CARAVAGGIO, null, 1600,
                    "Óleo sobre lienzo", "322 cm x 340 cm", "Barroco", "Religión",
                    "Cristo llama a Mateo desde la penumbra de una taberna, en un fuerte contraste de luz y sombra.",
                    "Sala de Arte Europeo", true, List.of()),
            new DatoObra("Escena de calle, Berlín", KIRCHNER, null, 1913,
                    "Óleo sobre lienzo", "120,6 cm x 91,1 cm", "Expresionismo", "Escena cotidiana",
                    "Dos mujeres en una calle berlinesa, con figuras angulosas y colores estridentes.",
                    "Depósito de conservación", false, List.of()),
            new DatoObra("La traición de las imágenes", MAGRITTE, null, 1929,
                    "Óleo sobre lienzo", "60,3 cm x 81,1 cm", "Surrealismo", "Fantasía",
                    "Una pipa acompañada de la frase \"Esto no es una pipa\", que cuestiona la representación.",
                    "Sala de Arte Contemporáneo", true, List.of()),
            new DatoObra("Las señoritas de Aviñón", PICASSO, null, 1907,
                    "Óleo sobre lienzo", "243,9 cm x 233,7 cm", "Cubismo", "Retrato",
                    "Cinco figuras femeninas de rostros geométricos, considerada precursora del cubismo.",
                    "Sala de Arte Contemporáneo", false, List.of())
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
            @Qualifier("UsuarioSQLRepositoryImpl") UsuarioRepository usuarioRepository,
            @Value("${app.datos-de-ejemplo-ampliados:true}") boolean ampliados) {

        return args -> {
            List<UsuarioEntity> visitantes = EMAILS_VISITANTES.stream()
                    .map(email -> usuarioRepository.findByEmail(email)
                            .orElseThrow(() -> new IllegalStateException("Falta el usuario " + email)))
                    .toList();

            // Ronda entre los visitantes de ejemplo para repartir la autoría de
            // los comentarios: no hay forma de saber a qué usuario real
            // correspondía cada nombre ficticio de ObraData.
            int indiceComentario = 0;
            List<DatoObra> obras = ampliados
                    ? Stream.concat(OBRAS.stream(), OBRAS_AMPLIADAS.stream()).toList()
                    : OBRAS;
            for (DatoObra dato : obras) {
                Optional<ObraEntity> existente = obraRepository.buscarPorTituloConArtista(dato.titulo());
                if (existente.isPresent()) {
                    completarFaltantes(existente.get(), dato, obraRepository, artistaRepository);
                    continue;
                }

                ArtistaEntity artistaEntity = artistaRepository.findFirstByNombre(dato.artista().nombre())
                        .orElseGet(ArtistaEntity::new);
                completarArtista(artistaEntity, dato.artista());
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
                obraEntity.setTematica(dato.tematica());
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

    // Solo se completa lo que está en null: una base ya poblada puede tener
    // valores editados a mano que no hay que pisar.
    private static void completarFaltantes(ObraEntity obra, DatoObra dato, ObraRepository obraRepository,
                                           ArtistaRepository artistaRepository) {
        if (obra.getTematica() == null) {
            obra.setTematica(dato.tematica());
            obraRepository.save(obra);
        }
        if (completarArtista(obra.getArtista(), dato.artista())) {
            artistaRepository.save(obra.getArtista());
        }
    }

    private static boolean completarArtista(ArtistaEntity artista, DatoArtista dato) {
        boolean cambio = false;
        if (artista.getNombre() == null) {
            artista.setNombre(dato.nombre());
            cambio = true;
        }
        if (artista.getBiografia() == null) {
            artista.setBiografia(dato.biografia());
            cambio = true;
        }
        if (artista.getNacionalidad() == null) {
            artista.setNacionalidad(dato.nacionalidad());
            cambio = true;
        }
        if (artista.getMovimiento() == null) {
            artista.setMovimiento(dato.movimiento());
            cambio = true;
        }
        if (artista.getAnioNacimiento() == null) {
            artista.setAnioNacimiento(dato.anioNacimiento());
            cambio = true;
        }
        if (artista.getAnioFallecimiento() == null) {
            artista.setAnioFallecimiento(dato.anioFallecimiento());
            cambio = true;
        }
        return cambio;
    }
}
