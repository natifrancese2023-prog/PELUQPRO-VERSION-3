package claseslogicas;

public class ClienteRedSocial {

    private int idCliente;
    private int idTipoRedSocial;
    private String nombreUsuario;
    private String nombreTipoRedSocial;

    public ClienteRedSocial() {}


    public int getIdCliente() {
        return idCliente;
    }
    public void setIdCliente(int idCliente) {
        this.idCliente = idCliente;
    }

    public void setIdTipoRedSocial(int idTipoRedSocial) {
        this.idTipoRedSocial = idTipoRedSocial;
    }

    public String getNombreTipoRedSocial() {
        return nombreTipoRedSocial;
    }
    public void setNombreTipoRedSocial(String nombreTipoRedSocial) {
        this.nombreTipoRedSocial = nombreTipoRedSocial;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }
    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }
}