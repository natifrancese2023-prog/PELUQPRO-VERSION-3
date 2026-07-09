package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.math.BigDecimal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReporteFacturacionDAO {

    private static final Logger log = LoggerFactory.getLogger(ReporteFacturacionDAO.class);

    // Corresponden a claseslogicas.EstadoFactura. Se dejan como constantes acá
    // (no se usa el enum directo porque este DAO arma SQL crudo) para no
    // repetir "2"/"4" sueltos en cada query sin explicación.
    private static final int ID_ESTADO_FACTURADA = 2;
    private static final int ID_ESTADO_PAGADA = 4;

    /**
     * FIX (flujo de estado): antes filtraba solo id_estado_factura = 2
     * (FACTURADA). Con el agregado del estado PAGADA, una factura recién
     * cobrada deja de estar en FACTURADA -pasa a PAGADA- y desaparecía de
     * este reporte, dando la sensación de que la facturación bajaba con el
     * tiempo a medida que se cobraban las facturas. "Facturación" tiene que
     * contar toda factura válida (FACTURADA o PAGADA), sin importar si ya se
     * cobró; lo único que se excluye es ANULADA.
     */
    public Map<LocalDate, BigDecimal> obtenerFacturacionPorDia(LocalDate inicio, LocalDate fin) {
        Map<LocalDate, BigDecimal> resultados = new LinkedHashMap<>();
        String sql = "SELECT DATE(fecha_hora) AS fecha, SUM(total) AS total_facturado " +
                "FROM factura " +
                "WHERE fecha_hora >= ? AND fecha_hora < ? " +
                "AND id_estado_factura IN (?, ?) " +
                "GROUP BY DATE(fecha_hora) " +
                "ORDER BY fecha";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1)); // incluye el último día completo
            stmt.setInt(3, ID_ESTADO_FACTURADA);
            stmt.setInt(4, ID_ESTADO_PAGADA);

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
                "LEFT JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "WHERE f.fecha_hora >= ? AND f.fecha_hora < ? " +
                "AND f.id_estado_factura = ? " +
                "GROUP BY mp.nombre_metodo " +
                "ORDER BY cantidad DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1)); // incluye el día final
            stmt.setInt(3, ID_ESTADO_PAGADA);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    String metodo = rs.getString("nombre_metodo");
                    int cantidad = rs.getInt("cantidad");
                    resultados.put(metodo != null ? metodo : "Sin especificar", cantidad);
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener uso de métodos de pago entre {} y {}", inicio, fin, e);
        }

        return resultados;
    }

    public Map<LocalDate, List<String>> obtenerMetodosPorFacturaPorDia(LocalDate inicio, LocalDate fin) {
        Map<LocalDate, List<String>> resultados = new LinkedHashMap<>();
        String sql = "SELECT DATE(f.fecha_hora) AS fecha, mp.nombre_metodo " +
                "FROM factura f " +
                "LEFT JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "WHERE f.fecha_hora >= ? AND f.fecha_hora < ? " +
                "AND f.id_estado_factura = ? " +
                "ORDER BY f.fecha_hora";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setObject(1, inicio);
            stmt.setObject(2, fin.plusDays(1));
            stmt.setInt(3, ID_ESTADO_PAGADA);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LocalDate fecha = rs.getDate("fecha").toLocalDate();
                    String metodo = rs.getString("nombre_metodo");
                    resultados.computeIfAbsent(fecha, k -> new ArrayList<>()).add(metodo);
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener métodos por factura entre {} y {}", inicio, fin, e);
        }

        return resultados;
    }
}