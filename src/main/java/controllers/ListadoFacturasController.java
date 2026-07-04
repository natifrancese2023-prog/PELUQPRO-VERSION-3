package controllers;

import claseslogicas.*;
import dao.FacturaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import utilidades.AlertaUtil;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

public class ListadoFacturasController {

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtFiltroCliente;
    @FXML private ComboBox<String> cmbFiltroMetodo;
    @FXML private Button btnBuscar;
    @FXML private Button btnVerPDF;

    @FXML private TableView<Factura> tblFacturas;
    @FXML private TableColumn<Factura, Integer> colId;
    @FXML private TableColumn<Factura, String> colCliente;
    @FXML private TableColumn<Factura, String> colFecha;
    @FXML private TableColumn<Factura, String> colMetodo;
    @FXML private TableColumn<Factura, Double> colTotal;
    @FXML private TableColumn<Factura, String> colEstado;

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
        colEstado.setCellValueFactory(new PropertyValueFactory<>("estadoFacturaNombre"));

        dpDesde.setValue(LocalDate.now().minusMonths(1));
        dpHasta.setValue(LocalDate.now());

        cmbFiltroMetodo.getItems().addAll("Todos", "Efectivo", "Transferencia", "Débito/Crédito");
        cmbFiltroMetodo.getSelectionModel().selectFirst();

        btnVerPDF.setDisable(true);
        tblFacturas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nueva) -> btnVerPDF.setDisable(nueva == null));
    }

    @FXML
    private void handleBuscar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Rango inválido", null, "Seleccioná un rango de fechas válido.");
            return;
        }

        List<Factura> resultado = FacturaDAO.obtenerPorRango(desde, hasta);

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

    private void aplicarFiltrosSecundarios() {
        String cliente = txtFiltroCliente.getText().toLowerCase().trim();
        String metodo = cmbFiltroMetodo.getValue();

        facturasFiltradas.setPredicate(f -> {
            boolean coincideCliente = cliente.isEmpty() ||
                    (f.getCliente() != null &&
                            f.getCliente().getNombreCompleto().toLowerCase().contains(cliente));

            boolean coincideMetodo = metodo == null || metodo.equals("Todos") ||
                    f.getMetodoPago().equalsIgnoreCase(metodo);

            return coincideCliente && coincideMetodo;
        });
    }

    @FXML
    private void handleVerPDF() {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

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
}