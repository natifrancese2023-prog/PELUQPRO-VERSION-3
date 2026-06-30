package claseslogicas;
public class EstadoFactura {
    private int idEstadoFactura;
    private String nombre;

    public EstadoFactura(int id, String nombre) {
        this.idEstadoFactura = id;
        this.nombre = nombre;
    }

    public int getIdEstadoFactura() { return idEstadoFactura; }
    public String getNombre() { return nombre; }
}
