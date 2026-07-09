package claseslogicas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

public class FacturaResumen {

    private LocalDate fecha;
    private BigDecimal totalFacturado;
    // FIX: antes era Map<String, Integer> con el conteo de todo el período
    // (ej: "Efectivo: 5 | Transferencia: 3"), y encima el mismo mapa se
    // repetía en todas las filas de la tabla sin importar el día. Ahora es
    // la lista de métodos de pago, uno por cada factura de ESE día
    // puntual, en el orden en que se cobraron.
    private List<String> metodosPago;

    public FacturaResumen(LocalDate fecha, BigDecimal totalFacturado, List<String> metodosPago) {
        this.fecha = fecha;
        this.totalFacturado = totalFacturado;
        this.metodosPago = metodosPago;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public BigDecimal getTotalFacturado() {
        return totalFacturado;
    }

    public void setTotalFacturado(BigDecimal totalFacturado) {
        this.totalFacturado = totalFacturado;
    }


    public String getResumenMetodosPago() {
        if (metodosPago == null || metodosPago.isEmpty()) return "Sin datos";
        StringJoiner joiner = new StringJoiner(", ");
        for (String metodo : metodosPago) {
            joiner.add(metodo != null ? metodo : "Sin especificar");
        }
        return joiner.toString();
    }
}