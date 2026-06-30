package claseslogicas;
public class Ciudad {
    private int idCiudad;
    private String nombreCiudad;
    private Provincia provincia;

    public Ciudad() {}

    public Ciudad(int idCiudad, String nombreCiudad, Provincia provincia) {
        this.idCiudad = idCiudad;
        this.nombreCiudad = nombreCiudad;
        this.provincia = provincia;
    }

    public int getIdCiudad() {
        return idCiudad;
    }

    public void setIdCiudad(int idCiudad) {
        this.idCiudad = idCiudad;
    }

    public String getNombreCiudad() {
        return nombreCiudad;
    }

    public void setNombreCiudad(String nombreCiudad) {
        this.nombreCiudad = nombreCiudad;
    }

    public Provincia getProvincia() {
        return provincia;
    }

    public void setProvincia(Provincia provincia) {
        this.provincia = provincia;
    }

    @Override
    public String toString() {
        return nombreCiudad + " (" + provincia + ")";
    }
}