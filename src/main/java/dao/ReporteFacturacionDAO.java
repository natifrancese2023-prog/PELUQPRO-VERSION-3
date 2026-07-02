package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.math.BigDecimal;

public class ReporteFacturacionDAO {
    private final ConexionBD conexionBD = new ConexionBD();

    public Map<LocalDate, BigDecimal> obtenerFacturacionPorDia(LocalDate inicio, LocalDate fin) {
        Map<LocalDate, BigDecimal> resultados = new LinkedHashMap<>();
        String sql = "SELECT DATE(fecha_hora) AS fecha, SUM(total) AS total_facturado " +
                "FROM factura " +
                "WHERE fecha_hora >= ? AND fecha_hora < ? " +
                "AND id_estado_factura = 2 " +
                "GROUP BY DATE(fecha_hora) " +
                "ORDER BY fecha";
        try (Connection conn = conexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            // Se usa fin.plusDays(1) para incluir el último día completamente
            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1));

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                LocalDate fecha = rs.getDate("fecha").toLocalDate();
                BigDecimal total = rs.getBigDecimal("total_facturado");
                resultados.put(fecha, total != null ? total : BigDecimal.ZERO);
            }

        } catch (Exception e) {
            e.printStackTrace();
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
        try (Connection conn = conexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1)); // incluye el día final

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String metodo = rs.getString("nombre_metodo");
                int cantidad = rs.getInt("cantidad");
                resultados.put(metodo, cantidad);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return resultados;
    }
}
