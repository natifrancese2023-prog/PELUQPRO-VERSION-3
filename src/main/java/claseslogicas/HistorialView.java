package claseslogicas;

import java.time.LocalDateTime;


public class HistorialView {

    private LocalDateTime fechaHora;
    private String nombreEstilista;
    private String nombreServicio;
    private String observaciones;

    private int idVisita;

    // Constructor vacío
    public HistorialView() {
    }


    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(LocalDateTime fechaHora) {
        this.fechaHora = fechaHora;
    }


    public String getNombreEstilista() {
        return nombreEstilista;
    }

    public void setNombreEstilista(String nombreEstilista) {
        this.nombreEstilista = nombreEstilista;
    }


    public String getNombreServicio() {
        return nombreServicio;
    }

    public void setNombreServicio(String nombreServicio) {
        this.nombreServicio = nombreServicio;
    }


    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }


    public void setIdVisita(int idVisita) {
        this.idVisita = idVisita;
    }
}