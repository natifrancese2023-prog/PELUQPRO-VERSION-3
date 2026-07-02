package controllers;

import claseslogicas.*;
import dao.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utilidades.AlertaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.List;
import utilidades.AlertaUtil;

public class FacturaController {

    @FXML private Label lblCliente;
    @FXML private ListView<Servicio> lvServicios;
    @FXML private ComboBox<String> cbFormaPago;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmar;

    private BigDecimal totalCalculado;
    private BigDecimal montoFinal;


    private final visitaDAO visitaDAO = new visitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();

    private Visita visitaActual;
    private Cliente clienteActual;
    private Factura facturaGenerada;

    public void cargarFacturaDesdeVisita(int idVisita) {
        visitaActual = visitaDAO.obtenerVisitaPorId(idVisita);
        if (visitaActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"No se encontró la visita.");
            return;
        }

        try {
            clienteActual = clienteDAO.obtenerPorId(visitaActual.getIdCliente());
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"No se pudo obtener el cliente.");
            return;
        }

        lblCliente.setText(
                clienteActual.getNombre() + " " +
                        clienteActual.getApellido() + " - " +
                        clienteActual.getNombreTipoDocumento() + " " +
                        clienteActual.getNumeroDocumento()
        );

        lvServicios.setItems(FXCollections.observableArrayList(visitaActual.getServiciosRealizados()));
        lvServicios.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Servicio servicio, boolean empty) {
                super.updateItem(servicio, empty);
                if (empty || servicio == null) {
                    setText(null);
                } else {
                    setText(servicio.getNombreServicio() + " - $" + String.format("%.2f", servicio.getPrecio()));
                }
            }
        });

        cbFormaPago.setItems(FXCollections.observableArrayList("Transferencia", "Efectivo", "Débito/Crédito"));
        cbFormaPago.getSelectionModel().selectFirst();
        cbFormaPago.setOnAction(e -> calcularMontoFinal());

        calcularMontoFinal();
    }
    public void calcularTotalServicios() {
        if (visitaActual == null) return;

        List<Servicio> servicios = visitaActual.getServiciosRealizados();
        totalCalculado = servicios.stream()
                .map(servicio -> BigDecimal.valueOf(servicio.getPrecio()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    private void calcularMontoFinal() {
        String nombreMetodo = cbFormaPago.getValue();
        calcularTotalServicios();

        try {
            MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodo);
            if (metodo != null) {
                BigDecimal porcentaje = BigDecimal.valueOf(metodo.getPorcentajeModificador());
                montoFinal = totalCalculado.add(
                        totalCalculado.multiply(porcentaje).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP)
                );
            } else {
                montoFinal = totalCalculado;
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar método de pago: " + e.getMessage());
            montoFinal = totalCalculado;
        }

        lblTotal.setText("Total: $" + montoFinal.setScale(2, RoundingMode.HALF_UP));
    }

    @FXML
    private void handleConfirmarFactura() {
        guardarFacturaEnBaseDeDatos();
    }

    private void guardarFacturaEnBaseDeDatos() {
        try {
            String nombreMetodo = cbFormaPago.getValue();
            MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodo);
            if (metodo == null) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"Método de pago no válido.");
                return;
            }

            int idTurno = visitaActual.getIdTurno();
            if (idTurno <= 0) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"El turno asociado no es válido.");
                return;
            }

            Factura facturaExistente = facturaDAO.obtenerPorTurno(idTurno);
            if (facturaExistente != null &&
                    facturaExistente.getEstadoFactura() != null &&
                    facturaExistente.getEstadoFactura().getIdEstadoFactura() == 2) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Turno ya facturado", null,"Este turno ya tiene una factura marcada como PAGADA.");
                return;
            }

            calcularMontoFinal();

            facturaGenerada = new Factura(visitaActual, nombreMetodo);
            facturaGenerada.setMontoTotal(montoFinal);
            facturaGenerada.setIdCliente(clienteActual.getIdCliente());

            // Estado de la factura → Pagada
            facturaGenerada.setEstadoFactura(new EstadoFactura(2, "Pagada"));

            // Guardar factura (esto ya deja el turno en estado FACTURADO dentro
            // de la misma transacción — ver FacturaDAO.guardarFactura). No se
            // vuelve a tocar el estado del turno acá para evitar una segunda
            // actualización redundante y una conexión aparte sin cerrar.
            facturaDAO.guardarFactura(facturaGenerada, idTurno, metodo.getIdMetodo());

            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", null,"Factura registrada correctamente y turno marcado como Facturado.");
            btnConfirmar.setDisable(true);

        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", null,"No se pudo guardar la factura:\n" + e.getMessage());
            System.err.println("🧨 Error completo: " + e);
        }
    }

}