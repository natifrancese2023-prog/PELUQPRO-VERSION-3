package service;

import claseslogicas.EstadoFactura;
import claseslogicas.Factura;
import claseslogicas.MetodoPago;
import claseslogicas.Servicio;
import claseslogicas.Visita;
import dao.FacturaDAO;
import dao.MetodoPagoDAO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FacturaService {

    private static final Logger log = LoggerFactory.getLogger(FacturaService.class);

    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    private static final int ID_ESTADO_PAGADA = 2;

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
                && facturaExistente.getEstadoFactura().getIdEstadoFactura() == ID_ESTADO_PAGADA) {
            log.info("Factura ya pagada para turno={}", idTurno);
            return new ResultadoFactura(ResultadoFactura.Estado.YA_PAGADA, null);
        }

        Factura facturaGenerada = new Factura(visita, nombreMetodoPago);
        facturaGenerada.setMontoTotal(montoFinal);
        facturaGenerada.setIdCliente(idCliente);
        facturaGenerada.setEstadoFactura(new EstadoFactura(ID_ESTADO_PAGADA, "Pagada"));

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
}
