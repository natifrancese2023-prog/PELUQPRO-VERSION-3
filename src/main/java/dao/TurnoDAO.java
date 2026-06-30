package dao;

import claseslogicas.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class TurnoDAO {

    private static final ConexionBD conexionBD = new ConexionBD();
    private final HorarioAtencionDAO horarioDAO = new HorarioAtencionDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();

    private static final int ID_PENDIENTE = EstadoTurno.PENDIENTE.getId();
    private static final int ID_CONFIRMADO = EstadoTurno.CONFIRMADO.getId();
    private static final int ID_CANCELADO = EstadoTurno.CANCELADO.getId();
    private static final int ID_FINALIZADO = EstadoTurno.FINALIZADO.getId();


    // I. Validación de disponibilidad
    public boolean validarDisponibilidad(int idEmpleado, LocalDate fecha, LocalTime horaInicioPropuesta, int duracionMinutos) throws SQLException {
        LocalTime horaFinPropuesta = horaInicioPropuesta.plusMinutes(duracionMinutos);


        String sql = "SELECT COUNT(*) FROM turno WHERE id_empleado = ? AND fecha = ? AND id_estado IN (?, ?) AND (hora_inicio < ?) AND (hora_fin > ?)";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idEmpleado);
            ps.setDate(2, Date.valueOf(fecha));
            ps.setInt(3, ID_PENDIENTE);
            ps.setInt(4, ID_CONFIRMADO);
            ps.setTime(5, Time.valueOf(horaFinPropuesta));
            ps.setTime(6, Time.valueOf(horaInicioPropuesta));

            ResultSet rs = ps.executeQuery();
            return rs.next() && rs.getInt(1) == 0;
        }
    }
    public boolean insertarTurno(Turno turno) throws SQLException {
        Connection conn = null;
        boolean exito = false;

        String sqlTurno = "INSERT INTO turno (id_cliente, id_empleado, id_estado, fecha, hora_inicio, hora_fin, observaciones, fecha_creacion) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        String sqlServicio = "INSERT INTO turno_servicios (id_turno, id_tipo_servicio) VALUES (?, ?)";

        try {
            conn = conexionBD.getConnection();
            conn.setAutoCommit(false);


            if (!clienteExiste(turno.getIdCliente())) {
                throw new SQLException("El cliente con ID " + turno.getIdCliente() + " no existe en la base.");
            }

            System.out.println("DEBUG: ID de cliente recibido → " + turno.getIdCliente());
            System.out.println("DEBUG: ID de empleado recibido → " + turno.getIdEmpleado());
            System.out.println("DEBUG: Fecha → " + turno.getFecha() + ", Inicio → " + turno.getHoraInicio() + ", Fin → " + turno.getHoraFin());


            PreparedStatement psTurno = conn.prepareStatement(sqlTurno, Statement.RETURN_GENERATED_KEYS);
            psTurno.setInt(1, turno.getIdCliente());
            psTurno.setInt(2, turno.getIdEmpleado());
            psTurno.setInt(3, EstadoTurno.PENDIENTE.getId());
            psTurno.setDate(4, Date.valueOf(turno.getFecha()));
            psTurno.setTime(5, Time.valueOf(turno.getHoraInicio()));
            psTurno.setTime(6, Time.valueOf(turno.getHoraFin()));
            psTurno.setString(7, turno.getObservaciones());
            psTurno.setTimestamp(8, Timestamp.valueOf(LocalDateTime.now()));
            psTurno.executeUpdate();

            ResultSet rsKeys = psTurno.getGeneratedKeys();
            if (rsKeys.next()) {
                turno.setIdTurno(rsKeys.getInt(1));
                System.out.println("DEBUG: ID de turno generado → " + turno.getIdTurno());
            } else {
                throw new SQLException("Fallo al obtener ID del turno.");
            }
            psTurno.close();


            PreparedStatement psServicio = conn.prepareStatement(sqlServicio);
            for (Servicio servicio : turno.getServicios()) {
                psServicio.setInt(1, turno.getIdTurno());
                psServicio.setInt(2, servicio.getIdTipoServicio());
                System.out.println("DEBUG: Insertando en turno_servicios → turno=" + turno.getIdTurno() + ", servicio=" + servicio.getIdTipoServicio());
                psServicio.addBatch();
            }
            psServicio.executeBatch();

            conn.commit();
            exito = true;

        } catch (SQLException e) {
            if (conn != null) conn.rollback();

            System.err.println("ERROR al guardar turno:");
            System.err.println("Código SQLState → " + e.getSQLState());
            System.err.println("Código de error → " + e.getErrorCode());
            System.err.println("Mensaje → " + e.getMessage());
            e.printStackTrace();

            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
        return exito;
    }


    public List<BloqueDisponible> obtenerTurnosDisponibles(LocalDate fecha, int duracionTotalMinutos, Integer idEstilistaOpcional) throws SQLException {
        List<BloqueDisponible> disponibles = new ArrayList<>();


        HorarioAtencion horario = horarioDAO.obtenerHorarioPorDia(fecha);
        System.out.println("DEBUG: Horario recuperado → " + (horario != null ? horario.getHoraApertura() + " a " + horario.getHoraCierre() : "null"));

        if (horario == null || horario.getHoraApertura().equals(horario.getHoraCierre())) {
            System.out.println("DEBUG: Día sin horario válido o sin atención.");
            return disponibles;
        }

        LocalTime inicioDia = horario.getHoraApertura();
        LocalTime finDia = horario.getHoraCierre();
        int intervalo = 30;

        List<Empleado> estilistas;
        if (idEstilistaOpcional != null) {
            estilistas = empleadoDAO.obtenerEstilistasPorId(idEstilistaOpcional);
        } else {
            estilistas = empleadoDAO.obtenerEstilistas();
        }
        System.out.println("DEBUG: Estilistas encontrados → " + estilistas.size());
        System.out.println("DEBUG: Generando bloques desde " + inicioDia + " hasta " + finDia);

        for (Empleado empleado : estilistas) {
            LocalTime horaActual = inicioDia;

            while (!horaActual.plusMinutes(duracionTotalMinutos).isAfter(finDia)) {
                System.out.println("DEBUG: Evaluando bloque " + horaActual + " a " + horaActual.plusMinutes(duracionTotalMinutos) + " para estilista ID " + empleado.getIdEmpleado());

                boolean disponible = validarDisponibilidad(empleado.getIdEmpleado(), fecha, horaActual, duracionTotalMinutos);
                System.out.println("DEBUG: ¿Disponible? " + disponible);

                if (disponible) {
                    BloqueDisponible bloque = new BloqueDisponible(
                            horaActual,
                            horaActual.plusMinutes(duracionTotalMinutos),
                            empleado
                    );
                    disponibles.add(bloque);
                }

                horaActual = horaActual.plusMinutes(intervalo);
            }
        }

        System.out.println("DEBUG: Total de bloques disponibles generados → " + disponibles.size());
        return disponibles;
    }
    public List<Turno> obtenerTurnosPorCliente(int idCliente) throws SQLException {
        List<Turno> turnos = new ArrayList<>();

        String sql = "SELECT t.id_turno, t.id_cliente, t.id_empleado, t.id_estado, t.fecha, t.hora_inicio, t.hora_fin, " +
                "t.observaciones, t.fecha_creacion, t.motivo_log, et.nombre_estado AS nombre_turno " +
                "FROM turno t " +
                "JOIN estado et ON t.id_estado = et.id_estado " +
                "WHERE t.id_cliente = ? AND t.id_estado IN (1, 2) AND t.fecha >= CURDATE() " +
                "ORDER BY t.fecha, t.hora_inicio";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Turno turno = new Turno();

                    int idEstado = rs.getInt("id_estado");
                    EstadoTurno estado = EstadoTurno.obtenerPorId(idEstado);

                    turno.setEstadoTurno(estado);
                    turno.setIdTurno(rs.getInt("id_turno"));
                    turno.setIdCliente(rs.getInt("id_cliente"));
                    turno.setIdEmpleado(rs.getInt("id_empleado"));
                    turno.setFecha(rs.getDate("fecha").toLocalDate());
                    turno.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
                    turno.setHoraFin(rs.getTime("hora_fin").toLocalTime());
                    turno.setObservaciones(rs.getString("observaciones"));
                    turno.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                    turno.setIdEstado(idEstado);
                    turno.setMotivoLog(rs.getString("motivo_log"));

                    turno.setCliente(clienteDAO.obtenerPorId(turno.getIdCliente()));
                    turno.setEmpleado(empleadoDAO.obtenerPorId(turno.getIdEmpleado()));
                    turno.setServicios(servicioDAO.obtenerServiciosPorTurno(turno.getIdTurno()));

                    turnos.add(turno);
                }
            }
        }
        return turnos;
    }


    public List<Turno> obtenerTurnosFiltrados(LocalDate fecha, Integer idEmpleado) throws SQLException {
        List<Turno> turnos = new ArrayList<>();
        // 🚨 CORREGIDO: De 'turnos' a 'turno' y de 'estado_turno' a 'estado'
        String sql = "SELECT t.id_turno, t.id_cliente, t.id_empleado, t.id_estado, t.fecha, t.hora_inicio, t.hora_fin, t.observaciones, t.fecha_creacion, t.motivo_log, et.nombre_estado AS nombre_turno FROM turno t JOIN estado et ON t.id_estado = et.id_estado WHERE t.fecha = ? ";
        if (idEmpleado != null) sql += "AND t.id_empleado = ? ";
        sql += "ORDER BY t.hora_inicio";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            int paramIndex = 1;
            ps.setDate(paramIndex++, Date.valueOf(fecha));
            if (idEmpleado != null) ps.setInt(paramIndex++, idEmpleado);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Turno turno = new Turno();

                    int idEstado = rs.getInt("id_estado");
                    EstadoTurno estado = EstadoTurno.obtenerPorId(idEstado);
                    turno.setEstadoTurno(estado);

                    turno.setIdTurno(rs.getInt("id_turno"));
                    turno.setIdCliente(rs.getInt("id_cliente"));
                    turno.setIdEmpleado(rs.getInt("id_empleado"));
                    turno.setFecha(rs.getDate("fecha").toLocalDate());
                    turno.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
                    turno.setHoraFin(rs.getTime("hora_fin").toLocalTime());
                    turno.setObservaciones(rs.getString("observaciones"));
                    turno.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());
                    turno.setIdEstado(idEstado);
                    turno.setMotivoLog(rs.getString("motivo_log"));

                    // Carga de objetos relacionados
                    turno.setCliente(clienteDAO.obtenerPorId(turno.getIdCliente()));
                    turno.setEmpleado(empleadoDAO.obtenerPorId(turno.getIdEmpleado()));
                    turno.setServicios(servicioDAO.obtenerServiciosPorTurno(turno.getIdTurno()));

                    turnos.add(turno);
                }

            }
        }
        return turnos;
    }
    public static List<Turno> obtenerTodos() {
        List<Turno> lista = new ArrayList<>();
        String sql = "SELECT t.id_turno, t.id_cliente, dcli.numero_documento AS cliente_documento, " +
                "t.id_empleado, demp.numero_documento AS empleado_documento, t.id_estado, t.fecha, " +
                "t.hora_inicio, t.hora_fin, t.observaciones, t.motivo_log, t.fecha_creacion, " +
                "s.id_tipo_servicio, s.nombre_servicio " +
                "FROM turno t " +
                "JOIN cliente c ON t.id_cliente = c.id_cliente " +
                "JOIN persona pc ON c.id_persona = pc.id_persona " +
                "JOIN documento dcli ON pc.id_documento = dcli.id_documento " +   // documento del cliente
                "JOIN empleado e ON t.id_empleado = e.id_empleado " +
                "JOIN persona pe ON e.id_persona = pe.id_persona " +
                "JOIN documento demp ON pe.id_documento = demp.id_documento " +   // documento del empleado
                "JOIN turno_servicios ts ON t.id_turno = ts.id_turno " +
                "JOIN servicios s ON ts.id_tipo_servicio = s.id_tipo_servicio " +  // 👈 espacio agregado
                "ORDER BY t.id_turno DESC";


        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Turno turno = new Turno();
                turno.setIdTurno(rs.getInt("id_turno"));
                turno.setIdCliente(rs.getInt("id_cliente"));
                turno.setClienteDocumento(rs.getString("cliente_documento")); // documento del cliente
                turno.setIdEmpleado(rs.getInt("id_empleado"));
                turno.setEmpleadoDocumento(rs.getString("empleado_documento")); // documento del empleado
                turno.setIdEstado(rs.getInt("id_estado"));
                turno.setFecha(rs.getDate("fecha").toLocalDate());
                turno.setHoraInicio(rs.getTime("hora_inicio").toLocalTime());
                turno.setHoraFin(rs.getTime("hora_fin").toLocalTime());
                turno.setObservaciones(rs.getString("observaciones"));
                turno.setMotivoLog(rs.getString("motivo_log"));
                turno.setFechaCreacion(rs.getTimestamp("fecha_creacion").toLocalDateTime());

                // Servicio realizado en el turno
                Servicio servicio = new Servicio();
                servicio.setIdTipoServicio(rs.getInt("id_tipo_servicio"));
                servicio.setNombreServicio(rs.getString("nombre_servicio"));
                turno.addServicio(servicio);

                lista.add(turno);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }


    public boolean actualizarEstado(int idTurno, EstadoTurno nuevoEstado, String motivoLog) throws SQLException {

        String SQL = "UPDATE turno SET id_estado = ?, motivo_log = ? WHERE id_turno = ?";
        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SQL)) {
            ps.setInt(1, nuevoEstado.getId());
            ps.setString(2, motivoLog);
            ps.setInt(3, idTurno);
            return ps.executeUpdate() > 0;
        }
    }

    public void actualizarEstadoTurno(Connection conn, int idTurno, int idEstado) throws SQLException {

        String sql = "UPDATE turno SET id_estado = ? WHERE id_turno = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idEstado);
            ps.setInt(2, idTurno);
            ps.executeUpdate();
        }
    }
    public boolean clienteExiste(int idCliente) throws SQLException {
        String sql = "SELECT 1 FROM cliente WHERE id_cliente = ?";
        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        }
    }




}