package claseslogicas;


import java.math.BigDecimal;

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
                          int cantidad, BigDecimal subtotal) {
        this.idDetalle = idDetalle;
        this.idFactura = idFactura;
        this.idServicio = idServicio;
        this.descripcionServicio = descripcionServicio;
        this.precioUnitario = precioUnitario;
        this.cantidad = cantidad;
        this.subtotal = subtotal;
    }

    public Servicio getServicio() { return servicio; }
    public void setIdDetalle(int idDetalle) { this.idDetalle = idDetalle; }

    public void setDescripcionServicio(String descripcionServicio) { this.descripcionServicio = descripcionServicio; }
    public void setPrecioUnitario(BigDecimal precioUnitario) { this.precioUnitario = precioUnitario; }
    public void setCantidad(int cantidad) { this.cantidad = cantidad; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setServicio(Servicio servicio) { this.servicio = servicio; }
    public int getIdDetalle() { return idDetalle; }
    public int getIdFactura() { return idFactura; }
    public int getIdServicio() { return idServicio; }
    public String getDescripcionServicio() { return descripcionServicio; }
    public BigDecimal getPrecioUnitario() { return precioUnitario; }
    public int getCantidad() { return cantidad; }
    public BigDecimal getSubtotal() { return subtotal; }

    @Override
    public String toString() {
        return "DetalleFactura #" + idDetalle +
                " | Servicio: " + (servicio != null ? servicio.getNombreServicio() : "N/A") +
                " | Cantidad: " + cantidad +
                " | Precio Unitario: " + precioUnitario +
                " | Subtotal: " + subtotal;
    }
}

