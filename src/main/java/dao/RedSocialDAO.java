package dao;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedSocialDAO {

    private static final Logger log = LoggerFactory.getLogger(RedSocialDAO.class);

    private static final String SELECT_TIPOS_RED_SOCIAL_ID =
            "SELECT id_tipo_red_social FROM tipos_red_social WHERE tipo_red_social = ?";
    private static final String SELECT_TIPOS_RED_SOCIAL =
            "SELECT tipo_red_social FROM tipos_red_social ORDER BY tipo_red_social";
    private static final String INSERT_RED_SOCIAL_SQL =
            "INSERT INTO red_social (nombre_usuario, id_tipo_red_social, id_cliente) VALUES (?, ?, ?)";
    private static final String UPDATE_RED_SOCIAL_SQL =
            "UPDATE red_social SET nombre_usuario = ?, id_tipo_red_social = ? WHERE id_cliente = ?";
    private static final String DELETE_RED_SOCIAL_SQL =
            "DELETE FROM red_social WHERE id_cliente = ?";
    private static final String SELECT_RED_SOCIAL_EXISTENTE =
            "SELECT id_cliente FROM red_social WHERE id_cliente = ?";
    private static final String SELECT_REDES_SOCIAL_POR_CLIENTE =
            "SELECT rs.nombre_usuario, trs.tipo_red_social " +
                    "FROM red_social rs " +
                    "JOIN tipos_red_social trs ON rs.id_tipo_red_social = trs.id_tipo_red_social " +
                    "WHERE rs.id_cliente = ?";

    public int obtenerIdTipoRedSocial(Connection conn, String nombreTipoRedSocial) throws SQLException {
        if (nombreTipoRedSocial == null || nombreTipoRedSocial.isEmpty()) return -1;
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TIPOS_RED_SOCIAL_ID)) {
            ps.setString(1, nombreTipoRedSocial);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void insertar(Connection conn, String nombreUsuario, int idTipoRedSocial, int idCliente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_RED_SOCIAL_SQL)) {
            ps.setString(1, nombreUsuario);
            ps.setInt(2, idTipoRedSocial);
            ps.setInt(3, idCliente);
            if (ps.executeUpdate() == 0) {
                throw new SQLException("No se pudo insertar la red social.");
            }
            log.info("Red social insertada para cliente={} tipo={}", idCliente, idTipoRedSocial);
        }
    }

    public boolean existePorCliente(Connection conn, int idCliente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_RED_SOCIAL_EXISTENTE)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void actualizar(Connection conn, Cliente cliente) throws SQLException {
        ClienteRedSocial rsCliente = cliente.getRedSocial();
        int idCliente = cliente.getIdCliente();

        if (rsCliente == null || rsCliente.getNombreUsuario() == null || rsCliente.getNombreUsuario().isEmpty()) {
            eliminarPorCliente(conn, idCliente);
            return;
        }

        int idTipoRedSocial = obtenerIdTipoRedSocial(conn, rsCliente.getNombreTipoRedSocial());
        if (idTipoRedSocial == -1) {
            throw new SQLException("Error: Tipo de Red Social '" + rsCliente.getNombreTipoRedSocial() + "' no encontrado.");
        }

        if (existePorCliente(conn, idCliente)) {
            try (PreparedStatement ps = conn.prepareStatement(UPDATE_RED_SOCIAL_SQL)) {
                ps.setString(1, rsCliente.getNombreUsuario());
                ps.setInt(2, idTipoRedSocial);
                ps.setInt(3, idCliente);
                ps.executeUpdate();
                log.info("Red social actualizada para cliente={} tipo={}", idCliente, idTipoRedSocial);
            }
        } else {
            insertar(conn, rsCliente.getNombreUsuario(), idTipoRedSocial, idCliente);
        }
    }

    public void eliminarPorCliente(Connection conn, int idCliente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_RED_SOCIAL_SQL)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
            log.info("Red social eliminada para cliente={}", idCliente);
        }
    }

    public ClienteRedSocial consultarPorCliente(Connection conn, int idCliente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_REDES_SOCIAL_POR_CLIENTE)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    ClienteRedSocial redSocial = new ClienteRedSocial();
                    redSocial.setNombreUsuario(rs.getString("nombre_usuario"));
                    redSocial.setNombreTipoRedSocial(rs.getString("tipo_red_social"));
                    return redSocial;
                }
            }
        }
        return null;
    }

    public List<String> obtenerTiposRedSocial() throws SQLException {
        List<String> tipos = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_TIPOS_RED_SOCIAL);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.add(rs.getString("tipo_red_social"));
            }
        }
        return tipos;
    }
}
