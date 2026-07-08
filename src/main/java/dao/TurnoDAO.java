package dao;

import claseslogicas.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.sql.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TurnoDAO {

    private static final Logger log = LoggerFactory.getLogger(TurnoDAO.class);

    private static final int ID_PENDIENTE = EstadoTurno.PENDIENTE.getId();
    private static final int ID_CONFIRMADO = EstadoTurno.CONFIRMADO.getId();
    private static final int ID_CANCELADO = EstadoTurno.CANCELADO.getId();
    private static final int ID_FINALIZADO = EstadoTurno.FINALIZADO.getId();

    // I. Validación de disponibilidad
    public boolean validarDisponibilidad(int idEmpleado, LocalDate fecha, LocalTime horaInicioPropuesta, int duracionMinutos) throws SQLException {
        LocalTime horaFinPropuesta = horaInicioPropuesta.plusMinutes(duracionMinutos);

        String sql = "SELECT COUNT(*) FROM turno WHERE id_empleado = ? AND fecha = ? AND id_estado IN (?, ?) AND (hora_inicio < ?) AND (hora_fin > ?)";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpleado);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setInt(3, ID_PENDIENTE);
            ps.setInt(4, ID_CONFIRMADO);
            ps.setTime(5, Time.valueOf(horaFinPropuesta));
            ps.setTime(6, Time.valueOf(horaInicioPropuesta));

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) == 0;
            }
        }
    }
    // Inserción de turno con servicios
    public boolean insertarTurno(Turno turno) throws SQLException {
        boolean exito = false;
        String sqlTurno = "INSERT INTO turno (id_cliente, id_empleado, id_estado, fecha, hora_inicio, hora_fin, observaciones, fecha_creacion) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlServicio = "INSERT INTO turno_servicios (id_turno, id_servicio) VALUES (?, ?)";

        try (Connection conn = ConexionBD.getConnection()) {
            conn.setAutoCommit(false);

            if (!clienteExiste(turno.getIdCliente())) {
                throw new SQLException("El cliente con ID " + turno.getIdCliente() + " no existe en la base.");
            }

            try (PreparedStatement psTurno = conn.prepareStatement(sqlTurno, Statement.RETURN_GENERATED_KEYS)) {
                psTurno.setInt(1, turno.getIdCliente());
                psTurno.setInt(2, turno.getIdEmpleado());

                // 🔹 Estado inicial siempre PENDIENTE
                psTurno.setInt(3, EstadoTurno.PENDIENTE.getId());
                turno.setEstadoTurno(EstadoTurno.PENDIENTE);

                psTurno.setDate(4, Date.valueOf(turno.getFecha()));
                psTurno.setTime(5, Time.valueOf(turno.getHoraInicio()));
                psTurno.setTime(6, Time.valueOf(turno.getHoraFin()));
                psTurno.setString(7, turno.getObservaciones());
                psTurno.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
                psTurno.executeUpdate();

                try (ResultSet rsKeys = psTurno.getGeneratedKeys()) {
                    if (rsKeys.next()) {
                        turno.setIdTurno(rsKeys.getInt(1));
                    } else {
                        throw new SQLException("Fallo al obtener ID del turno.");
                    }
                }
            }

            try (PreparedStatement psServicio = conn.prepareStatement(sqlServicio)) {
                for (Servicio servicio : turno.getServicios()) {
                    psServicio.setInt(1, turno.getIdTurno());
                    psServicio.setInt(2, servicio.getIdServicio());
                    psServicio.addBatch();
                }
                psServicio.executeBatch();
            }

            conn.commit();
            exito = true;

        } catch (SQLException e) {
            log.error("Error al insertar turno cliente={} empleado={}", turno.getIdCliente(), turno.getIdEmpleado(), e);
            throw e;
        }
        return exito;
    }


    public List<Turno> obtenerTurnosPorCliente(int idCliente) throws SQLException {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT t.id_turno, t.fecha, t.hora_inicio, t.motivo_log, " +
                "e.id_empleado, pe.nombre AS estilista_nombre, pe.apellido AS estilista_apellido, " +
                "t.id_estado, " +
                "s.id_servicio, s.nombre_servicio AS servicio_nombre " +
                "FROM turno t " +
                "JOIN empleado e ON t.id_empleado = e.id_empleado " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "LEFT JOIN turno_servicios ts ON t.id_turno = ts.id_turno " +
                "LEFT JOIN servicios s ON ts.id_servicio = s.id_servicio " +
                "WHERE t.id_cliente = ? " +
                "ORDER BY t.fecha, t.hora_inicio";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                Map<Integer, Turno> mapaTurnos = new HashMap<>();

                while (rs.next()) {
                    int idTurno = rs.getInt("id_turno");
                    Turno turno = mapaTurnos.get(idTurno);

                    if (turno == null) {
                        turno = new Turno();
                        turno.setIdTurno(idTurno);
                        turno.setFecha(rs.getDate("fecha").toLocalDate());
                        turno.setHoraInicio(LocalTime.parse(rs.getString("hora_inicio")));
                        turno.setMotivoLog(rs.getString("motivo_log"));

                        // Estilista
                        Empleado empleado = new Empleado();
                        empleado.setIdEmpleado(rs.getInt("id_empleado"));
                        empleado.setNombre(rs.getString("estilista_nombre"));
                        empleado.setApellido(rs.getString("estilista_apellido"));
                        turno.setEmpleado(empleado);

                        // Estado con chequeo defensivo
                        int idEstado = rs.getInt("id_estado");
                        if (rs.wasNull()) {
                            turno.setEstadoTurno(EstadoTurno.PENDIENTE);
                        } else {
                            turno.setEstadoTurno(EstadoTurno.obtenerPorId(idEstado));
                        }

                        // Inicializar lista de servicios
                        turno.setServicios(new ArrayList<>());

                        mapaTurnos.put(idTurno, turno);
                    }

                    // Servicios asociados (puede haber varios por turno)
                    int idServicio = rs.getInt("id_servicio");
                    if (idServicio != 0) {
                        Servicio servicio = new Servicio();
                        servicio.setIdServicio(idServicio);
                        servicio.setNombreServicio(rs.getString("servicio_nombre"));
                        turno.getServicios().add(servicio);
                    }
                }

                turnos.addAll(mapaTurnos.values());
            }
        }
        return turnos;
    }
    public List<Turno> obtenerTurnosFiltrados(LocalDate fecha, Integer idEmpleado) throws SQLException {
        List<Turno> turnos = new ArrayList<>();

        String sql = "SELECT t.id_turno, t.fecha, t.hora_inicio, t.motivo_log, " +
                "c.id_cliente, pc.nombre AS cliente_nombre, pc.apellido AS cliente_apellido, " +
                "e.id_empleado, pe.nombre AS estilista_nombre, pe.apellido AS estilista_apellido, " +
                "t.id_estado " +
                "FROM turno t " +
                "JOIN cliente c ON t.id_cliente = c.id_cliente " +
                "JOIN persona pc ON c.id_persona = pc.id_persona " +
                "JOIN empleado e ON t.id_empleado = e.id_empleado " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "WHERE t.fecha = ? " +
                (idEmpleado != null ? "AND t.id_empleado = ?" : "") +
                "ORDER BY t.hora_inicio";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setDate(1, java.sql.Date.valueOf(fecha));
            if (idEmpleado != null) {
                ps.setInt(2, idEmpleado);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Turno turno = new Turno();
                    turno.setIdTurno(rs.getInt("id_turno"));

                    if (rs.getDate("fecha") != null) {
                        turno.setFecha(rs.getDate("fecha").toLocalDate());
                    }

                    // 🔴 FIX 1: Recuperar LocalTime de forma segura usando el tipo nativo java.sql.Time
                    java.sql.Time horaSql = rs.getTime("hora_inicio");
                    if (horaSql != null) {
                        turno.setHoraInicio(horaSql.toLocalTime());
                    }

                    turno.setMotivoLog(rs.getString("motivo_log"));

                    // Cliente
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    Persona personaCliente = new Persona();
                    personaCliente.setNombre(rs.getString("cliente_nombre"));
                    personaCliente.setApellido(rs.getString("cliente_apellido"));
                    cliente.setPersona(personaCliente);
                    turno.setCliente(cliente);

                    // Estilista
                    Empleado empleado = new Empleado();
                    empleado.setIdEmpleado(rs.getInt("id_empleado"));
                    empleado.setNombre(rs.getString("estilista_nombre"));
                    empleado.setApellido(rs.getString("estilista_apellido"));
                    turno.setEmpleado(empleado);

                    // 🔴 FIX 2: Mapeo seguro con el método exacto del Enum
                    int idEstado = rs.getInt("id_estado");
                    if (rs.wasNull()) {
                        turno.setEstadoTurno(EstadoTurno.PENDIENTE);
                    } else {
                        // Usamos buscarPorId que creamos en el paso anterior
                        turno.setEstadoTurno(EstadoTurno.buscarPorId(idEstado));
                    }

                    turnos.add(turno);
                }
            }
        }
        return turnos;
    }



    // Listado completo
    // Listado completo
    public Turno obtenerPorId(int idTurno) throws SQLException {
        String sql = "SELECT t.id_turno, t.fecha, t.hora_inicio, t.motivo_log, " +
                "c.id_cliente, pc.nombre AS cliente_nombre, pc.apellido AS cliente_apellido, " +
                "e.id_empleado, pe.nombre AS estilista_nombre, pe.apellido AS estilista_apellido, " +
                "t.id_estado " +
                "FROM turno t " +
                "JOIN cliente c ON t.id_cliente = c.id_cliente " +
                "JOIN persona pc ON c.id_persona = pc.id_persona " +
                "JOIN empleado e ON t.id_empleado = e.id_empleado " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "WHERE t.id_turno = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idTurno);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Turno turno = new Turno();
                    turno.setIdTurno(rs.getInt("id_turno"));
                    if (rs.getDate("fecha") != null) turno.setFecha(rs.getDate("fecha").toLocalDate());
                    java.sql.Time horaSql = rs.getTime("hora_inicio");
                    if (horaSql != null) turno.setHoraInicio(horaSql.toLocalTime());
                    turno.setMotivoLog(rs.getString("motivo_log"));

                    // Cliente
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    Persona pCliente = new Persona();
                    pCliente.setNombre(rs.getString("cliente_nombre"));
                    pCliente.setApellido(rs.getString("cliente_apellido"));
                    cliente.setPersona(pCliente);
                    turno.setCliente(cliente);

                    // Estilista
                    Empleado empleado = new Empleado();
                    empleado.setIdEmpleado(rs.getInt("id_empleado"));

                    // Corrección defensiva: Asignar nombre/apellido a la Persona del Empleado si tu modelo lo requiere,
                    // o directamente al empleado si mapea los strings directo.
                    Persona pEmpleado = new Persona();
                    pEmpleado.setNombre(rs.getString("estilista_nombre"));
                    pEmpleado.setApellido(rs.getString("estilista_apellido"));
                    empleado.setPersona(pEmpleado);
                    turno.setEmpleado(empleado);

                    // --- ESTADO (FIX DE DUPLICIDAD DE SETTERS) ---
                    int idEstado = rs.getInt("id_estado");
                    EstadoTurno estadoReal = rs.wasNull() ? EstadoTurno.PENDIENTE : EstadoTurno.buscarPorId(idEstado);

                    // 🔴 Forzamos la asignación en ambas propiedades para evitar fallas en Service o UI
                    turno.setEstadoTurno(estadoReal);
                    turno.setEstadoLogico(estadoReal);

                    return turno;
                }
            }
        }
        return null;
    }
    public List<Turno> obtenerTodos() {
        List<Turno> turnos = new ArrayList<>();
        String sql = "SELECT t.id_turno, t.fecha, t.hora_inicio, t.motivo_log, " +
                "c.id_cliente, pc.nombre AS cliente_nombre, pc.apellido AS cliente_apellido, " +
                "e.id_empleado, pe.nombre AS estilista_nombre, pe.apellido AS estilista_apellido, " +
                "t.id_estado " +
                "FROM turno t " +
                "JOIN cliente c ON t.id_cliente = c.id_cliente " +
                "JOIN persona pc ON c.id_persona = pc.id_persona " +
                "JOIN empleado e ON t.id_empleado = e.id_empleado " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "ORDER BY t.fecha DESC, t.hora_inicio DESC"; // Ordenados por los más recientes

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Turno turno = new Turno();
                turno.setIdTurno(rs.getInt("id_turno"));
                if (rs.getDate("fecha") != null) turno.setFecha(rs.getDate("fecha").toLocalDate());
                java.sql.Time horaSql = rs.getTime("hora_inicio");
                if (horaSql != null) turno.setHoraInicio(horaSql.toLocalTime());
                turno.setMotivoLog(rs.getString("motivo_log"));

                // Cliente
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                Persona pCliente = new Persona();
                pCliente.setNombre(rs.getString("cliente_nombre"));
                pCliente.setApellido(rs.getString("cliente_apellido"));
                cliente.setPersona(pCliente);
                turno.setCliente(cliente);

                // Estilista
                Empleado empleado = new Empleado();
                empleado.setIdEmpleado(rs.getInt("id_empleado"));
                Persona pEmpleado = new Persona();
                pEmpleado.setNombre(rs.getString("estilista_nombre"));
                pEmpleado.setApellido(rs.getString("estilista_apellido"));
                empleado.setPersona(pEmpleado);
                turno.setEmpleado(empleado);

                // Estado (Doble asignación defensiva para tus setters)
                int idEstado = rs.getInt("id_estado");
                EstadoTurno estadoReal = rs.wasNull() ? EstadoTurno.PENDIENTE : EstadoTurno.buscarPorId(idEstado);
                turno.setEstadoTurno(estadoReal);
                turno.setEstadoLogico(estadoReal);

                turnos.add(turno);
            }
        } catch (SQLException e) {
            log.error("Error al obtener todos los turnos en TurnoDAO", e);
        }
        return turnos;
    }

    // Actualización de estado
    public boolean actualizarEstado(int idTurno, EstadoTurno nuevoEstado, String motivoLog) throws SQLException {
        String SQL = "UPDATE turno SET id_estado = ?, motivo_log = ? WHERE id_turno = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setInt(1, nuevoEstado.getId());
            ps.setString(2, motivoLog);
            ps.setInt(3, idTurno);
            return ps.executeUpdate() > 0;
        }
    }

    public void actualizarEstadoTurno(Connection conn, int idTurno, int idEstado) throws SQLException {
        String sqlSelect = "SELECT id_estado FROM turno WHERE id_turno = ?";
        int estadoActual = -1;
        try (PreparedStatement psSel = conn.prepareStatement(sqlSelect)) {
            psSel.setInt(1, idTurno);
            try (ResultSet rs = psSel.executeQuery()) {
                if (rs.next()) {
                    estadoActual = rs.getInt("id_estado");
                }
            }
        }

        if (estadoActual == -1) {
            throw new SQLException("Error: Turno no encontrado.");
        }

        boolean transicionValida = false;
        if (estadoActual == EstadoTurno.PENDIENTE.getId() || estadoActual == EstadoTurno.CONFIRMADO.getId()) {
            transicionValida = true;
        }
        if (estadoActual == EstadoTurno.FINALIZADO.getId() && idEstado == EstadoTurno.FACTURADO.getId()) {
            transicionValida = true;
        }

        if (!transicionValida) {
            throw new SQLException("Error: No se puede cambiar el estado de un turno en estado " + estadoActual);
        }

        String sqlUpdate = "UPDATE turno SET id_estado = ? WHERE id_turno = ?";
        try (PreparedStatement psUpd = conn.prepareStatement(sqlUpdate)) {
            psUpd.setInt(1, idEstado);
            psUpd.setInt(2, idTurno);
            psUpd.executeUpdate();
        }
    }

    // Validación de cliente
    public boolean clienteExiste(int idCliente) throws SQLException {
        String sql = "SELECT 1 FROM cliente WHERE id_cliente = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
