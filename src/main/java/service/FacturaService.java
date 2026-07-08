package service;

import claseslogicas.EstadoFactura;
import claseslogicas.Factura;
import claseslogicas.MetodoPago;
import claseslogicas.Servicio;
import claseslogicas.Visita;
import dao.ConexionBD;
import dao.FacturaDAO;
import dao.MetodoPagoDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    // Renombrado de ID_ESTADO_PAGADA a ID_ESTADO_FACTURADA: el valor (2) sigue
    // siendo el mismo y la lógica de registrarFactura no cambia, pero el nombre
    // original inducía a pensar que id=2 era "pagada" cuando en realidad es
    // el id de FACTURADA (factura emitida, no necesariamente cobrada). Con el
    // agregado del estado PAGADA en el enum, mantener el nombre viejo hubiera
    // sido confuso/incorrecto.
    private static final int ID_ESTADO_FACTURADA = EstadoFactura.FACTURADA.getIdEstadoFactura();

    public record ResultadoFactura(Estado estado, Factura factura) {
        public enum Estado { OK, YA_PAGADA, METODO_INVALIDO, TURNO_INVALIDO }
    }

    public BigDecimal calcularTotalServicios(List<Servicio> servicios) {
        return servicios.stream()
                .map(s -> BigDecimal.valueOf(s.getPrecio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal calcularMontoFinal(BigDecimal totalServicios, String nombreMetodoPago) throws SQLException {
        MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodoPago);
        if (metodo == null) {
            log.warn("Método de pago '{}' no encontrado, se aplica total sin modificar", nombreMetodoPago);
            return totalServicios;
        }

        BigDecimal porcentaje = BigDecimal.valueOf(metodo.getPorcentajeModificador());
        BigDecimal montoFinal = totalServicios.add(
                totalServicios.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        );
        log.info("Monto final calculado con método={} total={}", nombreMetodoPago, montoFinal);
        return montoFinal;
    }

    public ResultadoFactura registrarFactura(Visita visita, int idCliente, String nombreMetodoPago, BigDecimal montoFinal) throws SQLException {
        MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodoPago);
        if (metodo == null) {
            log.warn("Intento de registrar factura con método inválido '{}'", nombreMetodoPago);
            return new ResultadoFactura(ResultadoFactura.Estado.METODO_INVALIDO, null);
        }

        int idTurno = visita.getIdTurno();
        if (idTurno <= 0) {
            log.warn("Intento de registrar factura con turno inválido id={}", idTurno);
            return new ResultadoFactura(ResultadoFactura.Estado.TURNO_INVALIDO, null);
        }

        Factura facturaExistente = facturaDAO.obtenerPorTurno(idTurno);
        if (facturaExistente != null
                && facturaExistente.getEstadoFactura() != null
                && facturaExistente.getEstadoFactura().getIdEstadoFactura() == ID_ESTADO_FACTURADA) {
            log.info("Factura ya facturada para turno={}", idTurno);
            return new ResultadoFactura(ResultadoFactura.Estado.YA_PAGADA, null);
        }

        Factura facturaGenerada = new Factura(visita, nombreMetodoPago);
        facturaGenerada.setMontoTotal(montoFinal);
        facturaGenerada.setIdCliente(idCliente);
        facturaGenerada.setEstadoFactura(EstadoFactura.FACTURADA);


        facturaDAO.guardarFactura(facturaGenerada, idTurno, metodo.getIdMetodo());
        log.info("Factura registrada OK idTurno={} idCliente={}", idTurno, idCliente);

        return new ResultadoFactura(ResultadoFactura.Estado.OK, facturaGenerada);
    }

    public List<Factura> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        return FacturaDAO.obtenerPorRango(desde, hasta);
    }

    public Factura obtenerPorTurno(int idTurno) throws SQLException {
        return facturaDAO.obtenerPorTurno(idTurno);
    }

    // 🔹 Nuevo: obtener todas las facturas
    public List<Factura> obtenerTodas() {
        return FacturaDAO.obtenerTodas();
    }

    /**
     * FIX (flujo de estado): antes reasignaba EstadoFactura.FACTURADA, que ya
     * era el estado con el que se creaba la factura en registrarFactura(), por
     * lo que "marcar como pagada" no cambiaba nada. Ahora:
     * 1) Busca el estado real de la factura por id (antes no existía forma de hacerlo).
     * 2) Valida con EstadoFactura.puedeTransicionarA(...) que la transición sea válida
     *    (ej: no se puede pagar una factura ya ANULADA).
     * 3) Recién ahí actualiza a PAGADA (estado nuevo, antes no existía).
     */
    public void marcarComoPagada(int idFactura) throws SQLException {
        Factura factura = facturaDAO.obtenerPorId(idFactura);
        if (factura == null) {
            throw new EstadoFacturaInvalidoException("No existe una factura con id " + idFactura);
        }
        if (!factura.getEstadoFactura().puedeTransicionarA(EstadoFactura.PAGADA)) {
            throw new EstadoFacturaInvalidoException(
                    "No se puede marcar como pagada una factura en estado " + factura.getEstadoFactura().getNombre());
        }

        try (Connection conn = ConexionBD.getConnection()) {
            facturaDAO.actualizarEstadoFactura(conn, idFactura, EstadoFactura.PAGADA);
            log.info("Factura marcada como PAGADA idFactura={}", idFactura);
        }
    }

    /**
     * FIX (flujo de estado): mismo problema que marcarComoPagada, pero para
     * la transición hacia ANULADA. Antes se podía anular una factura ya
     * anulada o incluso una ya pagada sin ningún control.
     */
    public void cancelarFactura(int idFactura) throws SQLException {
        Factura factura = facturaDAO.obtenerPorId(idFactura);
        if (factura == null) {
            throw new EstadoFacturaInvalidoException("No existe una factura con id " + idFactura);
        }
        if (!factura.getEstadoFactura().puedeTransicionarA(EstadoFactura.ANULADA)) {
            throw new EstadoFacturaInvalidoException(
                    "No se puede cancelar una factura en estado " + factura.getEstadoFactura().getNombre());
        }

        try (Connection conn = ConexionBD.getConnection()) {
            facturaDAO.actualizarEstadoFactura(conn, idFactura, EstadoFactura.ANULADA);
            log.info("Factura CANCELADA idFactura={}", idFactura);
        }
    }
}