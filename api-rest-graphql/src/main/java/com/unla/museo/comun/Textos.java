package com.unla.museo.comun;

import java.text.Normalizer;
import java.util.Locale;

public final class Textos {

    private Textos() {
    }

    // Minúsculas y sin acentos, para que "OLEO", "óleo" y "Óleo" coincidan. Los
    // filtros gRPC se comparan en memoria con esto porque el collation de
    // MariaDB y el de H2 no tratan los acentos igual.
    public static String sinAcentos(String valor) {
        if (valor == null) {
            return "";
        }
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }
}
