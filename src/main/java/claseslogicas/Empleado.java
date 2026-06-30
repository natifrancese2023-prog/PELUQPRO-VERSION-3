package claseslogicas;

import java.time.LocalDate;

public class Empleado extends Persona {

    private boolean esEstilista;
    private LocalDate fechaIngreso;
    private String puesto;
    private int idEmpleado;
    public Persona persona;

    public Empleado() {
        super();
    }

    public Empleado(int idPersona, String nombre, String apellido, boolean esEstilista) {
        super();
        super.setIdPersona(idPersona);
        super.setNombre(nombre);
        super.setApellido(apellido);
        this.esEstilista = esEstilista;
    }

    public String getNombreCompleto() {
        return getNombre() + " " + getApellido();
    }

    @Override

    public String toString() {
        // Si el objeto 'persona' tiene los datos, los usamos.
        // Si no, usamos los de la herencia.
        if (this.persona != null) {
            return this.persona.getNombre() + " " + this.persona.getApellido();
        }
        return getNombre() + " " + getApellido();
    }

    public void setFechaIngreso(LocalDate fechaIngreso) {
        this.fechaIngreso = fechaIngreso;
    }

    public void setRol(Rol rol) {
    }

    public void setIdEmpleado(int idEmpleado) {
        this.idEmpleado=idEmpleado;
    }


    public int getIdEmpleado() {
        return idEmpleado; // ✅ Esto es correcto
    }

    public Persona getPersona() {
        return persona;
    }

    public void setPersona(Persona persona) {
        this.persona = persona;
    }



}