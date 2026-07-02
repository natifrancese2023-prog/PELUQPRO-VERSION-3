package dao;

import claseslogicas.Empleado;
import claseslogicas.Persona;
import claseslogicas.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    private final ConexionBD conexionBD = new ConexionBD();

    /**
     * Arma un Empleado completo (con su Persona y Rol) a partir de una fila
     * que trae las columnas de empleado + persona + roles ya unidas por JOIN.
     * Antes este mismo bloque estaba copiado en obtenerEstilistas(),
     * obtenerPorId() y obtenerEstilistasPorId(); un cambio en el modelo
     * Empleado obligaba a editar las 3 copias, con el riesgo de que alguna
     * quedara desactualizada.
     */
    private Empleado mapearEmpleado(ResultSet rs) throws SQLException {
        Empleado emp = new Empleado();
        emp.setIdEmpleado(rs.getInt("id_empleado"));

        Persona p = new Persona();
        p.setIdPersona(rs.getInt("id_persona"));
        p.setNombre(rs.getString("nombre"));
        p.setApellido(rs.getString("apellido"));
        emp.setPersona(p);

        Date sqlDate = rs.getDate("fecha_ingreso");
        if (sqlDate != null) {
            emp.setFechaIngreso(sqlDate.toLocalDate());
        }

        Rol rol = new Rol();
        rol.setIdRol(rs.getInt("id_rol"));
        rol.setNombre(rs.getString("nombre_rol"));
        rol.setEsEstilista(rs.getBoolean("es_estilista"));
        emp.setRol(rol);

        return emp;
    }

    // 🔥 OBTENER ESTILISTAS
    public List<Empleado> obtenerEstilistas() throws SQLException {

        String sql = "SELECT e.id_empleado, e.fecha_ingreso, " +
                "p.id_persona, p.nombre, p.apellido, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM empleado e " +
                "JOIN persona p ON e.id_persona = p.id_persona " +
                "JOIN roles r ON e.id_rol = r.id_rol " +
                "WHERE r.es_estilista = true";

        List<Empleado> lista = new ArrayList<>();

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                lista.add(mapearEmpleado(rs));
            }
        }

        return lista;
    }

    // 🔥 OBTENER POR ID
    public Empleado obtenerPorId(int idEmpleado) throws SQLException {

        String sql = "SELECT e.id_empleado, e.fecha_ingreso, " +
                "p.id_persona, p.nombre, p.apellido, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM empleado e " +
                "JOIN persona p ON e.id_persona = p.id_persona " +
                "JOIN roles r ON e.id_rol = r.id_rol " +
                "WHERE e.id_empleado = ?";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpleado);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapearEmpleado(rs);
                }
            }
        }

        return null;
    }

    // 🔥 OBTENER ESTILISTA POR ID (LISTA)
    public List<Empleado> obtenerEstilistasPorId(int idEstilista) throws SQLException {

        String sql = "SELECT e.id_empleado, e.fecha_ingreso, " +
                "p.id_persona, p.nombre, p.apellido, " +
                "r.id_rol, r.nombre_rol, r.es_estilista " +
                "FROM empleado e " +
                "JOIN persona p ON e.id_persona = p.id_persona " +
                "JOIN roles r ON e.id_rol = r.id_rol " +
                "WHERE e.id_empleado = ? AND r.es_estilista = true";

        List<Empleado> lista = new ArrayList<>();

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEstilista);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(mapearEmpleado(rs));
                }
            }
        }

        return lista;
    }

    // 🔥 OBTENER POR ID PERSONA
    // (Consulta distinta y más liviana que las anteriores: solo trae
    // id_empleado/id_persona, sin JOIN a roles, por eso no usa mapearEmpleado)
    public Empleado obtenerPorIdPersona(int idPersona) {

        Empleado empleado = null;

        String sql = "SELECT id_empleado, id_persona FROM empleado WHERE id_persona = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idPersona);

            try (ResultSet rs = ps.executeQuery()) {

                if (rs.next()) {

                    empleado = new Empleado();
                    empleado.setIdEmpleado(rs.getInt("id_empleado"));

                    Persona p = new Persona();
                    p.setIdPersona(rs.getInt("id_persona"));

                    empleado.setPersona(p);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error: " + e.getMessage());
        }

        return empleado;
    }
}