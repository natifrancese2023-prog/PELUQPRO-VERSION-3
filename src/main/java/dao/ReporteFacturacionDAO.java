package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReporteFacturacionDAO {

    private static final Logger log = LoggerFactory.getLogger(ReporteFacturacionDAO.class);

    public Map<LocalDate, BigDecimal> obtenerFacturacionPorDia(LocalDate inicio, LocalDate fin) {
        Map<LocalDate, BigDecimal> resultados = new LinkedHashMap<>();
        String sql = "SELECT DATE(fecha_hora) AS fecha, SUM(total) AS total_facturado " +
                "FROM factura " +
                "WHERE fecha_hora >= ? AND fecha_hora < ? " +
                "AND id_estado_factura = 2 " +
                "GROUP BY DATE(fecha_hora) " +
                "ORDER BY fecha";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1)); // incluye el último día completo

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate fecha = rs.getDate("fecha").toLocalDate();
                    BigDecimal total = rs.getBigDecimal("total_facturado");
                    resultados.put(fecha, total != null ? total : BigDecimal.ZERO);
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener facturación entre {} y {}", inicio, fin, e);
        }

        return resultados;
    }

    public Map<String, Integer> obtenerUsoMetodosPago(LocalDate inicio, LocalDate fin) {
        Map<String, Integer> resultados = new LinkedHashMap<>();
        String sql = "SELECT mp.nombre_metodo, COUNT(f.id_factura) AS cantidad " +
                "FROM factura f " +
                "JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "WHERE f.fecha_hora >= ? AND f.fecha_hora < ? " +
                "AND f.id_estado_factura = 2 " +
                "GROUP BY mp.nombre_metodo " +
                "ORDER BY cantidad DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1)); // incluye el día final

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("nombre_metodo");
                    int cantidad = rs.getInt("cantidad");
                    resultados.put(metodo, cantidad);
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener uso de métodos de pago entre {} y {}", inicio, fin, e);
        }

        return resultados;
    }
}
