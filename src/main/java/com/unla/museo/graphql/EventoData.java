package com.unla.museo.graphql;

import java.time.LocalDateTime;
import java.util.List;

public class EventoData {

    public static List<EventoGraphQL> obtenerEventos() {

        return List.of(

        		new EventoGraphQL(
        			    1L,
        			    "Visita guiada al museo",
        			    "VISITA_GUIADA",
        			    LocalDateTime.of(2026, 9, 10, 15, 0),
        			    List.of("Ana", "Carlos", "Lucía", "Pedro")
        			),

        			new EventoGraphQL(
        			    2L,
        			    "Taller de pintura para principiantes",
        			    "TALLER",
        			    LocalDateTime.of(2026, 9, 12, 16, 0),
        			    List.of("María", "Juan", "Sofía", "Pedro", "Lucía")
        			),

        			new EventoGraphQL(
        			    3L,
        			    "Charla sobre Vincent van Gogh",
        			    "CHARLA",
        			    LocalDateTime.of(2026, 9, 15, 18, 0),
        			    List.of(
        			        "Carlos",
        			        "Ana",
        			        "Martina",
        			        "Lucas",
        			        "Sofía",
        			        "Juan"
        			    )
        			),

        			new EventoGraphQL(
        			    4L,
        			    "Visita guiada: Arte Moderno",
        			    "VISITA_GUIADA",
        			    LocalDateTime.of(2026, 9, 18, 14, 0),
        			    List.of(
        			        "Pedro",
        			        "Lucía",
        			        "Ana",
        			        "Carlos",
        			        "María"
        			    )
        			),

        			new EventoGraphQL(
        			    5L,
        			    "Introducción al surrealismo",
        			    "CHARLA",
        			    LocalDateTime.of(2026, 9, 20, 17, 0),
        			    List.of(
        			        "Juan",
        			        "Sofía",
        			        "Martina",
        			        "Lucas"
        			    )
        			),

        			new EventoGraphQL(
        			    6L,
        			    "Taller de técnicas de óleo",
        			    "TALLER",
        			    LocalDateTime.of(2026, 9, 22, 16, 0),
        			    List.of(
        			        "Ana",
        			        "Carlos",
        			        "Lucía",
        			        "Pedro",
        			        "María",
        			        "Juan",
        			        "Sofía"
        			    )
        			),

        			new EventoGraphQL(
        			    7L,
        			    "Recorrido por el arte barroco",
        			    "VISITA_GUIADA",
        			    LocalDateTime.of(2026, 8, 3, 15, 0),
        			    List.of(
        			        "Lucas",
        			        "Martina",
        			        "Ana",
        			        "Pedro",
        			        "Carlos"
        			    )
        			),

        			new EventoGraphQL(
        			    8L,
        			    "Historia del arte contemporáneo",
        			    "CHARLA",
        			    LocalDateTime.of(2026, 7, 10, 18, 0),
        			    List.of(
        			        "María",
        			        "Juan",
        			        "Sofía",
        			        "Lucía",
        			        "Pedro",
        			        "Ana"
        			    )
        			),

        			new EventoGraphQL(
        			    9L,
        			    "Taller de dibujo artístico",
        			    "TALLER",
        			    LocalDateTime.of(2026, 6, 15, 16, 0),
        			    List.of(
        			        "Carlos",
        			        "Lucas",
        			        "Martina",
        			        "Sofía",
        			        "Juan"
        			    )
        			),

        			new EventoGraphQL(
        			    10L,
        			    "Visita especial: Grandes obras europeas",
        			    "VISITA_GUIADA",
        			    LocalDateTime.of(2026, 5, 20, 15, 0),
        			    List.of(
        			        "Ana",
        			        "Carlos",
        			        "Lucía",
        			        "Pedro",
        			        "María",
        			        "Juan",
        			        "Sofía",
        			        "Lucas"
        			    )
        			)
        );
    }
}