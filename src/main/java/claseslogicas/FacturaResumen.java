package claseslogicas;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.StringJoiner;

public class FacturaResumen {

    private LocalDate fecha;
    private BigDecimal totalFacturado;
    private int cantidadFacturas;
    private List<String> metodosPago;

    public FacturaResumen(LocalDate fecha, BigDecimal totalFacturado, List<String> metodosPago, int cantidadFacturas) {
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
    public int getCantidadFacturas() {
        return cantidadFacturas;
    }
}