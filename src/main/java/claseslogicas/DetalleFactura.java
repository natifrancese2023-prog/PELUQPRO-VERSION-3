package claseslogicas;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class DetalleFactura {

    private int idDetalle;
    private int idFactura;
    private int idServicio;
    private String descripcionServicio;
    private BigDecimal precioUnitario;
    private int cantidad;
    private BigDecimal subtotal;
    private Servicio servicio; // relación con la clase Servicio

    // ===================== CONSTRUCTORES =====================

    public DetalleFactura() {}

    public DetalleFactura(int idDetalle, int idFactura, int idServicio,
                          String descripcionServicio, BigDecimal precioUnitario,
                          int cantidad) {
        this.idDetalle = idDetalle;
        this.idFactura = idFactura;
        this.idServicio = idServicio;
        this.descripcionServicio = descripcionServicio;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        recalcularSubtotal(); // ✅ siempre calcular subtotal
    }

    // ===================== GETTERS =====================

    public int getIdDetalle() { return idDetalle; }
    public int getIdFactura() { return idFactura; }
    public int getIdServicio() { return idServicio; }
    public String getDescripcionServicio() { return descripcionServicio; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() { return subtotal; }
    public Servicio getServicio() { return servicio; }

    // ===================== SETTERS =====================

    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }
    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }


    public void setDescripcionServicio(String descripcionServicio) { this.descripcionServicio = descripcionServicio; }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
        recalcularSubtotal();
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public void setServicio(Servicio servicio) { this.servicio = servicio; }

    // ===================== MÉTODOS =====================

    private void recalcularSubtotal() {
        if (precioUnitario != null && cantidad > 0) {
            this.subtotal = precioUnitario
                    .multiply(BigDecimal.valueOf(cantidad))
                    .setScale(2, RoundingMode.HALF_UP);
        } else {
            this.subtotal = BigDecimal.ZERO;
        }
    }

    @Override
    public String toString() {
        return "DetalleFactura #" + idDetalle +
                " | Servicio: " + (servicio != null ? servicio.getNombreServicio() : "N/A") +
                " | Cantidad: " + cantidad +
                " | Precio Unitario: " + (precioUnitario != null ? precioUnitario.setScale(2, RoundingMode.HALF_UP) : "0.00") +
                " | Subtotal: " + (subtotal != null ? subtotal.setScale(2, RoundingMode.HALF_UP) : "0.00");
    }
}
