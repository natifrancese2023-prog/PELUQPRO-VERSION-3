package dao;

import claseslogicas.Usuario;
import claseslogicas.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utilidades.SeguridadUtil; // nuestra clase con BCrypt

public class UsuarioDao {

    private final ConexionBD conexionBD = new ConexionBD();

    public Usuario validarUsuario(String usuario, String contrasenaIngresada) {
        Usuario user = null;

        // ⚠️ Ya NO filtramos por contraseña en la SQL, solo por usuario
        String sql = "SELECT u.id, u.usuario, u.contrasena, u.id_empleado_fk, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM usuarios u " +
                "JOIN roles r ON u.id_rol_fk = r.id_rol " +
                "WHERE u.usuario = ?";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            System.out.println("✅ Intentando conexión para usuario: " + usuario);
            stmt.setString(1, usuario);

            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String hashAlmacenado = rs.getString("contrasena");

                // 🔑 Verificamos la contraseña ingresada contra el hash
                if (SeguridadUtil.verificarPassword(contrasenaIngresada, hashAlmacenado)) {
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
                    user.setContrasena(hashAlmacenado); // guardamos el hash, no el texto plano
                    user.setRol(rol);

                    // 3. Mapeo del ID de Empleado
                    user.setIdEmpleadoFk(rs.getInt("id_empleado_fk"));
                } else {
                    System.out.println("⚠️ Contraseña incorrecta para usuario: " + usuario);
                }
            } else {
                System.out.println("⚠️ Usuario no encontrado: " + usuario);
            }

        } catch (SQLException e) {
            System.err.println("❌ Error en la base de datos: " + e.getMessage());
            e.printStackTrace();
        }

        return user;
    }
}
