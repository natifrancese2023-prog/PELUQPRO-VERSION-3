package claseslogicas;

import java.time.LocalTime;
import jakarta.persistence.*;

@Entity
@Table(name = "horario_atencion")
public class HorarioAtencion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    private String diaSemana;
    private LocalTime horaApertura;
    private LocalTime horaCierre;





    public HorarioAtencion() {
    }

    public HorarioAtencion(int id, String diaSemana, LocalTime horaApertura, LocalTime horaCierre) {
        this.id = id;
        this.diaSemana = diaSemana;
        this.horaApertura = horaApertura;
        this.horaCierre = horaCierre;
    }



    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }


    public LocalTime getHoraApertura() {
        return horaApertura;
    }


    public LocalTime getHoraCierre() {
        return horaCierre;
    }

}