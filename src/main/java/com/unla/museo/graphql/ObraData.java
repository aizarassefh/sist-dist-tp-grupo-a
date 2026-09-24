package com.unla.museo.graphql;

import java.util.List;
import java.util.Locale;

public class ObraData {

    public static List<Obra> obtenerObras() {

        return List.of(

        		   new Obra(
        			        1L,
        			        "La Gioconda",
        			        new Artista(
        			            "Leonardo da Vinci",
        			            "Artista italiano del Renacimiento, reconocido por sus aportes al arte y la ciencia."
        			        ),
        			        "/obras/gioconda.jpg",
        			        1503,
        			        "Óleo sobre tabla",
        			        "77 cm x 53 cm",
        			        "Renacimiento",
        			        "Retrato de Lisa Gherardini, una de las obras más reconocidas del Renacimiento.",
        			        "Sala de Arte Europeo",
        			        true,
        			        List.of(
        			            new Comentario(
        			                "Ana",
        			                "Una de las obras más impresionantes del museo.",
        			                "2026-09-10"
        			            ),
        			            new Comentario(
        			                "Carlos",
        			                "Me interesa mucho la técnica utilizada.",
        			                "2026-09-12"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        2L,
        			        "La noche estrellada",
        			        new Artista(
        			            "Vincent van Gogh",
        			            "Pintor neerlandés asociado al postimpresionismo."
        			        ),
        			        "/obras/noche-estrellada.jpg",
        			        1889,
        			        "Óleo sobre lienzo",
        			        "73,7 cm x 92,1 cm",
        			        "Postimpresionismo",
        			        "Paisaje nocturno caracterizado por sus formas curvas y colores intensos.",
        			        "Sala de Arte Moderno",
        			        true,
        			        List.of(
        			            new Comentario(
        			                "Lucía",
        			                "Los colores de esta obra son increíbles.",
        			                "2026-09-08"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        3L,
        			        "El grito",
        			        new Artista(
        			            "Edvard Munch",
        			            "Pintor noruego vinculado al expresionismo."
        			        ),
        			        "/obras/el-grito.jpg",
        			        1893,
        			        "Óleo y pastel sobre cartón",
        			        "91 cm x 73,5 cm",
        			        "Expresionismo",
        			        "Obra que representa una figura humana sobre un paisaje de colores intensos.",
        			        "Depósito de conservación",
        			        false,
        			        List.of()
        			    ),

        			    new Obra(
        			        4L,
        			        "La persistencia de la memoria",
        			        new Artista(
        			            "Salvador Dalí",
        			            "Pintor español asociado al surrealismo."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/c/c9/Dal%C3%AD%2C_Perfil_del_tiempo%2C_Vroclavo%2C_7.jpeg/500px-Dal%C3%AD%2C_Perfil_del_tiempo%2C_Vroclavo%2C_7.jpeg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
        			        1931,
        			        "Óleo sobre lienzo",
        			        "24 cm x 33 cm",
        			        "Surrealismo",
        			        "Paisaje surrealista donde aparecen relojes deformados sobre distintos elementos.",
        			        "Sala de Arte Moderno",
        			        true,
        			        List.of(
        			            new Comentario(
        			                "Martín",
        			                "Una obra muy interesante para analizar desde el surrealismo.",
        			                "2026-09-05"
        			            ),
        			            new Comentario(
        			                "Sofía",
        			                "Los relojes son el elemento que más me llamó la atención.",
        			                "2026-09-11"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        5L,
        			        "Las meninas",
        			        new Artista(
        			            "Diego Velázquez",
        			            "Pintor español del Siglo de Oro."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/9/99/Las_Meninas_01.jpg/500px-Las_Meninas_01.jpg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
        			        1656,
        			        "Óleo sobre lienzo",
        			        "318 cm x 276 cm",
        			        "Barroco",
        			        "Compleja composición que representa a la familia de Felipe IV en el taller del artista.",
        			        "Sala de Arte Español",
        			        true,
        			        List.of(
        			            new Comentario(
        			                "Pedro",
        			                "La composición de esta obra es fascinante.",
        			                "2026-09-03"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        6L,
        			        "El nacimiento de Venus",
        			        new Artista(
        			            "Sandro Botticelli",
        			            "Pintor italiano del Renacimiento florentino."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/0/0b/Sandro_Botticelli_-_La_nascita_di_Venere_-_Google_Art_Project_-_edited.jpg/500px-Sandro_Botticelli_-_La_nascita_di_Venere_-_Google_Art_Project_-_edited.jpg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
        			        1485,
        			        "Temple sobre lienzo",
        			        "172 cm x 278 cm",
        			        "Renacimiento",
        			        "Representación del nacimiento de Venus, inspirada en la mitología clásica.",
        			        "Sala de Arte Europeo",
        			        true,
        			        List.of()
        			    ),

        			    new Obra(
        			        7L,
        			        "Guernica",
        			        new Artista(
        			            "Pablo Picasso",
        			            "Artista español y una de las figuras centrales del arte del siglo XX."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/7/76/Mural_del_%22Guernica%22_de_Picasso.jpg/500px-Mural_del_%22Guernica%22_de_Picasso.jpg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
        			        1937,
        			        "Óleo sobre lienzo",
        			        "349 cm x 777 cm",
        			        "Cubismo",
        			        "Obra de gran formato realizada en respuesta a los acontecimientos de la Guerra Civil Española.",
        			        "Sala de Arte Contemporáneo",
        			        false,
        			        List.of(
        			            new Comentario(
        			                "Valentina",
        			                "El tamaño de la obra cambia completamente la experiencia.",
        			                "2026-09-01"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        8L,
        			        "El beso",
        			        new Artista(
        			            "Gustav Klimt",
        			            "Pintor austríaco relacionado con el simbolismo y el modernismo."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/4/40/The_Kiss_-_Gustav_Klimt_-_Google_Cultural_Institute.jpg/500px-The_Kiss_-_Gustav_Klimt_-_Google_Cultural_Institute.jpg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail?download",
        			        1908,
        			        "Óleo y pan de oro sobre lienzo",
        			        "180 cm x 180 cm",
        			        "Modernismo",
        			        "Obra caracterizada por sus patrones decorativos y el uso de pan de oro.",
        			        "Sala de Arte Moderno",
        			        true,
        			        List.of(
        			            new Comentario(
        			                "Julia",
        			                "El uso del dorado hace que la obra sea muy llamativa.",
        			                "2026-08-29"
        			            )
        			        )
        			    ),

        			    new Obra(
        			        9L,
        			        "La joven de la perla",
        			        new Artista(
        			            "Johannes Vermeer",
        			            "Pintor neerlandés del período barroco."
        			        ),
        			        "https://thumb.wikimedia.org/wikipedia/commons/thumb/d/d7/Meisje_met_de_parel.jpg/500px-Meisje_met_de_parel.jpg?utm_source=es.wikipedia.org&utm_campaign=imageinfo&utm_content=thumbnail",
        			        1665,
        			        "Óleo sobre lienzo",
        			        "44,5 cm x 39 cm",
        			        "Barroco",
        			        "Retrato conocido por el tratamiento de la luz y la expresión de la figura.",
        			        "Sala de Arte Europeo",
        			        true,
        			        List.of()
        			    )
        			
            
        );
    }
    
    public static List<Obra> filtrarObras(FiltroObras filtro) {
        List<Obra> obras = obtenerObras();

        if (filtro == null) {
            return obras;
        }

        return obras.stream()
                .filter(obra -> {

                    // Palabra clave:
                    // busca en título, descripción o artista.
                    if (filtro.getPalabraClave() != null
                            && !filtro.getPalabraClave().isBlank()) {

                        String palabra = filtro.getPalabraClave()
                                .toLowerCase(Locale.ROOT);

                        boolean coincide = obra.getTitulo()
                                .toLowerCase(Locale.ROOT)
                                .contains(palabra)
                                || obra.getDescripcion()
                                .toLowerCase(Locale.ROOT)
                                .contains(palabra)
                                || obra.getArtista()
                                .getNombre()
                                .toLowerCase(Locale.ROOT)
                                .contains(palabra);

                        if (!coincide) {
                            return false;
                        }
                    }

                    // Época: coincidencia parcial, ignorando mayúsculas/minúsculas.
                    if (filtro.getEpoca() != null
                            && !filtro.getEpoca().isBlank()
                            && !obra.getEpoca()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(filtro.getEpoca()
                                            .toLowerCase(Locale.ROOT))) {
                        return false;
                    }

                    // Técnica: coincidencia parcial.
                    if (filtro.getTecnica() != null
                            && !filtro.getTecnica().isBlank()
                            && !obra.getTecnica()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(filtro.getTecnica()
                                            .toLowerCase(Locale.ROOT))) {
                        return false;
                    }

                    // Ubicación: coincidencia parcial.
                    if (filtro.getUbicacion() != null
                            && !filtro.getUbicacion().isBlank()
                            && !obra.getUbicacion()
                                    .toLowerCase(Locale.ROOT)
                                    .contains(filtro.getUbicacion()
                                            .toLowerCase(Locale.ROOT))) {
                        return false;
                    }

                    // En exhibición: true y false son filtros válidos.
                    if (filtro.getEnExhibicion() != null
                            && !obra.getEnExhibicion()
                                    .equals(filtro.getEnExhibicion())) {
                        return false;
                    }

                    return true;
                })
                .sorted(
                        java.util.Comparator
                                .comparing(
                                        Obra::getTitulo,
                                        String.CASE_INSENSITIVE_ORDER
                                )
                                .thenComparing(Obra::getId)
                )
                .toList();
    }
}