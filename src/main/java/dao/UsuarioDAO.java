package dao;

import claseslogicas.Usuario;
import claseslogicas.Rol;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import utilidades.SeguridadUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDAO.class);

    public Usuario validarUsuario(String usuario, String contrasenaIngresada) {
        Usuario user = null;

        String sql = "SELECT u.id, u.usuario, u.contrasena, u.id_empleado_fk, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM usuarios u " +
                "JOIN roles r ON u.id_rol_fk = r.id_rol " +
                "WHERE u.usuario = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            log.info("Intentando conexión para usuario {}", usuario);
            stmt.setString(1, usuario);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String hashAlmacenado = rs.getString("contrasena");

                    if (SeguridadUtil.verificarPassword(contrasenaIngresada, hashAlmacenado)) {
                        log.info("Usuario validado correctamente: {}", usuario);

                        Rol rol = new Rol();
                        rol.setIdRol(rs.getInt("id_rol"));
                        rol.setNombre(rs.getString("nombre_rol"));
                        rol.setEsEstilista(rs.getBoolean("es_estilista"));

                        user = new Usuario();
                        user.setId(rs.getInt("id"));
                        user.setUsuario(rs.getString("usuario"));
                        user.setContrasena(hashAlmacenado);
                        user.setRol(rol);
                        user.setIdEmpleadoFk(rs.getInt("id_empleado_fk"));
                    } else {
                        log.warn("Contraseña incorrecta para usuario {}", usuario);
                    }
                } else {
                    log.warn("Usuario no encontrado: {}", usuario);
                }
            }

        } catch (SQLException e) {
            log.error("Error en la base de datos al validar usuario {}", usuario, e);
        }

        return user;
    }
}
