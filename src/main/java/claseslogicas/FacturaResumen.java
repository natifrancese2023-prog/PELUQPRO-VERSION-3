package claseslogicas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.StringJoiner;

public class FacturaResumen {

    private LocalDate fecha;
    private BigDecimal totalFacturado;
    private Map<String, Integer> metodosPago;

    public FacturaResumen(LocalDate fecha, BigDecimal totalFacturado, Map<String, Integer> metodosPago) {
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

    // Método para mostrar los métodos de pago como texto concatenado
    public String getResumenMetodosPago() {
        if (metodosPago == null || metodosPago.isEmpty()) return "Sin datos";
        StringJoiner joiner = new StringJoiner(" | ");
        for (Map.Entry<String, Integer> entry : metodosPago.entrySet()) {
            joiner.add(entry.getKey() + ": " + entry.getValue());
        }
        return joiner.toString();
    }
}
