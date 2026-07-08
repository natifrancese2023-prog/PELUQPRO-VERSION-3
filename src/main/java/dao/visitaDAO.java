package dao;

import claseslogicas.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class visitaDAO {

    private static final Logger log = LoggerFactory.getLogger(visitaDAO.class);
    private final ClienteDAO clienteDAO = new ClienteDAO();

    private static final String INSERT_VISITA_CABECERA =
            "INSERT INTO visita (id_cliente, id_estilista, id_turno, fecha_hora) VALUES (?, ?, ?, ?)";
    private static final String SELECT_FECHA_HORA_TURNO =
            "SELECT fecha, hora_inicio FROM turno WHERE id_turno = ?";
    private static final String INSERT_DETALLE_SERVICIO =
            "INSERT INTO detalle_servicio (id_visita, id_servicio, observaciones_servicio) VALUES (?, ?, ?)";
    private static final String SELECT_ID_TIPO_SERVICIO =
            "SELECT id_servicio FROM servicios WHERE nombre_servicio = ?";
    private static final String SELECT_NOMBRES_SERVICIOS =
            "SELECT nombre_servicio FROM servicios ORDER BY nombre_servicio";
    private static final String SELECT_HISTORIAL_COMPLETO =
            "SELECT v.id_visita, v.fecha_hora, " +
                    "p.nombre AS nombre_estilista, " +
                    "s.nombre_servicio, " +
                    "ds.observaciones_servicio " +
                    "FROM visita v " +
                    "JOIN empleado e ON v.id_estilista = e.id_empleado " +
                    "JOIN persona p ON e.id_persona = p.id_persona " +
                    "LEFT JOIN detalle_servicio ds ON v.id_visita = ds.id_visita " +
                    "LEFT JOIN servicios s ON ds.id_servicio = s.id_servicio " +
                    "WHERE v.id_cliente = ? " +
                    "ORDER BY v.fecha_hora DESC";
    private static final String SELECT_VISITA_POR_ID =
            "SELECT v.id_visita, v.id_cliente, v.id_estilista, v.fecha_hora, v.id_turno " +
                    "FROM visita v WHERE v.id_visita = ?";
    private static final String SELECT_VISITA_POR_TURNO =
            "SELECT id_visita FROM visita WHERE id_turno = ?";
    private static final String SELECT_SERVICIOS_DE_VISITA =
            "SELECT ts.id_servicio, ts.nombre_servicio, ts.precio " +
                    "FROM detalle_servicio ds " +
                    "JOIN servicios ts ON ds.id_servicio = ts.id_servicio " +
                    "WHERE ds.id_visita = ?";


    public boolean guardarNuevaVisita(Cliente cliente, List<ServicioTemp> servicios, int idEstilista, int idTurno) {
        if (servicios.isEmpty() || cliente == null) {
            log.warn("No hay servicios o cliente seleccionado para guardar.");
            return false;
        }

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            int idVisitaGenerado = insertarVisitaCabecera(conn, cliente.getIdCliente(), idEstilista, idTurno);
            if (idVisitaGenerado == -1 || !insertarDetalleServicios(conn, idVisitaGenerado, servicios)) {
                conn.rollback();
                return false;
            }
            new TurnoDAO().actualizarEstadoTurno(conn, idTurno, EstadoTurno.FINALIZADO.getId());

// 👇 Generar factura pendiente automáticamente
            FacturaDAO facturaDAO = new FacturaDAO();
            Factura facturaPendiente = new Factura();
            facturaPendiente.setCliente(cliente);
            facturaPendiente.setEstadoFactura(EstadoFactura.PENDIENTE);


// ⚠️ Como tu método requiere idTurno e idMetodoPago, pasalos explícitamente
            int idMetodoPago = MetodoPagoDAO.obtenerPorDefecto(conn); // o el que definas como default
            facturaDAO.guardarFactura(facturaPendiente, idTurno, idMetodoPago);

            conn.commit();
            return true;


        } catch (SQLException e) {
            log.error("Error de transacción al guardar visita cliente={} turno={}",
                    cliente != null ? cliente.getIdCliente() : null, idTurno, e);
            return false;
        }
    }

    private int insertarVisitaCabecera(Connection conn, int idCliente, int idEstilista, int idTurno) throws SQLException {
        // La fecha de la visita debe ser la fecha/hora del turno asociado,
        // no el instante en que se registra la visita en el sistema.
        LocalDateTime fechaHoraTurno = obtenerFechaHoraTurno(conn, idTurno);
        if (fechaHoraTurno == null) {
            throw new SQLException("No se encontró el turno con ID " + idTurno + " para determinar la fecha de la visita.");
        }

        int idGenerado = -1;
        try (PreparedStatement ps = conn.prepareStatement(INSERT_VISITA_CABECERA, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idEstilista);
            ps.setInt(3, idTurno);
            ps.setTimestamp(4, Timestamp.valueOf(fechaHoraTurno));

            if (ps.executeUpdate() > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        idGenerado = rs.getInt(1);
                    }
                }
            }
        }
        return idGenerado;
    }

    private LocalDateTime obtenerFechaHoraTurno(Connection conn, int idTurno) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_FECHA_HORA_TURNO)) {
            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Date fechaSql = rs.getDate("fecha");
                    Time horaSql = rs.getTime("hora_inicio");
                    if (fechaSql == null || horaSql == null) {
                        return null;
                    }
                    LocalDate fecha = fechaSql.toLocalDate();
                    LocalTime hora = horaSql.toLocalTime();
                    return LocalDateTime.of(fecha, hora);
                }
            }
        }
        return null;
    }
    private boolean insertarDetalleServicios(Connection conn, int idVisita, List<ServicioTemp> servicios) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DETALLE_SERVICIO)) {
            for (ServicioTemp temp : servicios) {
                // 👇 traducimos el nombre a ID
                int idServicio = obtenerIdTipoServicio(conn, temp.getServicio());
                if (idServicio == -1) {
                    throw new SQLException("No se encontró el servicio: " + temp.getServicio());
                }

                ps.setInt(1, idVisita);
                ps.setInt(2, idServicio);
                ps.setString(3, temp.getObservaciones());
                ps.addBatch();
            }
            int[] resultados = ps.executeBatch();
            for (int res : resultados) {
                if (res <= 0) return false;
            }
            return true;
        }
    }

    private int obtenerIdTipoServicio(Connection conn, String nombreServicio) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ID_TIPO_SERVICIO)) {
            ps.setString(1, nombreServicio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("id_servicio");
                }
            }
        }
        return -1;
    }

    public List<String> obtenerNombresServicios() {
        List<String> servicios = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SELECT_NOMBRES_SERVICIOS)) {
            while (rs.next()) {
                servicios.add(rs.getString("nombre_servicio"));
            }
        } catch (SQLException e) {
            log.error("Error al obtener nombres de servicios", e);
        }
        return servicios;
    }

    public List<HistorialView> obtenerHistorialPorCliente(int idCliente) {
        List<HistorialView> historial = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_HISTORIAL_COMPLETO)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    HistorialView hv = new HistorialView();
                    hv.setIdVisita(rs.getInt("id_visita"));
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    if (ts != null) hv.setFechaHora(ts.toLocalDateTime());
                    hv.setNombreEstilista(rs.getString("nombre_estilista"));
                    hv.setNombreServicio(rs.getString("nombre_servicio"));
                    hv.setObservaciones(rs.getString("observaciones_servicio"));
                    historial.add(hv);
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener historial de cliente {}", idCliente, e);
        }
        return historial;
    }
    public Visita obtenerVisitaPorId(int idVisita) {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VISITA_POR_ID)) {
            ps.setInt(1, idVisita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    // 1. Extraer TODOS los datos del ResultSet de la visita primero
                    int idCliente = rs.getInt("id_cliente");
                    int idEstilista = rs.getInt("id_estilista");
                    int idTurno = rs.getInt("id_turno");
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    LocalDateTime fechaHora = ts != null ? ts.toLocalDateTime() : null;

                    // 2. Cargar las listas y relaciones externas PRIMERO, antes de construir la Visita
                    List<Servicio> servicios = obtenerServiciosDeVisita(idVisita);

                    // Extraemos los IDs de servicios para pasárselos al constructor
                    List<Integer> idServicios = new ArrayList<>();
                    for (Servicio s : servicios) {
                        idServicios.add(s.getIdServicio());
                    }

                    // 3. Instanciar el objeto utilizando su constructor correcto de 4 parámetros
                    Visita visita = new Visita(idVisita, idCliente, fechaHora, idServicios);

                    // 4. Setear el resto de propiedades mediante sus setters individuales
                    visita.setIdEstilista(idEstilista);
                    visita.setIdTurno(idTurno);
                    visita.setServiciosRealizados(servicios);

                    Cliente cliente = clienteDAO.obtenerPorId(idCliente);
                    visita.setCliente(cliente);

                    return visita;
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener visita por ID {}", idVisita, e);
        }
        return null;
    }
    public Visita obtenerVisitaPorTurno(int idTurno) {
        int idVisita = -1;
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VISITA_POR_TURNO)) {
            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    idVisita = rs.getInt("id_visita");
                }
            }
        } catch (SQLException e) {
            log.error("Error al obtener id_visita por turno {}", idTurno, e);
        }

        // Se invoca por fuera de la conexión anterior para evitar bloqueos en el Statement
        if (idVisita != -1) {
            return obtenerVisitaPorId(idVisita);
        }
        return null;
    }
    private List<Integer> obtenerIdsServicios(int idVisita) throws SQLException {
        List<Integer> idServicios = new ArrayList<>();
        String sql = "SELECT id_servicio FROM detalle_servicio WHERE id_visita = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVisita);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idServicios.add(rs.getInt("id_servicio"));
                }
            }
        }
        return idServicios;
    }

    private List<Servicio> obtenerServiciosDeVisita(int idVisita) throws SQLException {
        List<Servicio> servicios = new ArrayList<>();
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_SERVICIOS_DE_VISITA)) {

            ps.setInt(1, idVisita);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Servicio s = new Servicio();
                    s.setIdServicio(rs.getInt("id_servicio"));
                    s.setNombreServicio(rs.getString("nombre_servicio"));
                    s.setPrecio(rs.getDouble("precio"));
                    servicios.add(s);
                }
            }
        }
        return servicios;
    }

    public List<Visita> obtenerTodas() {
        List<Visita> lista = new ArrayList<>();
        String sql = "SELECT v.id_visita, v.fecha_hora AS fecha, c.id_cliente, " +
                "p.nombre AS cliente_nombre, p.apellido AS cliente_apellido, " +
                "v.id_estilista AS id_estilista, pe.nombre AS estilista_nombre, pe.apellido AS estilista_apellido, " +
                "v.id_turno " +
                "FROM visita v " +
                "JOIN cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN empleado e ON v.id_estilista = e.id_empleado " +
                "JOIN roles r ON e.id_rol = r.id_rol " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "WHERE r.es_estilista = 1 " +
                "ORDER BY v.fecha_hora DESC";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Visita v = new Visita();
                v.setIdVisita(rs.getInt("id_visita"));
                v.setFechaHoraCierre(rs.getTimestamp("fecha").toLocalDateTime());
                v.setIdTurno(rs.getInt("id_turno"));

                Persona personaCliente = new Persona();
                personaCliente.setNombre(rs.getString("cliente_nombre"));
                personaCliente.setApellido(rs.getString("cliente_apellido"));

                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setPersona(personaCliente);
                v.setCliente(cliente);

                Persona personaEstilista = new Persona();
                personaEstilista.setNombre(rs.getString("estilista_nombre"));
                personaEstilista.setApellido(rs.getString("estilista_apellido"));

                Empleado empleado = new Empleado();
                empleado.setIdEmpleado(rs.getInt("id_estilista"));
                empleado.setPersona(personaEstilista);
                v.setEmpleado(empleado);

                lista.add(v);
            }

        } catch (SQLException e) {
            log.error("Error al obtener todas las visitas", e);
        }
        return lista;
    }
}