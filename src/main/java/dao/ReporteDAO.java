package dao;

import clasesreportes.ClienteReporteExtendido;
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
        COUNT(DISTINCT v.id_visita) AS cantidad_visitas,
        COALESCE(SUM(f.total), 0) AS gasto_total,
        MAX(et.nombre_estado) AS estado_ultimo_turno,
        STRING_AGG(rs.nombre_usuario, ', ') AS red_social
    FROM persona p
    JOIN cliente c ON p.id_persona = c.id_persona
    LEFT JOIN visita v ON c.id_cliente = v.id_cliente
    LEFT JOIN factura f ON c.id_cliente = f.id_cliente
    LEFT JOIN turno t ON c.id_cliente = t.id_cliente
    LEFT JOIN estado et ON t.id_estado = et.id_estado
    LEFT JOIN red_social rs ON c.id_cliente = rs.id_cliente
    GROUP BY p.id_persona, c.fecha_alta
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
                        rs.getDouble("gasto_total"),
                        rs.getString("estado_ultimo_turno"),
                        rs.getString("red_social")
                );
                lista.add(cliente);
            }
        }

        return lista;
    }
}
