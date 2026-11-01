package com.unla.museo.mensajeria;

import java.util.List;

public record ConsistenciaDTO(
        long generados,
        long publicados,
        long pendientesDePublicar,
        List<ConsumidorDTO> consumidores,
        long fallidosPendientes,
        boolean consistente
) {

    /** faltantes: ids publicados que el consumidor todavía no registró (hasta 50). */
    public record ConsumidorDTO(String consumidor, long procesados, List<String> faltantes) {
    }
}
