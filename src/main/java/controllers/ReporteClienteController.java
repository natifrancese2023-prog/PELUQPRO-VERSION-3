package controllers;

import claseslogicas.ClienteReporteExtendido;
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
import utilidades.AlertaUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import claseslogicas.ExportadorReporte;
import claseslogicas.ExportadorExcel;
import claseslogicas.ExportadorPDF;

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
        colGasto.setCellValueFactory(data -> new SimpleDoubleProperty(
                data.getValue().getGastoTotal().doubleValue()).asObject()); // ✅ BigDecimal → double
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
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar datos", null, "No se pudieron obtener los datos de clientes.");
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
            // ✅ BigDecimal → doubleValue() para que el gráfico lo interprete como Number
            double gasto = c.getGastoTotal() != null
                    ? c.getGastoTotal().setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            serie.getData().add(new XYChart.Data<>(c.getNombreCompleto(), gasto));
        }

        graficoClientes.getData().add(serie);
    }


    @FXML
    private void exportarPDF(ActionEvent event) {
        if (tablaClientes.getItems().isEmpty()) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", null, "No hay clientes para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            try {
                ExportadorReporte exportador = new ExportadorPDF();
                exportador.exportarClientesReporte(tablaClientes.getItems(), archivo);
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", null, "El reporte fue exportado correctamente a PDF.");
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null, "Ocurrió un error al generar el archivo PDF.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void exportarExcel(ActionEvent event) {
        if (tablaClientes.getItems().isEmpty()) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", null, "No hay clientes para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo Excel", "*.xlsx"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            try {
                ExportadorReporte exportador = new ExportadorExcel();
                exportador.exportarClientesReporte(tablaClientes.getItems(), archivo);
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", null, "El reporte fue exportado correctamente a Excel.");
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null, "Ocurrió un error al generar el archivo Excel.");
                e.printStackTrace();
            }
        }
    }
}