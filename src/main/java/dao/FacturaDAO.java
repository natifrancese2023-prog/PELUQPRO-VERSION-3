package dao;

import claseslogicas.*;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacturaDAO {

    private static final Logger log = LoggerFactory.getLogger(FacturaDAO.class);

    private static final String INSERT_FACTURA =
            "INSERT INTO factura (id_cliente, id_turno, fecha_hora, id_metodo, total, id_estado_factura) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String INSERT_DETALLE =
            "INSERT INTO detalle_factura (id_factura, id_servicio, descripcion_servicio, precio_unitario, cantidad) VALUES (?, ?, ?, ?, ?)";
    private final TurnoDAO turnoDAO = new TurnoDAO();

    public void guardarFactura(Factura factura, int idTurno, int idMetodoPago) throws SQLException {
        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            int idFactura = insertarCabecera(conn, factura, idTurno, idMetodoPago);
            if (idFactura == -1) {
                conn.rollback();
                throw new SQLException("No se pudo insertar la cabecera de la factura.");
            }

            if (!insertarDetalle(conn, idFactura, factura.getServicios())) {
                conn.rollback();
                throw new SQLException("No se pudo insertar el detalle de la factura.");
            }

            turnoDAO.actualizarEstadoTurno(conn, idTurno, EstadoTurno.FACTURADO.getId());

            conn.commit();
            factura.setIdFactura(idFactura);

            log.info("Factura guardada id={} turno={} cliente={}", idFactura, idTurno, factura.getIdCliente());

        } catch (SQLException e) {
            log.error("Error al guardar factura turno={} cliente={}", idTurno, factura.getIdCliente(), e);
            throw e;
        }
    }

    public Factura obtenerPorTurno(int idTurno) throws SQLException {
        String sql = "SELECT f.*, mp.nombre_metodo, mp.porcentaje_modificador, ef.nombre AS nombre_estado " +
                "FROM factura f " +
                "JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "JOIN estado_factura ef ON f.id_estado_factura = ef.id_estado_factura " +
                "WHERE f.id_turno = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Factura factura = new Factura();
                    factura.setIdFactura(rs.getInt("id_factura"));
                    factura.setIdTurno(rs.getInt("id_turno"));
                    factura.setIdCliente(rs.getInt("id_cliente"));
                    factura.setMontoTotal(rs.getBigDecimal("total"));
                    factura.setMetodoPago(rs.getString("nombre_metodo"));

                    // 👇 Ajuste con enum
                    int idEstado = rs.getInt("id_estado_factura");
                    EstadoFactura estado = EstadoFactura.fromId(idEstado);
                    factura.setEstadoFactura(estado);

                    return factura;
                }
            }
        }
        return null;
    }

    /**
     * NUEVO (flujo de estado): faltaba una forma de recuperar una factura
     * por su propio id_factura antes de mutarle el estado. Sin este método,
     * FacturaService.marcarComoPagada/cancelarFactura no tenían cómo verificar
     * el estado actual antes de aplicar la transición, y el UPDATE en
     * actualizarEstadoFactura se ejecutaba "a ciegas".
     */
    public Factura obtenerPorId(int idFactura) throws SQLException {
        String sql = "SELECT f.*, mp.nombre_metodo, ef.nombre AS nombre_estado " +
                "FROM factura f " +
                "JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "JOIN estado_factura ef ON f.id_estado_factura = ef.id_estado_factura " +
                "WHERE f.id_factura = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idFactura);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Factura factura = new Factura();
                    factura.setIdFactura(rs.getInt("id_factura"));
                    factura.setIdTurno(rs.getInt("id_turno"));
                    factura.setIdCliente(rs.getInt("id_cliente"));
                    factura.setMontoTotal(rs.getBigDecimal("total"));
                    factura.setMetodoPago(rs.getString("nombre_metodo"));

                    int idEstado = rs.getInt("id_estado_factura");
                    factura.setEstadoFactura(EstadoFactura.fromId(idEstado));

                    return factura;
                }
            }
        }
        return null;
    }

    public static List<Factura> obtenerTodas() {
        Map<Integer, Factura> mapaFacturas = new LinkedHashMap<>();
        String sql = "SELECT f.id_factura, f.id_turno, f.fecha_hora, f.total, " +
                "mp.nombre_metodo AS metodo_pago, " +
                "c.id_cliente, p.nombre AS cliente_nombre, p.apellido AS cliente_apellido, " +
                "d.numero_documento AS cliente_documento, " +
                "ef.id_estado_factura, ef.nombre AS estado_factura, " +   // 👈 traemos id y nombre
                "df.id_detalle, df.descripcion_servicio, df.precio_unitario, df.cantidad, df.subtotal, " +
                "s.id_servicio, s.nombre_servicio " +
                "FROM factura f " +
                "JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "JOIN cliente c ON f.id_cliente = c.id_cliente " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "JOIN estado_factura ef ON f.id_estado_factura = ef.id_estado_factura " +
                "LEFT JOIN detalle_factura df ON f.id_factura = df.id_factura " +
                "LEFT JOIN servicios s ON df.id_servicio = s.id_servicio " +
                "ORDER BY f.id_factura DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                int idFactura = rs.getInt("id_factura");

                Factura factura = mapaFacturas.get(idFactura);
                if (factura == null) {
                    factura = new Factura();
                    factura.setIdFactura(idFactura);
                    factura.setIdTurno(rs.getInt("id_turno"));
                    factura.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                    factura.setMontoTotal(rs.getBigDecimal("total"));
                    factura.setMetodoPago(rs.getString("metodo_pago"));

                    // 👇 Ajuste con enum
                    int idEstado = rs.getInt("id_estado_factura");
                    EstadoFactura estado = EstadoFactura.fromId(idEstado);
                    factura.setEstadoFactura(estado);

                    Cliente cliente = new Cliente();
                    cliente.setNombre(rs.getString("cliente_nombre"));
                    cliente.setApellido(rs.getString("cliente_apellido"));
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    factura.setIdCliente(rs.getInt("id_cliente"));
                    factura.setCliente(cliente);

                    mapaFacturas.put(idFactura, factura);
                }

                int idDetalle = rs.getInt("id_detalle");
                if (idDetalle != 0) {
                    DetalleFactura detalle = new DetalleFactura();
                    detalle.setIdDetalle(idDetalle);
                    detalle.setDescripcionServicio(rs.getString("descripcion_servicio"));
                    detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                    detalle.setCantidad(rs.getInt("cantidad"));
                    detalle.setSubtotal(rs.getBigDecimal("subtotal"));

                    Servicio servicio = new Servicio();
                    servicio.setIdServicio(rs.getInt("id_servicio"));
                    servicio.setNombreServicio(rs.getString("nombre_servicio"));
                    detalle.setServicio(servicio);

                    factura.addDetalle(detalle);
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener todas las facturas", e);
        }
        return new ArrayList<>(mapaFacturas.values());
    }

    public static List<Factura> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        Map<Integer, Factura> mapaFacturas = new LinkedHashMap<>();
        String sql = "SELECT f.id_factura, f.id_turno, f.fecha_hora, f.total, " +
                "mp.nombre_metodo AS metodo_pago, " +
                "c.id_cliente, p.nombre AS cliente_nombre, p.apellido AS cliente_apellido, " +
                "d.numero_documento AS cliente_documento, " +
                "ef.id_estado_factura, ef.nombre AS estado_factura, " +   // 👈 traemos id y nombre
                "df.id_detalle, df.descripcion_servicio, df.precio_unitario, df.cantidad, df.subtotal, " +
                "s.nombre_servicio " +
                "FROM factura f " +
                "JOIN metodo_pago mp ON f.id_metodo = mp.id_metodo " +
                "JOIN cliente c ON f.id_cliente = c.id_cliente " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "JOIN estado_factura ef ON f.id_estado_factura = ef.id_estado_factura " +
                "LEFT JOIN detalle_factura df ON f.id_factura = df.id_factura " +
                "LEFT JOIN servicios s ON df.id_servicio = s.id_servicio " +
                "WHERE DATE(f.fecha_hora) BETWEEN ? AND ? " +
                "ORDER BY f.id_factura DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(desde));
            ps.setDate(2, java.sql.Date.valueOf(hasta));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idFactura = rs.getInt("id_factura");

                    Factura factura = mapaFacturas.get(idFactura);
                    if (factura == null) {
                        factura = new Factura();
                        factura.setIdFactura(idFactura);
                        factura.setIdTurno(rs.getInt("id_turno"));
                        factura.setFechaHora(rs.getTimestamp("fecha_hora").toLocalDateTime());
                        factura.setMontoTotal(rs.getBigDecimal("total"));
                        factura.setMetodoPago(rs.getString("metodo_pago"));

                        // 👇 Ajuste con enum
                        int idEstado = rs.getInt("id_estado_factura");
                        EstadoFactura estado = EstadoFactura.fromId(idEstado);
                        factura.setEstadoFactura(estado);

                        Cliente cliente = new Cliente();
                        cliente.setNombre(rs.getString("cliente_nombre"));
                        cliente.setApellido(rs.getString("cliente_apellido"));
                        cliente.setIdCliente(rs.getInt("id_cliente"));
                        factura.setIdCliente(rs.getInt("id_cliente"));
                        factura.setCliente(cliente);

                        mapaFacturas.put(idFactura, factura);
                    }

                    int idDetalle = rs.getInt("id_detalle");
                    if (idDetalle != 0) {
                        DetalleFactura detalle = new DetalleFactura();
                        detalle.setIdDetalle(idDetalle);
                        detalle.setDescripcionServicio(rs.getString("descripcion_servicio"));
                        detalle.setPrecioUnitario(rs.getBigDecimal("precio_unitario"));
                        detalle.setCantidad(rs.getInt("cantidad"));
                        detalle.setSubtotal(rs.getBigDecimal("subtotal"));

                        Servicio servicio = new Servicio();
                        servicio.setNombreServicio(rs.getString("nombre_servicio"));
                        detalle.setServicio(servicio);

                        factura.addDetalle(detalle);
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener facturas por rango {} - {}", desde, hasta, e);
        }
        return new ArrayList<>(mapaFacturas.values());
    }

    // Inserta cabecera de factura y devuelve id generado
    private int insertarCabecera(Connection conn, Factura factura, int idTurno, int idMetodoPago) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_FACTURA, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, factura.getIdCliente());
            ps.setInt(2, idTurno);
            ps.setTimestamp(3, Timestamp.valueOf(factura.getFechaHora()));
            ps.setInt(4, idMetodoPago);
            ps.setBigDecimal(5, factura.getMontoTotal());
            ps.setInt(6, factura.getEstadoFactura().getIdEstadoFactura());

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        }
        return -1;
    }

    // Inserta detalle de factura
    private boolean insertarDetalle(Connection conn, int idFactura, List<Servicio> servicios) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DETALLE)) {
            for (Servicio servicio : servicios) {
                ps.setInt(1, idFactura);
                ps.setInt(2, servicio.getIdServicio());

                String descripcion = (servicio.getDescripcion() != null && !servicio.getDescripcion().isBlank())
                        ? servicio.getDescripcion()
                        : "Sin descripción";
                ps.setString(3, descripcion);

                BigDecimal precioUnitario = BigDecimal.valueOf(servicio.getPrecio());
                ps.setBigDecimal(4, precioUnitario);
                ps.setInt(5, 1);

                ps.addBatch();
            }
            int[] resultados = ps.executeBatch();
            return resultados.length > 0;
        }

    }


    public void actualizarEstadoFactura(Connection conn, int idFactura, EstadoFactura nuevoEstado) throws SQLException {
        String sql = "UPDATE factura SET id_estado_factura = ? WHERE id_factura = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoEstado.getIdEstadoFactura());
            ps.setInt(2, idFactura);
            ps.executeUpdate();
        }
    }
}