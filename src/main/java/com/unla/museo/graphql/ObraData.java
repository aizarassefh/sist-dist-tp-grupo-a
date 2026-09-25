package com.unla.museo.graphql;

import java.util.List;
import java.util.Locale;

public final class ObraData {
	private ObraData() {
	}

	public static List<Obra> obtenerObras() {
		return List.of(
				new Obra(1L, "La Gioconda",
						new Artista("Leonardo da Vinci", "Pintor, inventor y científico del Renacimiento italiano."),
						"/obras/gioconda.jpg", 1503, "Óleo sobre tabla", "77 cm x 53 cm", "Renacimiento",
						"Retrato de una mujer realizado durante el Renacimiento italiano.", "Sala de Arte Europeo",
						true,
						List.of(new Comentario("Ana", "Una de las obras más reconocidas del museo.", "2026-09-01"),
								new Comentario("Carlos", "La expresión es fascinante.", "2026-09-03"))),
				new Obra(2L, "La noche estrellada",
						new Artista("Vincent van Gogh", "Pintor neerlandés asociado al postimpresionismo."),
						"/obras/noche-estrellada.jpg", 1889, "Óleo sobre lienzo", "73,7 cm x 92,1 cm",
						"Postimpresionismo",
						"Paisaje nocturno representado desde la ventana del sanatorio de Saint-Rémy.",
						"Sala de Arte Moderno", true,
						List.of(new Comentario("Lucía", "Los colores destacan muchísimo.", "2026-09-05"))),
				new Obra(3L, "El grito", new Artista("Edvard Munch", "Pintor noruego vinculado al expresionismo."),
						"/obras/el-grito.jpg", 1893, "Óleo y pastel sobre cartón", "91 cm x 73,5 cm", "Expresionismo",
						"Obra que representa una figura en un paisaje atravesado por una intensa sensación de angustia.",
						"Depósito de conservación", false, List.of()),
				new Obra(4L, "La persistencia de la memoria",
						new Artista("Salvador Dalí", "Artista español y una de las figuras del surrealismo."),
						"/obras/persistencia-memoria.jpg", 1931, "Óleo sobre lienzo", "24 cm x 33 cm", "Surrealismo",
						"Paisaje onírico donde aparecen relojes blandos sobre un espacio desértico.",
						"Sala de Arte Moderno", true,
						List.of(new Comentario("Sofía", "Los relojes son el elemento que más llama la atención.",
								"2026-09-07"),
								new Comentario("Juan", "Una obra muy interesante para analizar.", "2026-09-08"))),
				new Obra(5L, "Las meninas", new Artista("Diego Velázquez", "Pintor español del período barroco."),
						"/obras/las-meninas.jpg", 1656, "Óleo sobre lienzo", "318 cm x 276 cm", "Barroco",
						"Escena cortesana que representa a la infanta Margarita y su séquito.", "Sala de Arte Español",
						true, List.of(new Comentario("María", "La composición tiene muchos detalles.", "2026-09-09"))),
				new Obra(6L, "El nacimiento de Venus",
						new Artista("Sandro Botticelli", "Pintor italiano del Renacimiento florentino."),
						"/obras/nacimiento-venus.jpg", 1485, "Temple sobre lienzo", "172,5 cm x 278,5 cm",
						"Renacimiento", "Representación de Venus emergiendo del mar sobre una concha.",
						"Sala de Arte Europeo", true, List.of()),
				new Obra(7L, "Guernica",
						new Artista("Pablo Picasso",
								"Artista español y una de las figuras centrales del arte del siglo XX."),
						"/obras/guernica.jpg", 1937, "Óleo sobre lienzo", "349,3 cm x 776,6 cm", "Cubismo",
						"Pintura monumental relacionada con el bombardeo de Guernica durante la Guerra Civil Española.",
						"Sala de Arte Contemporáneo", false,
						List.of(new Comentario("Pedro", "La composición transmite una escena muy intensa.",
								"2026-09-10"))),
				new Obra(8L, "El beso",
						new Artista("Gustav Klimt", "Pintor austríaco asociado al modernismo y la Secesión de Viena."),
						"/obras/el-beso.jpg", 1908, "Óleo y pan de oro sobre lienzo", "180 cm x 180 cm", "Modernismo",
						"Representación de una pareja abrazada dentro de una composición decorativa.",
						"Sala de Arte Moderno", true,
						List.of(new Comentario("Martina", "El uso del dorado es muy característico.", "2026-09-11"))),
				new Obra(9L, "La joven de la perla",
						new Artista("Johannes Vermeer", "Pintor neerlandés del período barroco."),
						"/obras/joven-perla.jpg", 1665, "Óleo sobre lienzo", "44,5 cm x 39 cm", "Barroco",
						"Retrato de una joven con un pendiente de perla.", "Sala de Arte Europeo", true, List.of()));
	}

	public static List<Obra> filtrarObras(FiltroObras f) {
		if (f == null)
			return obtenerObras().stream().sorted((a, b) -> {
				int c = a.getTitulo().compareToIgnoreCase(b.getTitulo());
				return c != 0 ? c : a.getId().compareTo(b.getId());
			}).toList();
		String clave = f.getPalabraClave() == null ? "" : f.getPalabraClave().trim().toLowerCase(Locale.ROOT);
		String epoca = f.getEpoca() == null ? "" : f.getEpoca().trim().toLowerCase(Locale.ROOT);
		String tecnica = f.getTecnica() == null ? "" : f.getTecnica().trim().toLowerCase(Locale.ROOT);
		String ubicacion = f.getUbicacion() == null ? "" : f.getUbicacion().trim().toLowerCase(Locale.ROOT);
		return obtenerObras().stream()
				.filter(o -> clave.isBlank() || o.getTitulo().toLowerCase(Locale.ROOT).contains(clave)
						|| o.getDescripcion().toLowerCase(Locale.ROOT).contains(clave)
						|| o.getArtista().getNombre().toLowerCase(Locale.ROOT).contains(clave))
				.filter(o -> epoca.isBlank() || o.getEpoca().toLowerCase(Locale.ROOT).contains(epoca))
				.filter(o -> tecnica.isBlank() || o.getTecnica().toLowerCase(Locale.ROOT).contains(tecnica))
				.filter(o -> ubicacion.isBlank() || o.getUbicacion().toLowerCase(Locale.ROOT).contains(ubicacion))
				.filter(o -> f.getEnExhibicion() == null || o.getEnExhibicion().equals(f.getEnExhibicion()))
				.sorted((a, b) -> {
					int c = a.getTitulo().compareToIgnoreCase(b.getTitulo());
					return c != 0 ? c : a.getId().compareTo(b.getId());
				}).toList();
	}
}
