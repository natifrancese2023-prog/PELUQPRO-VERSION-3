package dao;

import claseslogicas.Empleado;
import claseslogicas.Persona;
import claseslogicas.Rol;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EmpleadoDAO {

    private final ConexionBD conexionBD = new ConexionBD();

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

                Empleado emp = new Empleado();
                emp.setIdEmpleado(rs.getInt("id_empleado"));

                // 🔥 PERSONA
                Persona p = new Persona();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));

                emp.setPersona(p);

                // 🔥 FECHA
                Date sqlDate = rs.getDate("fecha_ingreso");
                if (sqlDate != null) {
                    emp.setFechaIngreso(sqlDate.toLocalDate());
                }

                // 🔥 ROL
                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombre(rs.getString("nombre_rol"));
                rol.setEsEstilista(rs.getBoolean("es_estilista"));

                emp.setRol(rol);

                lista.add(emp);
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
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {

                Empleado emp = new Empleado();
                emp.setIdEmpleado(rs.getInt("id_empleado"));

                // 🔥 PERSONA
                Persona p = new Persona();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));

                emp.setPersona(p);

                // 🔥 FECHA
                Date sqlDate = rs.getDate("fecha_ingreso");
                if (sqlDate != null) {
                    emp.setFechaIngreso(sqlDate.toLocalDate());
                }

                // 🔥 ROL
                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombre(rs.getString("nombre_rol"));
                rol.setEsEstilista(rs.getBoolean("es_estilista"));

                emp.setRol(rol);

                return emp;
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
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {

                Empleado emp = new Empleado();
                emp.setIdEmpleado(rs.getInt("id_empleado"));

                // 🔥 PERSONA
                Persona p = new Persona();
                p.setIdPersona(rs.getInt("id_persona"));
                p.setNombre(rs.getString("nombre"));
                p.setApellido(rs.getString("apellido"));

                emp.setPersona(p);

                // 🔥 FECHA
                Date sqlDate = rs.getDate("fecha_ingreso");
                if (sqlDate != null) {
                    emp.setFechaIngreso(sqlDate.toLocalDate());
                }

                // 🔥 ROL
                Rol rol = new Rol();
                rol.setIdRol(rs.getInt("id_rol"));
                rol.setNombre(rs.getString("nombre_rol"));
                rol.setEsEstilista(rs.getBoolean("es_estilista"));

                emp.setRol(rol);

                lista.add(emp);
            }
        }

        return lista;
    }

    // 🔥 OBTENER POR ID PERSONA
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

                    // 🔥 PERSONA
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