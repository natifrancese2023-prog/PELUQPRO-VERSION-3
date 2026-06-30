package claseslogicas;

import java.sql.Date;
public class Cliente extends Persona {

    private ClienteRedSocial redSocial;
    private Date fechaAlta;
    private int numeroVisitas;
    private int idCliente;
    private Persona persona;
    public Cliente cliente;


    public Cliente() {
    }

    public ClienteRedSocial getRedSocial() {
        return redSocial;
    }

    public void setRedSocial(ClienteRedSocial redSocial) {
        this.redSocial = redSocial;
    }

    public Date getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(Date fechaAlta) {
        this.fechaAlta = fechaAlta;
    }


    public String getNombreCompleto() {
        return getNombre() + " " + getApellido(); // usa métodos heredados
    }

    public int getIdCliente() {
        return idCliente; // ✅ correcto
    }

    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }
    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;

    }
    public Cliente getCliente() {

        return cliente;
    }

    public void setCliente(Cliente cliente) {
        this.cliente= cliente;}



}