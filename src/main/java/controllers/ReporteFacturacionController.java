package controllers;

import claseslogicas.ExportadorReportes;
import dao.ReporteFacturacionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import claseslogicas.FacturaResumen;


import java.io.File;
import java.time.LocalDate;
import java.util.List;
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
    private List<clasesreportes.ClienteReporteExtendido> datosClientes;


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
        colTotal.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getTotalFacturado())));
        colMetodos.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResumenMetodosPago()));


        System.out.println("Inicialización completa del panel de reporte.");
    }

    @FXML
    public void generarReporte() {
        LocalDate inicio = fechaInicio.getValue();
        LocalDate fin = fechaFin.getValue();

        System.out.println("Fecha inicio seleccionada: " + inicio);
        System.out.println("Fecha fin seleccionada: " + fin);

        if (inicio == null || fin == null || fin.isBefore(inicio)) {
            mostrarAlerta(Alert.AlertType.INFORMATION, "Fechas inválidas", "Seleccioná un período válido.");
            return;
        }

        Map<LocalDate, Double> facturacion = dao.obtenerFacturacionPorDia(inicio, fin);
        Map<String, Integer> metodos = dao.obtenerUsoMetodosPago(inicio, fin);

        System.out.println("Facturación por día recibida: " + facturacion.size() + " registros.");
        System.out.println("Métodos de pago recibidos: " + metodos.size() + " registros.");

        cargarGraficoFacturacion(facturacion);
        cargarGraficoMetodosPago(metodos);
        cargarTablaResumen(facturacion, metodos);

        graficoFacturacion.setVisible(true);
        graficoMetodosPago.setVisible(true);
        tablaResumen.setVisible(true);
    }

    private void cargarGraficoFacturacion(Map<LocalDate, Double> datos) {
        graficoFacturacion.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Total facturado");

        for (Map.Entry<LocalDate, Double> entry : datos.entrySet()) {
            System.out.println("Agregando al gráfico de facturación: " + entry.getKey() + " → $" + entry.getValue());
            serie.getData().add(new XYChart.Data<>(entry.getKey().toString(), entry.getValue()));
        }

        graficoFacturacion.getData().add(serie);
    }

    private void cargarGraficoMetodosPago(Map<String, Integer> datos) {
        graficoMetodosPago.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Cantidad de facturas");

        for (Map.Entry<String, Integer> entry : datos.entrySet()) {
            System.out.println("Agregando al gráfico de métodos: " + entry.getKey() + " → " + entry.getValue() + " usos");
            serie.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }

        graficoMetodosPago.getData().add(serie);
    }

    private void cargarTablaResumen(Map<LocalDate, Double> facturacion, Map<String, Integer> metodos) {
        tablaResumen.getItems().clear();
        for (Map.Entry<LocalDate, Double> entry : facturacion.entrySet()) {
            System.out.println("Agregando a tabla: " + entry.getKey() + " → $" + entry.getValue());
            tablaResumen.getItems().add(new FacturaResumen(entry.getKey(), entry.getValue(), metodos));
        }
    }
    @FXML
    private void exportarReporte() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar reporte PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo PDF", "*.pdf"));
        File archivo = fileChooser.showSaveDialog(null);
        if (archivo != null) {
            WritableImage imgFacturacion = graficoFacturacion.snapshot(null, null);
            WritableImage imgMetodos = graficoMetodosPago.snapshot(null, null);

            ExportadorReportes.exportarFacturacionPDF(
                    tablaResumen.getItems(),
                    archivo,
                    fechaInicio.getValue(),
                    fechaFin.getValue(),
                    imgFacturacion,
                    imgMetodos
            );

            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", "El reporte fue exportado correctamente.");
        }
    }


    private void mostrarAlerta(Alert.AlertType information, String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.INFORMATION);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}

