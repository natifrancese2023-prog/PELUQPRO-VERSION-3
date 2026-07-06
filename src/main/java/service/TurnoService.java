package service;

import claseslogicas.*;
import dao.EmpleadoDAO;
import dao.HorarioAtencionDAO;
import dao.TurnoDAO;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio del dominio Turno.
 * <p>
 * Antes de esta clase, {@code TurnoDAO.obtenerTurnosDisponibles()} coordinaba
 * directamente 3 DAOs distintos (HorarioAtencionDAO, EmpleadoDAO, y el propio
 * TurnoDAO para validarDisponibilidad) — un DAO no debería orquestar otros
 * DAOs, esa es la responsabilidad de un Service. Y {@code GestionDiariaController}
 * tenía la lógica de "¿puede facturarse este turno?" (Finalizado + tiene
 * visita + no tiene factura) coordinando visitaDAO y FacturaDAO directo desde
 * un controller de UI.
 */
public class TurnoService {

    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final HorarioAtencionDAO horarioDAO = new HorarioAtencionDAO();
    private final VisitaService visitaService = new VisitaService();
    private final FacturaService facturaService = new FacturaService();

    private static final int INTERVALO_MINUTOS = 30;

    /**
     * Busca bloques horarios disponibles para agendar un turno, recorriendo
     * el horario de atención del día en intervalos de 30 minutos y
     * descartando los que no entran en el horario o que ya están ocupados.
     * (Antes vivía en TurnoDAO.obtenerTurnosDisponibles()).
     */
    public List<BloqueDisponible> buscarDisponibilidad(LocalDate fecha, int duracionTotalMinutos, Integer idEstilistaOpcional) throws SQLException {
        List<BloqueDisponible> disponibles = new ArrayList<>();

        HorarioAtencion horario = horarioDAO.obtenerHorarioPorDia(fecha);
        if (horario == null || horario.getHoraApertura().equals(horario.getHoraCierre())) {
            return disponibles;
        }

        LocalTime inicioDia = horario.getHoraApertura();
        LocalTime finDia = horario.getHoraCierre();

        List<Empleado> estilistas = (idEstilistaOpcional != null)
                ? empleadoDAO.obtenerEstilistasPorId(idEstilistaOpcional)
                : empleadoDAO.obtenerEstilistas();

        for (Empleado empleado : estilistas) {
            LocalTime horaActual = inicioDia;

            while (!horaActual.plusMinutes(duracionTotalMinutos).isAfter(finDia)) {
                boolean disponible = turnoDAO.validarDisponibilidad(
                        empleado.getIdEmpleado(), fecha, horaActual, duracionTotalMinutos);

                if (disponible) {
                    disponibles.add(new BloqueDisponible(
                            horaActual, horaActual.plusMinutes(duracionTotalMinutos), empleado));
                }

                horaActual = horaActual.plusMinutes(INTERVALO_MINUTOS);
            }
        }

        return disponibles;
    }

    /**
     * Registra un turno nuevo. Todo turno nace en estado PENDIENTE — esa
     * regla ahora vive acá en vez de que cada pantalla que crea un turno
     * tenga que acordarse de setearla (antes AltaTurnoController lo hacía
     * "a mano" con setEstadoLogico, de forma redundante con lo que el DAO
     * ya hardcodeaba en el INSERT).
     */
    public boolean registrarTurno(Turno turno) throws SQLException {
        turno.setEstadoLogico(EstadoTurno.PENDIENTE);
        return turnoDAO.insertarTurno(turno);
    }

    /**
     * Cambia el estado de un turno, validando primero que la transición sea
     * válida según las reglas del propio modelo Turno
     * ({@link Turno#puedeCambiarA(EstadoTurno)}). Si la transición no es
     * válida, lanza IllegalStateException con un mensaje listo para mostrar.
     */
    public void cambiarEstado(Turno turno, EstadoTurno nuevoEstado, String motivo) throws SQLException {
        if (!turno.puedeCambiarA(nuevoEstado)) {
            throw new IllegalStateException(
                    "No se puede cambiar de " + turno.getEstadoTurno().getNombre() + " a " + nuevoEstado.getNombre());
        }

        turnoDAO.actualizarEstado(turno.getIdTurno(), nuevoEstado, motivo);
        turno.setEstadoTurno(nuevoEstado);
        turno.setMotivoLog(motivo);
    }

    /**
     * Un turno se puede facturar si está FINALIZADO, tiene una visita
     * registrada, y todavía no tiene una factura asociada. Antes esta
     * coordinación (visitaDAO + FacturaDAO) vivía directo en
     * GestionDiariaController.actualizarEstadoBotones().
     */
    public boolean puedeFacturar(Turno turno) {
        if (turno == null || turno.getEstadoTurno() != EstadoTurno.FINALIZADO) {
            return false;
        }

        Visita visita = visitaService.obtenerVisitaPorTurno(turno.getIdTurno());
        if (visita == null) {
            return false;
        }

        try {
            return facturaService.obtenerPorTurno(turno.getIdTurno()) == null;
        } catch (SQLException e) {
            System.err.println("Error al verificar factura existente: " + e.getMessage());
            return false;
        }
    }

    /** Devuelve la visita asociada a un turno, o null si no tiene una registrada. */
    public Visita obtenerVisitaDeTurno(int idTurno) {
        return visitaService.obtenerVisitaPorTurno(idTurno);
    }

    public List<Turno> obtenerAgenda(LocalDate fecha, Integer idEmpleado) throws SQLException {
        return turnoDAO.obtenerTurnosFiltrados(fecha, idEmpleado);
    }

    public List<Turno> obtenerTurnosPorCliente(int idCliente) throws SQLException {
        return turnoDAO.obtenerTurnosPorCliente(idCliente);
    }
}