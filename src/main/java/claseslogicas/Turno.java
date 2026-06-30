package claseslogicas;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Turno {

    private int idTurno;
    private int idCliente;
    private int idEmpleado;
    private int idEstado;
    private EstadoTurno estadoTurno;

    private LocalDate fecha;
    private LocalTime horaInicio;
    private LocalTime horaFin;
    private String observaciones;
    private LocalDateTime fechaCreacion;
    private String motivoLog;

    private Cliente cliente;
    private Empleado empleado;
    private String nombreEstado;

    // NUEVOS CAMPOS
    private String clienteDocumento;
    private String empleadoDocumento;
    private int idServicio; // opcional, si querés guardar el último servicio asociado
    private List<Servicio> servicios;


    public Turno() {
        servicios = new ArrayList<>();
    }

    public Turno(int idTurno, int idCliente, int idEmpleado, int idEstado, LocalDate fecha,
                 LocalTime horaInicio, LocalTime horaFin, String observaciones,
                 LocalDateTime fechaCreacion, String motivoLog) {
        this.idTurno = idTurno;
        this.idCliente = idCliente;
        this.idEmpleado = idEmpleado;
        this.idEstado = idEstado;
        this.fecha = fecha;
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.observaciones = observaciones;
        this.fechaCreacion = fechaCreacion;
        this.motivoLog = motivoLog;
        this.servicios = new ArrayList<>();
    }

    // ===================== MÉTODOS DE ESTADO =====================

    public boolean puedeCambiarA(EstadoTurno nuevoEstado) {
        EstadoTurno actual = getEstadoLogico();
        return switch (actual) {
            case PENDIENTE -> (nuevoEstado == EstadoTurno.CONFIRMADO || nuevoEstado == EstadoTurno.CANCELADO);
            case CONFIRMADO -> (nuevoEstado == EstadoTurno.FINALIZADO || nuevoEstado == EstadoTurno.CANCELADO);
            case FINALIZADO -> (nuevoEstado == EstadoTurno.FACTURADO);
            default -> false; // CANCELADO y FACTURADO son terminales
        };
    }

    public boolean cambiarEstado(EstadoTurno nuevoEstado, String motivo) {
        if (!puedeCambiarA(nuevoEstado)) {
            return false;
        }
        setEstadoLogico(nuevoEstado);
        setMotivoLog(motivo);
        return true;
    }

    public EstadoTurno getEstadoLogico() {
        return EstadoTurno.fromId(this.idEstado);
    }

    public void setEstadoLogico(EstadoTurno estado) {
        this.idEstado = estado.getId();
        this.nombreEstado = estado.getNombre();
    }


    public void addServicio(Servicio servicio) {
        if (this.servicios == null) {
            this.servicios = new ArrayList<>();
        }
        this.servicios.add(servicio);
    }


    public String getResumenServicios() {
        if (servicios == null || servicios.isEmpty()) {
            return "Sin servicios definidos";
        }
        StringBuilder sb = new StringBuilder();
        for (Servicio s : servicios) {
            sb.append(s.getNombreServicio()).append(", ");
        }
        return sb.substring(0, sb.length() - 2);
    }


    public int getIdTurno() { return idTurno; }
    public void setIdTurno(int idTurno) { this.idTurno = idTurno; }

    public int getIdCliente() { return idCliente; }
    public void setIdCliente(int idCliente) { this.idCliente = idCliente; }

    public int getIdEmpleado() { return idEmpleado; }
    public void setIdEmpleado(int idEmpleado) { this.idEmpleado = idEmpleado; }

    public void setIdEstado(int idEstado) {
        this.idEstado = idEstado;
        this.setEstadoLogico(EstadoTurno.fromId(idEstado));
    }

    public String getMotivoLog() { return motivoLog; }
    public void setMotivoLog(String motivoLog) { this.motivoLog = motivoLog; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public LocalTime getHoraInicio() { return horaInicio; }
    public void setHoraInicio(LocalTime horaInicio) { this.horaInicio = horaInicio; }

    public LocalTime getHoraFin() { return horaFin; }
    public void setHoraFin(LocalTime horaFin) { this.horaFin = horaFin; }

    public String getObservaciones() { return observaciones; }
    public void setObservaciones(String observaciones) { this.observaciones = observaciones; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public Cliente getCliente() { return cliente; }
    public void setCliente(Cliente cliente) { this.cliente = cliente; }

    public Empleado getEmpleado() { return empleado; }
    public void setEmpleado(Empleado empleado) { this.empleado = empleado; }



    public List<Servicio> getServicios() { return servicios; }
    public void setServicios(List<Servicio> servicios) { this.servicios = servicios; }

    public String getClienteDocumento() { return clienteDocumento; }
    public void setClienteDocumento(String clienteDocumento) { this.clienteDocumento = clienteDocumento; }

    public String getEmpleadoDocumento() { return empleadoDocumento; }
    public void setEmpleadoDocumento(String empleadoDocumento) { this.empleadoDocumento = empleadoDocumento; }



    public String getEstilista() {
        return (empleado != null) ? empleado.getNombreCompleto() : "N/A";
    }

    public void setEstilista(Empleado estilista) { this.empleado = estilista; }

    public String getEstado() {
        return (nombreEstado != null) ? nombreEstado : "N/A";
    }

    public void setEstadoTurno(EstadoTurno estadoTurno) {
        this.estadoTurno = estadoTurno;
    }

    public EstadoTurno getEstadoTurno() {
        return estadoTurno;
    }

    @Override
    public String toString() {
        String estado = (estadoTurno != null) ? estadoTurno.getNombre() : "Sin estado";
        return fecha + " | " + horaInicio + " hs | Estado: " + estado;
    }
}
