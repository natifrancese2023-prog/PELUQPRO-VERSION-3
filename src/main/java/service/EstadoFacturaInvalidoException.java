package service;

/**
 * NUEVO (flujo de estado): se lanza cuando se intenta una transición de
 * EstadoFactura que EstadoFactura.puedeTransicionarA(...) no permite
 * (ej: pagar una factura ANULADA, anular una ya PAGADA, etc).
 * RuntimeException para no forzar try/catch en cadenas de llamada que
 * hoy solo manejan SQLException; el Controller la captura explícitamente.
 */
public class EstadoFacturaInvalidoException extends RuntimeException {
    public EstadoFacturaInvalidoException(String mensaje) {
        super(mensaje);
    }
}