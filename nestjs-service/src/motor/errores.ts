// Error de dominio sin dependencia de Nest: las funciones puras lo lanzan y el
// controller lo traduce a INVALID_ARGUMENT de gRPC.
export class ArgumentoInvalidoError extends Error {}
