package claseslogicas;

import claseslogicas.Empleado;
import java.time.LocalTime;

/**
 precentamos al usuario los horarios validos
 */
public class BloqueDisponible {

    private LocalTime horaInicio;
    private LocalTime horaFin;
    private Empleado estilista; // El estilista que está disponible en ese bloque

    public BloqueDisponible(LocalTime horaInicio, LocalTime horaFin, Empleado estilista) {
        this.horaInicio = horaInicio;
        this.horaFin = horaFin;
        this.estilista = estilista;
    }

    public String getResumenBloque() {
        return estilista.getNombreCompleto() +
                " | De " + horaInicio.toString() +
                " a " + horaFin.toString();
    }


    public LocalTime getHoraInicio()
    {
        return horaInicio;
    }

    public LocalTime getHoraFin()
    { //
        return horaFin;
    }

    public Empleado getEstilista() {
        return estilista;
    }






    public void setEstilista(Empleado estilista)
    {
        this.estilista = estilista;
    }

    @Override

    public String toString() {
        String nombreMostrable = "Estilista";

        if (estilista != null) {
            // Intentamos sacar el nombre de la herencia o del objeto interno persona
            if (estilista.getNombreCompleto() != null && !estilista.getNombreCompleto().equals("null null")) {
                nombreMostrable = estilista.getNombreCompleto();
            } else if (estilista.getPersona() != null) {
                nombreMostrable = estilista.getPersona().getNombre() + " " + estilista.getPersona().getApellido();
            }
        }

        return nombreMostrable + " | " + horaInicio.toString() + " - " + horaFin.toString();
    }
}