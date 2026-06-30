package claseslogicas;
public class Barrio {
    private int idBarrio;
    private String nombreBarrio;
    private Ciudad ciudad;

    public Barrio() {}

    public Barrio(int idBarrio, String nombreBarrio, Ciudad ciudad) {
        this.idBarrio = idBarrio;
        this.nombreBarrio = nombreBarrio;
        this.ciudad = ciudad;
    }

    public int getIdBarrio() {
        return idBarrio;
    }

    public void setIdBarrio(int idBarrio) {
        this.idBarrio = idBarrio;
    }

    public String getNombreBarrio() {
        return nombreBarrio;
    }

    public void setNombreBarrio(String nombreBarrio) {
        this.nombreBarrio = nombreBarrio;
    }

    public Ciudad getCiudad() {
        return ciudad;
    }

    public void setCiudad(Ciudad ciudad) {
        this.ciudad = ciudad;
    }

    @Override
    public String toString() {
        return nombreBarrio + " (" + ciudad + ")";
    }
}