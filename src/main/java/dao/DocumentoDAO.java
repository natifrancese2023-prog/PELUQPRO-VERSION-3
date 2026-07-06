package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a la tabla documento. Extraído de ClienteDAO, que antes concentraba
 * también esta responsabilidad además de persona, red_social y cliente.
 * Los métodos que participan de una transacción más grande (insertar,
 * obtenerIdTipoDocumento, existeClienteConDocumento) reciben la Connection
 * ya abierta por el que llama (ClienteDAO), para no romper la atomicidad.
 */
public class DocumentoDAO {

    private static final String SELECT_TIPO_DOCUMENTO_ID =
            "SELECT id_tipo_documento FROM tipo_documento WHERE tipo_documento = ?";
    private static final String SELECT_TIPOS_DOCUMENTO =
            "SELECT tipo_documento FROM tipo_documento ORDER BY tipo_documento";
    private static final String INSERT_DOCUMENTO_SQL =
            "INSERT INTO documento (numero_documento, id_tipo_documento) VALUES (?, ?)";
    private static final String SELECT_CLIENTE_EXISTENTE_POR_DOCUMENTO =
            "SELECT c.id_cliente " +
                    "FROM cliente c " +
                    "JOIN persona p ON c.id_persona = p.id_persona " +
                    "JOIN documento d ON p.id_documento = d.id_documento " +
                    "WHERE d.numero_documento = ? AND d.id_tipo_documento = ?";

    public int obtenerIdTipoDocumento(Connection conn, String nombreTipoDocumento) throws SQLException {
        if (nombreTipoDocumento == null || nombreTipoDocumento.isEmpty()) return -1;
        try (PreparedStatement ps = conn.prepareStatement(SELECT_TIPO_DOCUMENTO_ID)) {
            ps.setString(1, nombreTipoDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public boolean existeClienteConDocumento(Connection conn, String numeroDocumento, int idTipoDocumento) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_CLIENTE_EXISTENTE_POR_DOCUMENTO)) {
            ps.setString(1, numeroDocumento);
            ps.setInt(2, idTipoDocumento);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta un documento y devuelve su id generado, o -1 si falló. */
    public int insertar(Connection conn, String numeroDocumento, int idTipoDocumento) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DOCUMENTO_SQL, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, numeroDocumento);
            ps.setInt(2, idTipoDocumento);
            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    public List<String> obtenerTiposDocumento() throws SQLException {
        List<String> tipos = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_TIPOS_DOCUMENTO);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                tipos.add(rs.getString("tipo_documento"));
            }
        }
        return tipos;
    }
}