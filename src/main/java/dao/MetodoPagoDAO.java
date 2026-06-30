package dao;

import claseslogicas.MetodoPago;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MetodoPagoDAO {

    public MetodoPago obtenerPorNombre(String nombreMetodo) throws SQLException {
        // 🚨 CORREGIDO: De 'metodos_pago' a 'metodo_pago'
        String sql = "SELECT * FROM metodo_pago WHERE nombre_metodo = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, nombreMetodo);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new MetodoPago(
                        rs.getInt("id_metodo"),
                        rs.getString("nombre_metodo"),
                        rs.getDouble("porcentaje_modificador")
                );
            }
        }
        return null;
    }
}
