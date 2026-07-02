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



        // ✅ Método único y defensivo
        public static EstadoTurno obtenerPorId(int id) {
            return Arrays.stream(values())
                    .filter(e -> e.id == id)
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("Estado de turno inexistente: " + id));
        }
    }


