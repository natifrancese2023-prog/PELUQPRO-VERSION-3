package claseslogicas;

import java.time.LocalDate;
import jakarta.persistence.Transient;


public class ServicioTemp {

    private LocalDate fecha;
    private String servicio;
    private String estilista;
    private String observaciones;
    private String estado;

    // Constructor y getters

    public ServicioTemp(LocalDate fecha, String servicio, String estilista, String observaciones, String estado) {
        this.fecha = fecha;
        this.servicio = servicio;
        this.estilista = estilista;
        this.observaciones = observaciones;
        this.estado = estado;
    }

    public LocalDate getFecha() { return fecha; }
    public String getServicio() { return servicio; }
    public String getEstilista() { return estilista; }
    public String getObservaciones() { return observaciones; }
    public String getEstado() { return estado; }
}
