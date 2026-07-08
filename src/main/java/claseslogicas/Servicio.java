package claseslogicas;

public class Servicio {


    private int idServicio; // PK
    private String nombreServicio;
    private String descripcion;
    private int duracionMinutos;
    private int idTurno;

    private double precio;

    // Getters y Setters...


    public Servicio() {
    }


    public Servicio(int idTipoServicio, String nombreServicio, String descripcion, int duracionMinutos, double precio) {
        this.idServicio = idTipoServicio;
        this.nombreServicio = nombreServicio;
        this.descripcion = descripcion;
        this.duracionMinutos = duracionMinutos;
        this.precio = precio;
    }

    public String getNombreConDuracion() {
        return this.nombreServicio + " (" + this.duracionMinutos + " min)";
    }



    @Override
    public String toString() {
        return this.nombreServicio;
    }

    // --- GETTERS Y SETTERS ---

    public int getIdTipoServicio() { return idServicio; }
    public void setIdTipoServicio(int idTipoServicio) { this.idServicio = idTipoServicio; }

    public String getNombreServicio() { return nombreServicio; }
    public void setNombreServicio(String nombreServicio) { this.nombreServicio = nombreServicio; }

    public String getDescripcion() { return descripcion; }


    public int getDuracionMinutos() { return duracionMinutos; }


    public double getPrecio() { return precio; }
    public void setPrecio(double precio) { this.precio = precio; }

    public int getIdServicio() { return  idServicio;
    }

    public int getIdTurno() {

        return idTurno;
    }
    public void setIdTurno(int idTurno) {
        this.idTurno = idTurno;
    }

    public void setIdServicio(Integer id) {
    }
}