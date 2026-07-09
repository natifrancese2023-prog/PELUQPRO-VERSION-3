package controllers;

import claseslogicas.*;
import dao.*;
import service.ClienteService;
import service.EstadoFacturaInvalidoException;
import service.FacturaService;
import service.VisitaService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import utilidades.AlertaUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class FacturaController {

    @FXML private Label lblCliente;
    @FXML private ListView<Servicio> lvServicios;
    @FXML private ComboBox<String> cbFormaPago;
    @FXML private Label lblTotal;
    @FXML private Button btnConfirmar;

    private BigDecimal totalCalculado;
    private BigDecimal montoFinal;

    private final VisitaService visitaService = new VisitaService();
    private final ClienteService clienteService = new ClienteService();
    private final FacturaService facturaService = new FacturaService();

    private Visita visitaActual;
    private Cliente clienteActual;
    private Factura facturaGenerada;


    private Factura facturaACobrar;
    private boolean modoCobro = false;


    public void cargarFacturaParaCobro(Factura factura) {
        this.modoCobro = true;
        this.facturaACobrar = factura;

        Cliente cliente = factura.getCliente();
        if (cliente != null) {
            lblCliente.setText(cliente.getNombre() + " " + cliente.getApellido());
        }

        // Los servicios de una Factura cargada desde el listado vienen en
        // getDetalles() (DetalleFactura), no en getServicios() -ese campo
        // solo se llena al construir la Factura en memoria desde una Visita-.
        // Se adaptan a Servicio acá mismo para reusar el mismo ListView/celda.
        List<Servicio> serviciosParaMostrar = new ArrayList<>();
        for (DetalleFactura d : factura.getDetalles()) {
            Servicio s = new Servicio();
            if (d.getServicio() != null) {
                s.setIdServicio(d.getServicio().getIdServicio());
                s.setNombreServicio(d.getServicio().getNombreServicio());
            }
            s.setPrecio(d.getPrecioUnitario() != null ? d.getPrecioUnitario().doubleValue() : 0.0);
            serviciosParaMostrar.add(s);
        }
        lvServicios.setItems(FXCollections.observableArrayList(serviciosParaMostrar));
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

        totalCalculado = factura.getMontoTotal();

        cbFormaPago.setItems(FXCollections.observableArrayList("Transferencia", "Efectivo", "Débito/Crédito"));
        cbFormaPago.getSelectionModel().selectFirst();
        cbFormaPago.setOnAction(e -> calcularMontoFinalCobro());

        if (btnConfirmar != null) {
            btnConfirmar.setText("Confirmar cobro");
        }

        calcularMontoFinalCobro();
    }

    private void calcularMontoFinalCobro() {
        String nombreMetodo = cbFormaPago.getValue();
        try {
            montoFinal = facturaService.calcularMontoFinal(totalCalculado, nombreMetodo);
        } catch (SQLException e) {
            System.err.println("Error al consultar método de pago: " + e.getMessage());
            montoFinal = totalCalculado;
        }
        lblTotal.setText("Total: $" + montoFinal.setScale(2, RoundingMode.HALF_UP));
    }

    public void cargarFacturaDesdeVisita(int idVisita) {
        visitaActual = visitaService.obtenerVisitaPorId(idVisita);
        if (visitaActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"No se encontró la visita.");
            return;
        }

        try {
            clienteActual = clienteService.obtenerPorId(visitaActual.getIdCliente());
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
        totalCalculado = facturaService.calcularTotalServicios(visitaActual.getServiciosRealizados());
    }
    private void calcularMontoFinal() {
        String nombreMetodo = cbFormaPago.getValue();
        calcularTotalServicios();

        try {
            montoFinal = facturaService.calcularMontoFinal(totalCalculado, nombreMetodo);
        } catch (SQLException e) {
            System.err.println("Error al consultar método de pago: " + e.getMessage());
            montoFinal = totalCalculado;
        }

        lblTotal.setText("Total: $" + montoFinal.setScale(2, RoundingMode.HALF_UP));
    }

    @FXML
    private void handleConfirmarFactura() {
        btnConfirmar.setDisable(true);
        if (modoCobro) {
            cobrarFacturaExistente();
        } else {
            guardarFacturaEnBaseDeDatos();
        }
    }

    private void cobrarFacturaExistente() {
        btnConfirmar.setDisable(true);
        try {
            String nombreMetodo = cbFormaPago.getValue();
            calcularMontoFinalCobro();

            facturaService.cobrarFactura(facturaACobrar.getIdFactura(), nombreMetodo, montoFinal);

            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", null,
                    "Factura cobrada correctamente.");
            btnConfirmar.getScene().getWindow().hide();

        } catch (EstadoFacturaInvalidoException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Operación no permitida", null, e.getMessage());
            btnConfirmar.setDisable(false);
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error SQL", null,
                    "No se pudo registrar el cobro:\n" + e.getMessage());
            btnConfirmar.setDisable(false);
        }
    }

    private void guardarFacturaEnBaseDeDatos() {
        // 🔒 Deshabilitamos el botón ANTES de guardar para evitar doble clic
        btnConfirmar.setDisable(true);

        try {
            String nombreMetodo = cbFormaPago.getValue();
            calcularMontoFinal();

            FacturaService.ResultadoFactura resultado = facturaService.registrarFactura(
                    visitaActual, clienteActual.getIdCliente(), nombreMetodo, montoFinal);

            switch (resultado.estado()) {
                case OK -> {
                    facturaGenerada = resultado.factura();
                    AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", null,
                            "Factura registrada correctamente y turno marcado como Facturado.");
                }
                case METODO_INVALIDO -> AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,
                        "Método de pago no válido.");
                case TURNO_INVALIDO -> AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,
                        "El turno asociado no es válido.");
                case YA_PAGADA -> AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Turno ya facturado", null,
                        "Este turno ya tiene una factura marcada como PAGADA.");
            }

        } catch (SQLException e) {
            // ⚠️ Detectamos violación de UNIQUE en PostgreSQL (duplicado)
            if ("23505".equals(e.getSQLState())) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING,
                        "Duplicado",
                        null,
                        "Ya existe una factura registrada para este turno.");
                System.err.println("⚠️ Intento de facturar dos veces el mismo turno.");
            } else {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR,
                        "Error SQL",
                        null,
                        "No se pudo guardar la factura:\n" + e.getMessage());
                System.err.println("🧨 Error completo: " + e);
            }
        } finally {
            // 🔄 Rehabilitamos el botón para que el usuario pueda seguir usando la interfaz
            btnConfirmar.setDisable(false);
        }
    }

}