package claseslogicas;
public class Documento {
    private int idDocumento;
    private String numeroDocumento;
    private String tipoDocumento;

    public Documento() {}

    public int getIdDocumento() {
        return idDocumento;
    }

    public void setIdDocumento(int idDocumento) {
        this.idDocumento = idDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }



    @Override
    public String toString() {
        return tipoDocumento + ": " + numeroDocumento;
    }

    public void setTipoDocumento(String tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }
}