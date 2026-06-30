package dao;

import claseslogicas.Usuario;
import claseslogicas.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UsuarioDao {

    private final ConexionBD conexionBD = new ConexionBD();

    public Usuario validarUsuario(String usuario, String contrasena) {
        Usuario user = null;

        // 🏆 Agregamos u.id_empleado_fk a la consulta SQL
        String sql = "SELECT u.id, u.usuario, u.contrasena, u.id_empleado_fk, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM usuarios u " +
                "JOIN roles r ON u.id_rol_fk = r.id_rol " +
                "WHERE u.usuario = ? AND u.contrasena = ?";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            System.out.println("✅ Intentando conexión para usuario: " + usuario);
            stmt.setString(1, usuario);
            stmt.setString(2, contrasena);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                System.out.println("✅ Usuario validado correctamente.");

                // 1. Mapeo del Rol
                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombre(rs.getString("nombre_rol"));
                rol.setEsEstilista(rs.getBoolean("es_estilista"));

                // 2. Mapeo del Usuario
                user = new Usuario();
                user.setId(rs.getInt("id"));
                user.setUsuario(rs.getString("usuario"));
                user.setContrasena(rs.getString("contrasena"));
                user.setRol(rol);

                // 🏆 3. Mapeo del ID de Empleado (El vínculo que evita que se rompa el sistema)
                // Asegúrate de que en la clase Usuario el método se llame así:
                user.setIdEmpleadoFk(rs.getInt("id_empleado_fk"));

            } else {
                System.out.println("⚠️ Credenciales incorrectas para: " + usuario);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error en la base de datos: " + e.getMessage());
            e.printStackTrace();
        }

        return user;
    }
}