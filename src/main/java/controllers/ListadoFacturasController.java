package controllers;

import claseslogicas.*;
import service.EstadoFacturaInvalidoException;
import service.FacturaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import utilidades.AlertaUtil;

// FIX (flujo de estado): el import original era java.awt.event.ActionEvent,
// que no es compatible con los métodos @FXML de JavaFX. Esto hacía que el
// FXMLLoader no pudiera resolver correctamente los handlers de los botones
// "Marcar como pagada" / "Cancelar factura" (fallaba al cargar el FXML o
// simplemente no se disparaban los eventos).
import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ListadoFacturasController {

    private final FacturaService facturaService = new FacturaService();

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtFiltroCliente;
    @FXML private ComboBox<String> cmbFiltroMetodo;
    @FXML private Button btnBuscar;
    @FXML private Button btnVerPDF;
    @FXML private Button btnMarcarPagada;
    @FXML private Button btnCancelarFactura;

    @FXML private TableView<Factura> tblFacturas;
    @FXML private TableColumn<Factura, Integer> colId;
    @FXML private TableColumn<Factura, String> colCliente;
    @FXML private TableColumn<Factura, String> colFecha;
    @FXML private TableColumn<Factura, String> colMetodo;
    @FXML private TableColumn<Factura, Double> colTotal;
    @FXML private TableColumn<Factura, String> colEstado;
    @FXML
    private ComboBox<EstadoFactura> cmbFiltroEstado;

    private ObservableList<Factura> todasLasFacturas = FXCollections.observableArrayList();
    private FilteredList<Factura> facturasFiltradas;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idFactura"));
        colFecha.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFechaHora().toLocalDate().toString()));
        colCliente.setCellValueFactory(data -> {
            Cliente c = data.getValue().getCliente();
            if (c == null) return new javafx.beans.property.SimpleStringProperty("");
            return new javafx.beans.property.SimpleStringProperty(
                    c.getNombre() + " " + c.getApellido());
        });
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("montoTotal"));
        colEstado.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEstadoFacturaNombre()));

        dpDesde.setValue(LocalDate.now().minusMonths(1));
        dpHasta.setValue(LocalDate.now());

        cmbFiltroMetodo.getItems().addAll("Todos", "Efectivo", "Transferencia", "Débito/Crédito");
        cmbFiltroMetodo.getSelectionModel().selectFirst();

        btnVerPDF.setDisable(true);
        btnMarcarPagada.setDisable(true);
        btnCancelarFactura.setDisable(true);
        tblFacturas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nueva) -> {
                    // NUEVO: solo se pueden imprimir facturas ya PAGADA.
                    btnVerPDF.setDisable(!esImprimible(nueva));
                    // NUEVO (flujo de estado): "Cobrar" y "Anular" solo tienen
                    // sentido sobre una factura en estado FACTURADA -todavía
                    // no cobrada, todavía no anulada-. Una vez PAGADA o
                    // ANULADA son estados terminales (ver EstadoFactura).
                    boolean esFacturada = nueva != null && nueva.getEstadoFactura() == EstadoFactura.FACTURADA;
                    btnMarcarPagada.setDisable(!esFacturada);
                    btnCancelarFactura.setDisable(!esFacturada);
                });

        cmbFiltroEstado.getItems().add(null); // "Todos" los estados
        cmbFiltroEstado.getItems().addAll(EstadoFactura.values());
    }

    @FXML
    private void handleBuscar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Rango inválido", null, "Seleccioná un rango de fechas válido.");
            return;
        }

        List<Factura> resultado = facturaService.obtenerPorRango(desde, hasta);

        todasLasFacturas.setAll(resultado);
        facturasFiltradas = new FilteredList<>(todasLasFacturas, f -> true);
        tblFacturas.setItems(facturasFiltradas);
        aplicarFiltrosSecundarios();
    }

    @FXML
    private void handleAplicarFiltros() {
        if (facturasFiltradas == null) return;
        aplicarFiltrosSecundarios();
    }

    @FXML
    private void handleFiltrarPorEstado(ActionEvent event) {
        handleAplicarFiltros();
    }


    private void aplicarFiltrosSecundarios() {
        String cliente = txtFiltroCliente.getText().toLowerCase().trim();
        String metodo = cmbFiltroMetodo.getValue();
        EstadoFactura estado = cmbFiltroEstado.getValue();

        facturasFiltradas.setPredicate(f -> {
            boolean coincideCliente = cliente.isEmpty() ||
                    (f.getCliente() != null &&
                            f.getCliente().getNombreCompleto().toLowerCase().contains(cliente));


            boolean coincideMetodo = metodo == null || metodo.equals("Todos") ||
                    (f.getMetodoPago() != null && f.getMetodoPago().equalsIgnoreCase(metodo));

            boolean coincideEstado = estado == null || f.getEstadoFactura() == estado;

            return coincideCliente && coincideMetodo && coincideEstado;
        });
    }

    /**
     * NUEVO: solo se pueden imprimir/exportar a PDF las facturas ya cobradas
     * (PAGADA). No tiene sentido entregarle al cliente un comprobante de una
     * factura que todavía no se cobró.
     */
    private boolean esImprimible(Factura factura) {
        return factura != null && factura.getEstadoFactura() == EstadoFactura.PAGADA;
    }

    @FXML
    private void handleVerPDF() {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

        if (!esImprimible(seleccionada)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "No disponible", null,
                    "Solo se pueden imprimir facturas en estado Pagada. Esta factura está en estado: "
                            + seleccionada.getEstadoFacturaNombre());
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar factura PDF");
        chooser.setInitialFileName("Factura_" + seleccionada.getIdFactura() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File destino = chooser.showSaveDialog(tblFacturas.getScene().getWindow());
        if (destino != null) {
            try {
                ExportadorPDF.exportarFacturaIndividualPDF(seleccionada, destino);
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "PDF generado", null,
                        "Factura guardada en: " + destino.getAbsolutePath());
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null,
                        "No se pudo generar el PDF de la factura.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleMarcarPagada(ActionEvent event) {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", null,
                    "Seleccioná una factura de la tabla primero.");
            return;
        }
        if (seleccionada.getEstadoFactura() != EstadoFactura.FACTURADA) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "No disponible", null,
                    "Solo se pueden cobrar facturas en estado Facturada.");
            return;
        }
        abrirPantallaCobro(seleccionada);
    }

    private void abrirPantallaCobro(Factura factura) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/Factura.fxml"));
            Parent root = loader.load();

            FacturaController controller = loader.getController();
            controller.cargarFacturaParaCobro(factura);

            Stage stage = new Stage();
            stage.setTitle("Cobrar factura");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            // Al cerrar el modal (se cobró o se canceló), refrescar la tabla
            // para reflejar el nuevo estado sin que el usuario tenga que
            // volver a buscar manualmente.
            stage.setOnHidden(e -> refrescarTabla());
            stage.showAndWait();

        } catch (IOException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error al abrir pantalla", null,
                    "No se pudo cargar la pantalla de cobro.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCancelarFactura(ActionEvent event) {
        cambiarEstadoSeleccionada(
                idFactura -> facturaService.cancelarFactura(idFactura),
                "Factura cancelada",
                "Error al cancelar factura"
        );
    }

    @FunctionalInterface
    private interface OperacionEstado {
        void ejecutar(int idFactura) throws SQLException;
    }

    private void cambiarEstadoSeleccionada(OperacionEstado operacion, String mensajeOk, String mensajeErrorGenerico) {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", null,
                    "Seleccioná una factura de la tabla primero.");
            return;
        }

        try {
            operacion.ejecutar(seleccionada.getIdFactura());
            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Factura", null, mensajeOk);
            refrescarTabla();
        } catch (EstadoFacturaInvalidoException e) {
            // FIX (flujo de estado): transición no permitida (ej: pagar una
            // factura ya anulada). Antes no existía esta validación y el
            // UPDATE se ejecutaba igual, dejando datos inconsistentes.
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Operación no permitida", null, e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, mensajeErrorGenerico);
        }
    }

    private void refrescarTabla() {
        List<Factura> facturas = facturaService.obtenerTodas();
        tblFacturas.setItems(FXCollections.observableArrayList(facturas));
        facturasFiltradas = new FilteredList<>(FXCollections.observableArrayList(facturas), f -> true);
        tblFacturas.setItems(facturasFiltradas);
        aplicarFiltrosSecundarios();
    }
}