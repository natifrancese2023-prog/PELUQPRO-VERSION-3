package claseslogicas;

import java.math.BigDecimal;
import java.time.LocalDate;

public class ClienteReporteExtendido {
    private int idPersona;
    private String nombreCompleto;
    private String telefono;
    private String email;
    private String direccion;
    private LocalDate fechaAlta;
    private int cantidadVisitas;
    private BigDecimal gastoTotal;   // ✅ ahora BigDecimal
    private String estadoUltimoTurno;
    private String redesSociales;

    public ClienteReporteExtendido(int idPersona, String nombreCompleto, String telefono, String email,
                                   String direccion, LocalDate fechaAlta, int cantidadVisitas,
                                   BigDecimal gastoTotal, String estadoUltimoTurno, String redesSociales) {
        this.idPersona = idPersona;
        this.nombreCompleto = nombreCompleto;
        this.telefono = telefono;
        this.email = email;
        this.direccion = direccion;
        this.fechaAlta = fechaAlta;
        this.cantidadVisitas = cantidadVisitas;
        this.gastoTotal = gastoTotal != null ? gastoTotal : BigDecimal.ZERO;
        this.estadoUltimoTurno = estadoUltimoTurno;
        this.redesSociales = redesSociales;
    }

    // Getters

    public int getIdPersona() { return idPersona; }
    public String getNombreCompleto() { return nombreCompleto; }
    public String getTelefono() { return telefono; }
    public String getEmail() { return email; }
    public String getDireccion() { return direccion; }
    public LocalDate getFechaAlta() { return fechaAlta; }
    public int getCantidadVisitas() { return cantidadVisitas; }
    public BigDecimal getGastoTotal() { return gastoTotal; }   // ✅ devuelve BigDecimal
    public String getEstadoUltimoTurno() { return estadoUltimoTurno; }
    public String getRedesSociales() { return redesSociales; }
}
