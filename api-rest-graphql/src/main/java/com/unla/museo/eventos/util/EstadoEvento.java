package com.unla.museo.eventos.util;

/**
 * Estado de un evento relativo a "ahora" (el Clock inyectado), no una
 * columna calculada: un evento es PASADO si fechaHora &lt;= ahora.
 */
public enum EstadoEvento {
    PASADOS,
    FUTUROS,
    TODOS
}
