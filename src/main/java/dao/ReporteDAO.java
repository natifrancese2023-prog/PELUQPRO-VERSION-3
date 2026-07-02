package dao;

import claseslogicas.ClienteReporteExtendido;
import dao.ConexionBD;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ReporteDAO {

    public List<ClienteReporteExtendido> obtenerDatosClientesExtendido() throws SQLException {
        List<ClienteReporteExtendido> lista = new ArrayList<>();

        String sql = """
    SELECT
        p.id_persona,
        CONCAT(p.nombre, ' ', p.apellido) AS nombre_completo,
        p.telefono,
        p.email,
        CONCAT(p.calle, ' ', p.numero) AS direccion,
        c.fecha_alta,
        COALESCE(v.cantidad_visitas, 0) AS cantidad_visitas,
        COALESCE(f.gasto_total, 0) AS gasto_total,
        ut.nombre_estado AS estado_ultimo_turno,
        rs.red_social
    FROM persona p
    JOIN cliente c ON p.id_persona = c.id_persona
    LEFT JOIN (
        SELECT id_cliente, COUNT(*) AS cantidad_visitas
        FROM visita
        GROUP BY id_cliente
    ) v ON v.id_cliente = c.id_cliente
    LEFT JOIN (
        SELECT id_cliente, SUM(total) AS gasto_total
        FROM factura
        GROUP BY id_cliente
    ) f ON f.id_cliente = c.id_cliente
    LEFT JOIN (
        SELECT id_cliente, STRING_AGG(nombre_usuario, ', ') AS red_social
        FROM red_social
        GROUP BY id_cliente
    ) rs ON rs.id_cliente = c.id_cliente
    LEFT JOIN LATERAL (
        SELECT et.nombre_estado
        FROM turno t
        JOIN estado et ON et.id_estado = t.id_estado
        WHERE t.id_cliente = c.id_cliente
        ORDER BY t.fecha DESC, t.hora_inicio DESC
        LIMIT 1
    ) ut ON true
    ORDER BY gasto_total DESC
""";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                // Conversión de java.sql.Date a java.time.LocalDate
                Date sqlDate = rs.getDate("fecha_alta");
                LocalDate fechaAlta = (sqlDate != null) ? sqlDate.toLocalDate() : null;

                ClienteReporteExtendido cliente = new ClienteReporteExtendido(
                        rs.getInt("id_persona"),
                        rs.getString("nombre_completo"),
                        rs.getString("telefono"),
                        rs.getString("email"),
                        rs.getString("direccion"),
                        fechaAlta,
                        rs.getInt("cantidad_visitas"),
                        rs.getBigDecimal("gasto_total"),   // ✅ BigDecimal
                        rs.getString("estado_ultimo_turno"),
                        rs.getString("red_social")
                );

                lista.add(cliente);
            }
        }

        return lista;
    }
}