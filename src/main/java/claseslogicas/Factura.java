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
    private BigDecimal montoTotal;
    private String formaPago;
    private String metodoPago;
    private LocalDateTime fechaHora;
    private EstadoFactura estadoFactura;   // ✅ único campo para estado
    private Cliente cliente;
    private String clienteDocumento;

    // ===================== CONSTRUCTORES =====================

    public Factura(Visita visita, String formaPago) {
        this.idCliente = visita.getIdCliente();
        this.servicios = visita.getServiciosRealizados();
        this.formaPago = formaPago;
        this.fechaHora = LocalDateTime.now();
        this.detalles = new ArrayList<>();
        this.montoTotal = BigDecimal.ZERO;
        this.estadoFactura = EstadoFactura.PENDIENTE; // estado inicial
    }

    public Factura() {
        this.detalles = new ArrayList<>();
        this.montoTotal = BigDecimal.ZERO;
        this.estadoFactura = EstadoFactura.PENDIENTE; // estado inicial
    }

    // ===================== GETTERS =====================

    public int getIdFactura() { return idFactura; }
    public int getIdCliente() { return idCliente; }
    public int getIdTurno() { return idTurno; }
    public List<Servicio> getServicios() { return servicios; }
    public BigDecimal getMontoTotal() { return montoTotal; }
    public String getMetodoPago() { return metodoPago; }
    public LocalDateTime getFechaHora() { return fechaHora; }
    public EstadoFactura getEstadoFactura() { return estadoFactura; }
    public Cliente getCliente() { return cliente; }
    public List<DetalleFactura> getDetalles() { return detalles; }
    public String getClienteDocumento() { return clienteDocumento; }
    public String getEstadoFacturaNombre() {
        return estadoFactura != null ? estadoFactura.getNombre() : "N/A";
    }


    // ===================== SETTERS =====================

    public void setIdFactura(int idFactura) { this.idFactura = idFactura; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }
    public void setIdTurno(int idTurno) { this.idTurno = idTurno; }
    public void setServicios(List<Servicio> servicios) { this.servicios = servicios; }
    public void setMontoTotal(BigDecimal montoTotal) { this.montoTotal = montoTotal; }
    public void setMetodoPago(String metodoPago) { this.metodoPago = metodoPago; }
    public void setFechaHora(LocalDateTime fechaHora) { this.fechaHora = fechaHora; }
    public void setEstadoFactura(EstadoFactura estadoFactura) { this.estadoFactura = estadoFactura; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }

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
                "\nEstado: " + (estadoFactura != null ? estadoFactura.getNombre() : "N/A") +
                "\nFecha: " + (fechaHora != null ? fechaHora.toString() : "N/A");
    }
}
