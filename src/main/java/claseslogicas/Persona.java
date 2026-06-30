package claseslogicas;


public class Persona {

    // === Atributos comunes ===
    protected int idPersona;
    protected String nombre;
    protected String apellido;
    protected String telefono;
    protected String email;

    // === Dirección ===
    protected String calle;
    protected String numero;
    protected int idBarrio;
    protected String nombreBarrio;
    protected String nombreCiudad;
    protected String nombreProvincia;

    // === Documento ===
    protected int idDocumento;
    protected String nombreTipoDocumento;
    protected String numeroDocumento;

    // === Constructores ===
    public Persona() {
    }

    public Persona(String nombre, String apellido, String telefono, String email,
                   String calle, String numero, int idDocumento, int idBarrio) {
        this.nombre = nombre;
        this.apellido = apellido;
        this.telefono = telefono;
        this.email = email;
        this.calle = calle;
        this.numero = numero;
        this.idDocumento = idDocumento;
        this.idBarrio = idBarrio;
    }

    // === Getters ===
    public int getIdPersona() {
        return idPersona;
    }

    public String getNombre() {
        return nombre;
    }

    public String getApellido() {
        return apellido;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getEmail() {
        return email;
    }

    public String getCalle() {
        return calle;
    }

    public String getNumero() {
        return numero;
    }


    public String getNombreBarrio() {
        return nombreBarrio;
    }

    public String getNombreCiudad() {
        return nombreCiudad;
    }

    public String getNombreProvincia() {
        return nombreProvincia;
    }



    public String getNombreTipoDocumento() {
        return nombreTipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    // === Setters ===
    public void setIdPersona(int idPersona) {
        this.idPersona = idPersona;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setCalle(String calle) {
        this.calle = calle;
    }

    public void setNumero(String numero) {
        this.numero = numero;
    }

    public void setIdBarrio(int idBarrio) {
        this.idBarrio = idBarrio;
    }

    public void setNombreBarrio(String nombreBarrio) {
        this.nombreBarrio = nombreBarrio;
    }

    public void setNombreCiudad(String nombreCiudad) {
        this.nombreCiudad = nombreCiudad;
    }

    public void setNombreProvincia(String nombreProvincia) {
        this.nombreProvincia = nombreProvincia;
    }



    public void setNombreTipoDocumento(String nombreTipoDocumento) {
        this.nombreTipoDocumento = nombreTipoDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    @Override
    public String toString() {
        return nombre + " " + apellido;
    }


}