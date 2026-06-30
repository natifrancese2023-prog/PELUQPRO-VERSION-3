package clasesreportes;

import java.time.LocalDate;

public class ClienteReporteExtendido {
    private int idPersona;
    private String nombreCompleto;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDate fechaAlta;
    private int cantidadVisitas;
    private double gastoTotal;
    private String estadoUltimoTurno;
    private String redesSociales;

    public ClienteReporteExtendido(int idPersona, String nombreCompleto, String telefono, String email,
                                   String direccion, LocalDate fechaAlta, int cantidadVisitas,
                                   double gastoTotal, String estadoUltimoTurno, String redesSociales) {
        this.idPersona = idPersona;
        this.nombreCompleto = nombreCompleto;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.fechaAlta = fechaAlta;
        this.cantidadVisitas = cantidadVisitas;
        this.gastoTotal = gastoTotal;
        this.estadoUltimoTurno = estadoUltimoTurno;
        this.redesSociales = redesSociales;
    }

    // Getters

    public String getNombreCompleto() { return nombreCompleto; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }

    public int getCantidadVisitas() { return cantidadVisitas; }
    public double getGastoTotal() { return gastoTotal; }
    public String getEstadoUltimoTurno() { return estadoUltimoTurno; }

}
