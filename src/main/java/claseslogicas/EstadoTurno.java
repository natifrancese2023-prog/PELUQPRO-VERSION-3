package claseslogicas;

public enum EstadoTurno {

    PENDIENTE(1, "Pendiente"),
    CONFIRMADO(2, "Confirmado"),
    FINALIZADO(3, "Finalizado"),
    CANCELADO(4, "Cancelado"),
    FACTURADO(5, "Facturado (Pago Recibido)");

    private final int id;
    private final String nombre;

    EstadoTurno(int id, String nombre) {
        this.id = id;
        this.nombre = nombre;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }
    public static EstadoTurno fromId(int id) { for (EstadoTurno estado : EstadoTurno.values()) { if (estado.id == id) { return estado; } } throw new IllegalArgumentException("ID de EstadoTurno inválida: " + id); }

    public static EstadoTurno obtenerPorId(int id) {
        for (EstadoTurno estado : values()) {
            if (estado.id == id) {
                return estado;
            }
        }
        return null; // o lanzar excepción si preferís
    }
}
