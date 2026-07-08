package claseslogicas;

import java.time.LocalDateTime;

import java.util.List;


public class Visita {

    private int idVisita;
    private int idCliente;
    private int idTurno;
    private Empleado empleado;
    private List<Servicio> serviciosRealizados;
    private LocalDateTime fechaHoraCierre;
    private int idEstilista;
    private Cliente cliente;


    // Constructor vacío
    public Visita() {

    }



    // Constructor completo
    public Visita(int idVisita, int idCliente, LocalDateTime fechaHoraCierre, List<Integer> idsServicios) {
        this.idVisita = idVisita;
        this.idCliente = idCliente;
        this.fechaHoraCierre = fechaHoraCierre;

        this.serviciosRealizados = new java.util.ArrayList<>();
        for (Integer id : idsServicios) {
            Servicio servicio = new Servicio();
            servicio.setIdServicio(id);
            this.serviciosRealizados.add(servicio);
        }
    }


    public int getIdVisita() {
        return idVisita;
    }

    public void setIdVisita(int idVisita) {
        this.idVisita = idVisita;
    }

    public int getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public int getIdTurno() {
        return idTurno;
    }

    public void setIdTurno(int idTurno) {
        this.idTurno = idTurno;
    }
    public Empleado getEmpleado() {
        return empleado;
    }

    public void setEmpleado(Empleado empleado) {
        this.empleado = empleado;
    }

    public List<Servicio> getServiciosRealizados() {
        return serviciosRealizados;
    }

    public void setServiciosRealizados(List<Servicio> serviciosRealizados) {
        this.serviciosRealizados = serviciosRealizados;
    }





    public LocalDateTime getFechaHoraCierre() {
        return fechaHoraCierre;
    }

    public void setFechaHoraCierre(LocalDateTime fechaHoraCierre) {
        this.fechaHoraCierre = fechaHoraCierre;
    }


    public void setIdEstilista(int idEstilista) {
        this.idEstilista = idEstilista;
    }


    public void setCliente(Cliente cliente) {
        this.cliente=cliente;
    }
    public Cliente getCliente() {
        return cliente;
    }

}