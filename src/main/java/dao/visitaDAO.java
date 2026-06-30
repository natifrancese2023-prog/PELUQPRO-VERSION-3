package dao;

import claseslogicas .*;
import javafx.collections.ObservableList;

import java.sql .*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class visitaDAO {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    private static final String INSERT_VISITA_CABECERA =
            "INSERT INTO visita (id_cliente, id_estilista, id_turno) VALUES (?, ?, ?)";

    private static final String INSERT_DETALLE_SERVICIO =
            "INSERT INTO detalle_servicio (id_visita, id_tipo_servicio, observaciones_servicio) VALUES (?, ?, ?)";

    private static final String SELECT_ID_TIPO_SERVICIO =
            "SELECT id_tipo_servicio FROM servicios WHERE nombre_servicio = ?";

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
                    "LEFT JOIN servicios s ON ds.id_tipo_servicio = s.id_tipo_servicio " +
                    "WHERE v.id_cliente = ? " +
                    "ORDER BY v.fecha_hora DESC";




    private static final String SELECT_VISITA_POR_ID =
            "SELECT v.id_visita, v.id_cliente, v.id_estilista, v.fecha_hora, v.id_turno " +
                    "FROM visita v WHERE v.id_visita = ?";

    private static final String SELECT_VISITA_POR_TURNO =
            "SELECT id_visita FROM visita WHERE id_turno = ?";

    private static final String SELECT_SERVICIOS_DE_VISITA =
            "SELECT ts.id_tipo_servicio, ts.nombre_servicio, ts.precio " +
                    // 🚨 CORREGIDO: De 'detalle_servicios' a 'detalle_servicio'
                    "FROM detalle_servicio ds " +
                    // 🚨 CORREGIDO: De 'tipos_servicio' a 'servicios'
                    "JOIN servicios ts ON ds.id_tipo_servicio = ts.id_tipo_servicio " +
                    "WHERE ds.id_visita = ?";


    public visitaDAO() {
        // Constructor vacío
    }



    public boolean guardarNuevaVisita(Cliente cliente, ObservableList<ServicioTemp> servicios, int idEstilista, int idTurno) {
        if (servicios.isEmpty() || cliente == null) {
            System.err.println("Advertencia: No hay servicios o cliente seleccionado para guardar.");
            return false;
        }

        Connection conn = null;
        int idVisitaGenerado = -1;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);
            idVisitaGenerado = insertarVisitaCabecera(conn, cliente.getIdCliente(), idEstilista, idTurno); // ✅
            if (idVisitaGenerado == -1) {
                conn.rollback();
                return false;
            }
            if (!insertarDetalleServicios(conn, idVisitaGenerado, servicios)) {
                conn.rollback();
                return false;
            }
            TurnoDAO turnoDAO = new TurnoDAO();
            turnoDAO.actualizarEstadoTurno(conn, idTurno, 3);
            conn.commit();
            return true;

        } catch (SQLException e) {
            try {
                if (conn != null) conn.rollback();
            } catch (SQLException ex) {
                System.err.println("Error al hacer rollback: " + ex.getMessage());
            }
            System.err.println("ERROR DE TRANSACCIÓN al guardar la visita: " + e.getMessage());
            return false;
        } finally {
            try {
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }


    private int insertarVisitaCabecera(Connection conn, int idCliente, int idEstilista, int idTurno) throws SQLException {
        int idGenerado = -1;
        try (PreparedStatement ps = conn.prepareStatement(INSERT_VISITA_CABECERA, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, idCliente);
            ps.setInt(2, idEstilista);
            ps.setInt(3, idTurno);

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

    private boolean insertarDetalleServicios(Connection conn, int idVisita, ObservableList<ServicioTemp> servicios) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_DETALLE_SERVICIO)) {
            for (ServicioTemp temp : servicios) {
                int idTipoServicio = obtenerIdTipoServicio(conn, temp.getServicio());
                if (idTipoServicio == -1) {
                    throw new SQLException("Error al obtener ID de servicio para: " + temp.getServicio());
                }

                ps.setInt(1, idVisita);
                ps.setInt(2, idTipoServicio);
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
        int id = -1;
        try (PreparedStatement ps = conn.prepareStatement(SELECT_ID_TIPO_SERVICIO)) {
            ps.setString(1, nombreServicio);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    id = rs.getInt("id_tipo_servicio");
                }
            }
        }
        return id;
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
            System.err.println("❌ Error al obtener tipos de servicio: " + e.getMessage());
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
                    if (ts != null) {
                        hv.setFechaHora(ts.toLocalDateTime());
                    }

                    hv.setNombreEstilista(rs.getString("nombre_estilista"));
                    hv.setNombreServicio(rs.getString("nombre_servicio"));
                    hv.setObservaciones(rs.getString("observaciones_servicio"));

                    historial.add(hv);
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al obtener historial de cliente: " + e.getMessage());
        }

        return historial;
    }

    public Visita obtenerVisitaPorId(int idVisita) {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VISITA_POR_ID)) {

            ps.setInt(1, idVisita);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idCliente = rs.getInt("id_cliente");
                    int idEstilista = rs.getInt("id_estilista");
                    int idTurno = rs.getInt("id_turno");
                    Timestamp ts = rs.getTimestamp("fecha_hora");
                    LocalDateTime fechaHora = ts != null ? ts.toLocalDateTime() : null;

                    List<Integer> idServicios = obtenerIdsServicios(idVisita);
                    Visita visita = new Visita(idVisita, idCliente, fechaHora, idServicios);

                    Cliente cliente = clienteDAO.obtenerPorId(idCliente);
                    visita.setCliente(cliente);
                    visita.setIdEstilista(idEstilista);
                    visita.setIdTurno(idTurno);

                    List<Servicio> servicios = obtenerServiciosDeVisita(idVisita);
                    visita.setServiciosRealizados(servicios);

                    return visita;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener visita por ID: " + e.getMessage());
        }
        return null;
    }

    public Visita obtenerVisitaPorTurno(int idTurno) {
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_VISITA_POR_TURNO)) {

            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int idVisita = rs.getInt("id_visita");
                    return obtenerVisitaPorId(idVisita); // reutiliza el método completo
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener visita por turno: " + e.getMessage());
        }
        return null;
    }

    private List<Integer> obtenerIdsServicios(int idVisita) throws SQLException {
        List<Integer> idServicios = new ArrayList<>();
        // 🚨 CORREGIDO: De 'detalle_servicios' a 'detalle_servicio'
        String sql = "SELECT id_tipo_servicio FROM detalle_servicio WHERE id_visita = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idVisita);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    idServicios.add(rs.getInt("id_tipo_servicio"));
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
                    s.setIdTipoServicio(rs.getInt("id_tipo_servicio"));
                    s.setNombreServicio(rs.getString("nombre_servicio"));
                    s.setPrecio(rs.getDouble("precio"));
                    servicios.add(s);
                }
            }
        }
        return servicios;
    }
    public static List<Visita> obtenerTodas() {
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

                // Cliente con Persona
                Persona personaCliente = new Persona();
                personaCliente.setNombre(rs.getString("cliente_nombre"));
                personaCliente.setApellido(rs.getString("cliente_apellido"));

                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setPersona(personaCliente);
                v.setCliente(cliente);

                // Estilista como Empleado con Persona
                Persona personaEstilista = new Persona();
                personaEstilista.setNombre(rs.getString("estilista_nombre"));
                personaEstilista.setApellido(rs.getString("estilista_apellido"));

                Empleado empleado = new Empleado();
                empleado.setIdEmpleado(rs.getInt("id_estilista"));
                empleado.setPersona(personaEstilista);
                v.setEmpleado(empleado);

                lista.add(v);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }



}