package claseslogicas;

public class Rol {
    private int idRol;
    private String nombre;
    private boolean esEstilista;

    public Rol() {}



    public String getNombre() {
        return nombre;
    }

    public boolean isEsEstilista() {
        return esEstilista;
    }


    public void setIdRol(int idRol) {
        this.idRol = idRol;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEsEstilista(boolean esEstilista) {
        this.esEstilista = esEstilista;
    }

    @Override
    public String toString() {
        return nombre;
    }
}