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

/**
 * Reglas de negocio del dominio Factura: cálculo de monto y coordinación de
 * alta (chequeo de "¿ya está pagada?" + guardado).
 * <p>
 * Antes todo esto vivía directo en FacturaController, mezclado con el código
 * de UI (JavaFX). El cálculo del monto final en particular ya usaba
 * BigDecimal con RoundingMode explícito — eso se mantiene igual, solo se
 * relocalizó.
 */
public class FacturaService {

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

    /**
     * Aplica el porcentaje modificador del método de pago (recargo/descuento)
     * sobre el total de servicios. Si el método no existe, devuelve el total
     * sin modificar (mismo comportamiento que tenía el controller).
     */
    public BigDecimal calcularMontoFinal(BigDecimal totalServicios, String nombreMetodoPago) throws SQLException {
        MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodoPago);
        if (metodo == null) {
            return totalServicios;
        }

        BigDecimal porcentaje = BigDecimal.valueOf(metodo.getPorcentajeModificador());
        return totalServicios.add(
                totalServicios.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
        );
    }

    /**
     * Registra la factura de una visita, validando primero que el método de
     * pago exista, que el turno asociado sea válido, y que el turno no tenga
     * ya una factura marcada como Pagada.
     */
    public ResultadoFactura registrarFactura(Visita visita, int idCliente, String nombreMetodoPago, BigDecimal montoFinal) throws SQLException {
        MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodoPago);
        if (metodo == null) {
            return new ResultadoFactura(ResultadoFactura.Estado.METODO_INVALIDO, null);
        }

        int idTurno = visita.getIdTurno();
        if (idTurno <= 0) {
            return new ResultadoFactura(ResultadoFactura.Estado.TURNO_INVALIDO, null);
        }

        Factura facturaExistente = facturaDAO.obtenerPorTurno(idTurno);
        if (facturaExistente != null
                && facturaExistente.getEstadoFactura() != null
                && facturaExistente.getEstadoFactura().getIdEstadoFactura() == ID_ESTADO_PAGADA) {
            return new ResultadoFactura(ResultadoFactura.Estado.YA_PAGADA, null);
        }

        Factura facturaGenerada = new Factura(visita, nombreMetodoPago);
        facturaGenerada.setMontoTotal(montoFinal);
        facturaGenerada.setIdCliente(idCliente);
        facturaGenerada.setEstadoFactura(new EstadoFactura(ID_ESTADO_PAGADA, "Pagada"));

        // El guardado ya deja el turno en estado FACTURADO dentro de la misma
        // transacción (ver FacturaDAO.guardarFactura) — no hace falta tocar
        // el estado del turno acá.
        facturaDAO.guardarFactura(facturaGenerada, idTurno, metodo.getIdMetodo());

        return new ResultadoFactura(ResultadoFactura.Estado.OK, facturaGenerada);
    }

    public List<Factura> obtenerPorRango(LocalDate desde, LocalDate hasta) {
        return FacturaDAO.obtenerPorRango(desde, hasta);
    }

    public Factura obtenerPorTurno(int idTurno) throws SQLException {
        return facturaDAO.obtenerPorTurno(idTurno);
    }
}