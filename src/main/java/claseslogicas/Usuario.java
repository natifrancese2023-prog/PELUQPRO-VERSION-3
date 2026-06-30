package claseslogicas;
public class Usuario {
    private int id;
    private String usuario;
    private String contrasena;
    private Rol rol;
    private int idEmpleadoFk; // ✅ NUEVO: Para guardar el ID de empleado de la BD

    // Constructor completo actualizado
    public Usuario(int id, String usuario, String contrasena, Rol rol, int idEmpleadoFk) {
        this.id = id;
        this.usuario = usuario;
        this.contrasena = contrasena;
        this.rol = rol;
        this.idEmpleadoFk = idEmpleadoFk;
    }

    public Usuario() {}

    // Getter y Setter para el nuevo campo
    public int getIdEmpleadoFk() {
        return idEmpleadoFk;
    }

    public void setIdEmpleadoFk(int idEmpleadoFk) {
        this.idEmpleadoFk = idEmpleadoFk;
    }

    // ... resto de tus getters y setters ...


    // Getters
    public int getId() {
        return id;
    }

    public String getUsuario() {
        return usuario;
    }


    public Rol getRol() {
        return rol;
    }

    // Setters
    public void setId(int id) {
        this.id = id;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public void setRol(Rol rol) {
        this.rol = rol;
    }

    @Override
    public String toString() {
        return "Usuario{" +
                "id=" + id +
                ", usuario='" + usuario + '\'' +
                ", rol=" + (rol != null ? rol.getNombre() : "Sin rol") +
                '}';
    }


}
