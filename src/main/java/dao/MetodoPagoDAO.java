package dao;

import claseslogicas.MetodoPago;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetodoPagoDAO {

    private static final Logger log = LoggerFactory.getLogger(MetodoPagoDAO.class);

    public MetodoPago obtenerPorNombre(String nombreMetodo) throws SQLException {
        String sql = "SELECT * FROM metodo_pago WHERE nombre_metodo = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, nombreMetodo);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    MetodoPago metodo = new MetodoPago(
                            rs.getInt("id_metodo"),
                            rs.getString("nombre_metodo"),
                            rs.getDouble("porcentaje_modificador")
                    );
                    log.info("Método de pago encontrado: {}", nombreMetodo);
                    return metodo;
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener método de pago '{}'", nombreMetodo, e);
            throw e;
        }

        return null;
    }
}
