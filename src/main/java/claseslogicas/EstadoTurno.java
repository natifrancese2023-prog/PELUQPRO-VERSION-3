package claseslogicas;

import java.util.Arrays;

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


    public static EstadoTurno obtenerPorId(int id) {
        return Arrays.stream(values())
                .filter(e -> e.id == id)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Estado de turno inexistente: " + id));
    }


    public static EstadoTurno obtenerPorIdSeguro(int id, EstadoTurno fallback) {
        return Arrays.stream(values())
                .filter(e -> e.id == id)
                .findFirst()
                .orElse(fallback);
    }
    public static EstadoTurno buscarPorId(int id) {
        for (EstadoTurno estado : EstadoTurno.values()) {
            if (estado.getId() == id) {
                return estado;
            }
        }
        return PENDIENTE; // Estado por defecto en caso de fallas
    }
}