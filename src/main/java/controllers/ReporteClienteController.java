package controllers;

import claseslogicas.ExportadorExcel;
import claseslogicas.ExportadorReportes;
import clasesreportes.ClienteReporteExtendido;
import dao.ReporteDAO;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ReporteClienteController {

    @FXML private TableView<ClienteReporteExtendido> tablaClientes;
    @FXML private TableColumn<ClienteReporteExtendido, String> colNombre;
    @FXML private TableColumn<ClienteReporteExtendido, String> colTelefono;
    @FXML private TableColumn<ClienteReporteExtendido, String> colEmail;
    @FXML private TableColumn<ClienteReporteExtendido, Integer> colVisitas;
    @FXML private TableColumn<ClienteReporteExtendido, Double> colGasto;
    @FXML private TableColumn<ClienteReporteExtendido, String> colEstadoTurno;

    @FXML private ComboBox<String> cbFiltro;
    @FXML private BarChart<String, Number> graficoClientes;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private List<ClienteReporteExtendido> datosClientes;

    public void initialize() {
        configurarTabla();
        configurarFiltro();
        cargarDatosClientes();
    }

    private void configurarTabla() {
        colNombre.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getNombreCompleto()));
        colTelefono.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTelefono()));
        colEmail.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));
        colVisitas.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getCantidadVisitas()).asObject());
        colGasto.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getGastoTotal()).asObject());
        colEstadoTurno.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEstadoUltimoTurno()));
    }

    private void configurarFiltro() {
        cbFiltro.setItems(FXCollections.observableArrayList("Todos", "Frecuentes", "Mayor gasto"));
        cbFiltro.getSelectionModel().selectFirst();
        cbFiltro.setOnAction(e -> actualizarVisualizacion());
    }

    private void cargarDatosClientes() {
        try {
            datosClientes = reporteDAO.obtenerDatosClientesExtendido();
            tablaClientes.setItems(FXCollections.observableArrayList(datosClientes));
            actualizarVisualizacion();
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar datos", "No se pudieron obtener los datos de clientes.");
            System.err.println("🧨 Error SQL: " + e.getMessage());
        }
    }

    private void actualizarVisualizacion() {
        String filtro = cbFiltro.getValue();
        List<ClienteReporteExtendido> filtrados;

        switch (filtro) {
            case "Frecuentes" -> filtrados = datosClientes.stream()
                    .filter(c -> c.getCantidadVisitas() >= 2)
                    .limit(10)
                    .collect(Collectors.toList());
            case "Mayor gasto" -> filtrados = datosClientes.stream()
                    .sorted(Comparator.comparing(ClienteReporteExtendido::getGastoTotal).reversed())
                    .limit(10)
                    .collect(Collectors.toList());
            default -> filtrados = datosClientes;
        }

        tablaClientes.setItems(FXCollections.observableArrayList(filtrados));
        actualizarGrafico(filtrados);
    }

    private void actualizarGrafico(List<ClienteReporteExtendido> clientes) {
        graficoClientes.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Gasto total por cliente");

        for (ClienteReporteExtendido c : clientes) {
            serie.getData().add(new XYChart.Data<>(c.getNombreCompleto(), c.getGastoTotal()));
        }

        graficoClientes.getData().add(serie);
    }
    @FXML
    private void exportarPDF(ActionEvent event) {
        if (tablaClientes.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", "No hay clientes para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            String tipoReporte = cbFiltro.getValue();
            WritableImage imagenGrafico = graficoClientes.snapshot(new SnapshotParameters(), null);
            ExportadorReportes.exportarClientesPDF(tablaClientes.getItems(), archivo, tipoReporte, imagenGrafico);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", "El reporte fue exportado correctamente.");
        }
    }


    @FXML
    private void exportarExcel(ActionEvent event) {
        if (tablaClientes.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", "No hay clientes para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo Excel", "*.xlsx"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            try {
                ExportadorExcel.exportarClientesXLSX(tablaClientes.getItems(), archivo);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", "El reporte fue exportado correctamente a Excel.");
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", "Ocurrió un error al generar el archivo.");
                e.printStackTrace();
            }
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
