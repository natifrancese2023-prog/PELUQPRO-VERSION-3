package claseslogicas;

/**
 * Estados posibles de una Factura.
 *
 * FIX respecto a la versión original:
 * - Se agrega el estado PAGADA (id=4), que antes no existía.
 *   Antes "marcarComoPagada()" reasignaba FACTURADA, que ya era el estado
 *   con el que se creaba la factura -> la acción era un no-op semántico.
 * - Se sobreescribe toString() para que los ComboBox<EstadoFactura> del FXML
 *   muestren "Pendiente" / "Facturada" / "Pagada" / "Anulada" en vez del
 *   nombre crudo del enum (PENDIENTE, FACTURADA, etc.).
 */
public enum EstadoFactura {
    PENDIENTE(1, "Pendiente"),
    FACTURADA(2, "Facturada"),
    ANULADA(3, "Anulada"),
    PAGADA(4, "Pagada");

    private final int id;
    private final String nombre;

    EstadoFactura(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getIdEstadoFactura() { return id; }
    public String getNombre() { return nombre; }

    public static EstadoFactura fromId(int id) {
        for (EstadoFactura estado : values()) {
            if (estado.getIdEstadoFactura() == id) {
                return estado;
            }
        }
        throw new IllegalArgumentException("EstadoFactura no válido: " + id);
    }

    /**
     * Reglas de transición válidas. Usado por FacturaService para no permitir
     * por ejemplo "pagar" una factura ya ANULADA, o "anular" una PAGADA.
     * Ajustá estas reglas a tu negocio real si difieren.
     */
    public boolean puedeTransicionarA(EstadoFactura destino) {
        return switch (this) {
            case PENDIENTE -> destino == FACTURADA || destino == ANULADA;
            case FACTURADA -> destino == PAGADA || destino == ANULADA;
            case PAGADA -> false;   // estado terminal: no se puede anular ni repagar
            case ANULADA -> false;  // estado terminal
        };
    }

    @Override
    public String toString() {
        return nombre;
    }
}