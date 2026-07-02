package claseslogicas;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Factura {

    private int idFactura;
    private int idCliente;
    private int idTurno;
    private List<Servicio> servicios;
    private List<DetalleFactura> detalles;
    private BigDecimal montoTotal;        // ✅ ahora BigDecimal
    private String formaPago;
    private String metodoPago;
    private LocalDateTime fechaHora;
    private String estado;
    private String estadoFacturaNombre;
    private EstadoFactura estadoFactura;
    private Cliente cliente;
    private String clienteDocumento;

    // ===================== CONSTRUCTORES =====================

    public Factura(Visita visita, String formaPago) {
        this.idCliente = visita.getIdCliente();
        this.servicios = visita.getServiciosRealizados();
        this.formaPago = formaPago;
        this.fechaHora = LocalDateTime.now();
        this.detalles = new ArrayList<>();
        this.montoTotal = BigDecimal.ZERO; // inicializar en 0
    }

    public Factura() {
        this.detalles = new ArrayList<>();
        this.montoTotal = BigDecimal.ZERO;
    }

    // ===================== GETTERS =====================

    public int getIdFactura() { return idFactura; }
    public int getIdCliente() { return idCliente; }
    public int getIdTurno() { return idTurno; }
    public List<Servicio> getServicios() { return servicios; }
    public BigDecimal getMontoTotal() { return montoTotal; }   // ✅ BigDecimal
    public String getMetodoPago() { return metodoPago; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public String getEstado() { return estado; }
    public EstadoFactura getEstadoFactura() { return estadoFactura; }
    public Cliente getCliente() { return cliente; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    public String getEstadoFacturaNombre() { return estadoFacturaNombre; }

    // ===================== SETTERS =====================

    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public void setIdTurno(int idTurno) { this.idTurno = idTurno; }
    public void setServicios(List<Servicio> servicios) { this.servicios = servicios; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; } // ✅ BigDecimal
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setEstado(String estado) { this.estado = estado; }
    public void setEstadoFacturaNombre(String estadoFacturaNombre) { this.estadoFacturaNombre = estadoFacturaNombre; }
    public void setEstadoFactura(EstadoFactura estadoFactura) { this.estadoFactura = estadoFactura; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    // ===================== MÉTODOS =====================

    public void addDetalle(DetalleFactura detalle) {
        if (this.detalles == null) {
            this.detalles = new ArrayList<>();
        }
        this.detalles.add(detalle);
    }

    @Override
    public String toString() {
        return "Factura #" + idFactura +
                "\nID Cliente: " + idCliente +
                "\nDocumento Cliente: " + (clienteDocumento != null ? clienteDocumento : "N/A") +
                "\nServicios: " + (servicios != null ? servicios.size() : 0) +
                "\nForma de pago: " + formaPago +
                "\nMétodo de pago: " + metodoPago +
                "\nTotal: $" + (montoTotal != null ? montoTotal.setScale(2) : "0.00") +
                "\nEstado: " + (estadoFacturaNombre != null ? estadoFacturaNombre : "N/A") +
                "\nFecha: " + (fechaHora != null ? fechaHora.toString() : "N/A");
    }
}

