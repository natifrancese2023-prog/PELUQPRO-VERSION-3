package dao;

import claseslogicas.Cliente;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PersonaDAO {

    private static final Logger log = LoggerFactory.getLogger(PersonaDAO.class);

    private static final String SELECT_BARRIO_ID = "SELECT id_barrio FROM barrio WHERE nombre_barrio = ?";
    private static final String SELECT_PROVINCIAS = "SELECT nombre_provincia FROM provincia ORDER BY nombre_provincia";
    private static final String SELECT_CIUDADES_POR_PROVINCIA =
            "SELECT c.nombre_ciudad FROM ciudad c INNER JOIN provincia p ON c.id_provincia = p.id_provincia " +
                    "WHERE p.nombre_provincia = ? ORDER BY c.nombre_ciudad";
    private static final String SELECT_BARRIOS_POR_CIUDAD =
            "SELECT b.nombre_barrio FROM barrio b INNER JOIN ciudad c ON b.id_ciudad = c.id_ciudad " +
                    "WHERE c.nombre_ciudad = ? ORDER BY b.nombre_barrio";
    private static final String INSERT_PERSONA_SQL =
            "INSERT INTO persona (nombre, apellido, telefono, email, calle, numero, id_documento, id_barrio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String UPDATE_PERSONA_SQL =
            "UPDATE persona SET nombre = ?, apellido = ?, telefono = ?, email = ?, calle = ?, numero = ?, id_barrio = ? WHERE id_persona = ?";

    public int obtenerIdBarrio(Connection conn, String nombreBarrio) throws SQLException {
        if (nombreBarrio == null || nombreBarrio.isEmpty()) return -1;
        try (PreparedStatement ps = conn.prepareStatement(SELECT_BARRIO_ID)) {
            ps.setString(1, nombreBarrio);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public int insertar(Connection conn, Cliente cliente, int idDocumento, int idBarrio) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PERSONA_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getCalle());
            ps.setString(6, cliente.getNumero());
            ps.setInt(7, idDocumento);
            ps.setInt(8, idBarrio);
            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        int idGenerado = rs.getInt(1);
                        log.info("Persona insertada con id={}", idGenerado);
                        return idGenerado;
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error al insertar persona cliente={}", cliente.getIdCliente(), e);
            throw e;
        }
        return -1;
    }

    public void actualizar(Connection conn, Cliente cliente, int idBarrio) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_PERSONA_SQL)) {
            ps.setString(1, cliente.getNombre());
            ps.setString(2, cliente.getApellido());
            ps.setString(3, cliente.getTelefono());
            ps.setString(4, cliente.getEmail());
            ps.setString(5, cliente.getCalle());
            ps.setString(6, cliente.getNumero());
            ps.setInt(7, idBarrio);
            ps.setInt(8, cliente.getIdPersona());
            ps.executeUpdate();
            log.info("Persona actualizada id={}", cliente.getIdPersona());
        } catch (SQLException e) {
            log.error("Error al actualizar persona id={}", cliente.getIdPersona(), e);
            throw e;
        }
    }

    public void marcarInactiva(Connection conn, int idPersona) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("UPDATE persona SET activo = false WHERE id_persona = ?")) {
            ps.setInt(1, idPersona);
            ps.executeUpdate();
            log.info("Persona marcada inactiva id={}", idPersona);
        } catch (SQLException e) {
            log.error("Error al marcar persona inactiva id={}", idPersona, e);
            throw e;
        }
    }

    public List<String> obtenerProvincias() throws SQLException {
        return listar(SELECT_PROVINCIAS, null);
    }

    public List<String> obtenerCiudadesPorProvincia(String nombreProvincia) throws SQLException {
        return listar(SELECT_CIUDADES_POR_PROVINCIA, nombreProvincia);
    }

    public List<String> obtenerBarriosPorCiudad(String nombreCiudad) throws SQLException {
        return listar(SELECT_BARRIOS_POR_CIUDAD, nombreCiudad);
    }

    private List<String> listar(String sql, String parametro) throws SQLException {
        List<String> lista = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            if (parametro != null) {
                ps.setString(1, parametro);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    lista.add(rs.getString(1));
                }
            }
        }
        return lista;
    }
}
