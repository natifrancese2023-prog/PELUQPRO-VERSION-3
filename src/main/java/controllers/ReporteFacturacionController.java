package controllers;

import dao.ReporteFacturacionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.stage.FileChooser;
import claseslogicas.FacturaResumen;
import claseslogicas.ExportadorReporte;
import claseslogicas.ExportadorPDF;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ReporteFacturacionController {

    @FXML private DatePicker fechaInicio;
    @FXML private DatePicker fechaFin;
    @FXML private LineChart<String, Number> graficoFacturacion;
    @FXML private BarChart<String, Number> graficoMetodosPago;
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;
    @FXML private TableView<FacturaResumen> tablaResumen;
    @FXML private TableColumn<FacturaResumen, String> colFecha;
    @FXML private TableColumn<FacturaResumen, String> colTotal;
    @FXML private TableColumn<FacturaResumen, String> colMetodos;

    private final ReporteFacturacionDAO dao = new ReporteFacturacionDAO();

    @FXML
    public void initialize() {
        graficoFacturacion.setTitle("Evolución diaria de facturación");
        graficoMetodosPago.setTitle("Métodos de pago más usados");
        graficoFacturacion.setAnimated(false);
        graficoMetodosPago.setAnimated(false);
        graficoFacturacion.setLegendVisible(false);
        graficoMetodosPago.setLegendVisible(false);
        graficoFacturacion.setCreateSymbols(false);
        graficoFacturacion.setVisible(false);
        graficoMetodosPago.setVisible(false);
        tablaResumen.setVisible(false);

        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha().toString()));
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getTotalFacturado()
                        .setScale(2, java.math.RoundingMode.HALF_UP)
                        .toPlainString()   // ✅ BigDecimal → String con 2 decimales
        ));
        colMetodos.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResumenMetodosPago()));

        System.out.println("Inicialización completa del panel de reporte.");
    }

    @FXML
    public void generarReporte() {
        LocalDate inicio = fechaInicio.getValue();
        LocalDate fin = fechaFin.getValue();

        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            mostrarAlerta(Alert.AlertType.ERROR, "Fechas inválidas", null, "Seleccioná un período válido.");
            return;
        }

        Map<LocalDate, BigDecimal> facturacion = dao.obtenerFacturacionPorDia(inicio, fin);
        Map<String, Integer> metodos = dao.obtenerUsoMetodosPago(inicio, fin);

        cargarGraficoFacturacion(facturacion);
        cargarGraficoMetodosPago(metodos);
        cargarTablaResumen(facturacion, metodos);

        graficoFacturacion.setVisible(true);
        graficoMetodosPago.setVisible(true);
        tablaResumen.setVisible(true);
    }

    private void cargarGraficoFacturacion(Map<LocalDate, BigDecimal> datos) {
        graficoFacturacion.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Total facturado");

        for (Map.Entry<LocalDate, BigDecimal> entry : datos.entrySet()) {
            double total = entry.getValue() != null
                    ? entry.getValue().setScale(2, java.math.RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            serie.getData().add(new XYChart.Data<>(entry.getKey().toString(), total));
        }

        graficoFacturacion.getData().add(serie);
    }

    private void cargarGraficoMetodosPago(Map<String, Integer> datos) {
        graficoMetodosPago.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Cantidad de facturas");

        for (Map.Entry<String, Integer> entry : datos.entrySet()) {
            int cantidad = entry.getValue() != null ? entry.getValue() : 0;
            serie.getData().add(new XYChart.Data<>(entry.getKey(), cantidad));
        }

        graficoMetodosPago.getData().add(serie);
    }

    private void cargarTablaResumen(Map<LocalDate, BigDecimal> facturacion, Map<String, Integer> metodos) {
        tablaResumen.getItems().clear();
        for (Map.Entry<LocalDate, BigDecimal> entry : facturacion.entrySet()) {
            tablaResumen.getItems().add(new FacturaResumen(
                    entry.getKey(),
                    entry.getValue() != null ? entry.getValue() : BigDecimal.ZERO,
                    metodos
            ));
        }
    }

    @FXML
    private void exportarReporte() {
        if (tablaResumen.getItems().isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin datos", null, "No hay información para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            try {
                ExportadorReporte exportador = new ExportadorPDF();
                exportador.exportarClientes(tablaResumen.getItems(), archivo);
                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", null, "El reporte fue exportado correctamente.");
            } catch (Exception e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null, "Ocurrió un error al generar el archivo PDF.");
                e.printStackTrace();
            }
        }
    }

    // ✅ Ahora mostrarAlerta respeta tipo, título, header opcional y mensaje
    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String header, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(header);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
