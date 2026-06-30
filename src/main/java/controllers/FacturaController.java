package controllers;

import claseslogicas.*;
import dao.*;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class FacturaController {

    @FXML private Label lblCliente;
    @FXML private ListView<Servicio> lvServicios;
    @FXML private ComboBox<String> cbFormaPago;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmar;

    private double totalCalculado;
    private double montoFinal;

    private final visitaDAO visitaDAO = new visitaDAO();
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();
    private final FacturaDAO facturaDAO = new FacturaDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO(); // ✅ para actualizar estado del turno

    private Visita visitaActual;
    private Cliente clienteActual;
    private Factura facturaGenerada;

    public void cargarFacturaDesdeVisita(int idVisita) {
        visitaActual = visitaDAO.obtenerVisitaPorId(idVisita);
        if (visitaActual == null) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se encontró la visita.");
            return;
        }

        try {
            clienteActual = clienteDAO.obtenerPorId(visitaActual.getIdCliente());
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo obtener el cliente.");
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
                .mapToDouble(Servicio::getPrecio)
                .sum();
    }

    private void calcularMontoFinal() {
        String nombreMetodo = cbFormaPago.getValue();
        calcularTotalServicios();

        try {
            MetodoPago metodo = metodoPagoDAO.obtenerPorNombre(nombreMetodo);
            if (metodo != null) {
                double porcentaje = metodo.getPorcentajeModificador();
                montoFinal = totalCalculado + (totalCalculado * porcentaje / 100);
            } else {
                montoFinal = totalCalculado;
            }
        } catch (SQLException e) {
            System.err.println("Error al consultar método de pago: " + e.getMessage());
            montoFinal = totalCalculado;
        }

        lblTotal.setText("Total: $" + String.format("%.2f", montoFinal));
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
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "Método de pago no válido.");
                return;
            }

            int idTurno = visitaActual.getIdTurno();
            if (idTurno <= 0) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error", "El turno asociado no es válido.");
                return;
            }

            Factura facturaExistente = facturaDAO.obtenerPorTurno(idTurno);
            if (facturaExistente != null &&
                    facturaExistente.getEstadoFactura() != null &&
                    facturaExistente.getEstadoFactura().getIdEstadoFactura() == 2) {
                mostrarAlerta(Alert.AlertType.WARNING, "Turno ya facturado", "Este turno ya tiene una factura marcada como PAGADA.");
                return;
            }

            calcularMontoFinal();

            facturaGenerada = new Factura(visitaActual, nombreMetodo);
            facturaGenerada.setMontoTotal(montoFinal);
            facturaGenerada.setIdCliente(clienteActual.getIdCliente());

            // Estado de la factura → Pagada
            facturaGenerada.setEstadoFactura(new EstadoFactura(2, "Pagada"));

            // Guardar factura
            facturaDAO.guardarFactura(facturaGenerada, idTurno, metodo.getIdMetodo());

            Connection conn = ConexionBD.getConnection();
            turnoDAO.actualizarEstadoTurno(conn, idTurno, 5);

            mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Factura registrada correctamente y turno marcado como Facturado.");
            btnConfirmar.setDisable(true);

        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", "No se pudo guardar la factura:\n" + e.getMessage());
            System.err.println("🧨 Error completo: " + e);
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
