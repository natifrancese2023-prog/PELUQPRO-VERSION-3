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
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.event.ActionEvent;
import utilidades.AlertaUtil;

import javax.imageio.ImageIO;
import javafx.embed.swing.SwingFXUtils;
import com.itextpdf.text.Document;
import com.itextpdf.text.Image;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
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
    @FXML private TableColumn<ClienteReporteExtendido, java.math.BigDecimal> colGasto;
    @FXML private TableColumn<ClienteReporteExtendido, String> colEstadoTurno;

    @FXML private ComboBox<String> cbFiltro;


    @FXML private BarChart<String, Number> graficoBarras;
    @FXML private PieChart graficoTorta;
    @FXML private BarChart<String, Number> graficoHistograma;

    private final ReporteDAO reporteDAO = new ReporteDAO();
    private List<ClienteReporteExtendido> datosClientes;

    public void initialize() {
        configurarTitulosGraficos();
        configurarTabla();
        configurarFiltro();
        cargarDatosClientes();
    }

    private void configurarTitulosGraficos() {
        // 1. Establecer los títulos
        graficoBarras.setTitle("Gasto Total por Cliente");
        graficoTorta.setTitle("Distribución de Ingresos por Cliente");
        graficoHistograma.setTitle("Frecuencia de Visitas de Clientes");


        graficoBarras.setLegendVisible(false);
        graficoHistograma.setLegendVisible(false);

        String estiloTextoGraficos =
                "-fx-text-background-color: #000000; "
                        + "-fx-tick-label-fill: #000000; "
                        + "-fx-mark-highlight-inner: #000000; "
                        + "-fx-legend-text-fill: #000000;";


        graficoBarras.setStyle(estiloTextoGraficos);
        graficoTorta.setStyle(estiloTextoGraficos);
        graficoHistograma.setStyle(estiloTextoGraficos);


        graficoBarras.getXAxis().setStyle("-fx-tick-label-fill: #000000; -fx-label-padding: 5;");
        graficoBarras.getYAxis().setStyle("-fx-tick-label-fill: #000000;");
        graficoHistograma.getXAxis().setStyle("-fx-tick-label-fill: #000000; -fx-label-padding: 5;");
        graficoHistograma.getYAxis().setStyle("-fx-tick-label-fill: #000000;");
    }
    private void configurarTabla() {
        colNombre.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("nombreCompleto"));
        colTelefono.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("telefono"));
        colEmail.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("email"));
        colVisitas.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("cantidadVisitas"));
        colGasto.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("gastoTotal")); // 👈 Mapea directo el BigDecimal
        colEstadoTurno.setCellValueFactory(new javafx.scene.control.cell.PropertyValueFactory<>("estadoUltimoTurno"));

        // ✨ Agrega esto para que el número de la billetera no se vea plano y tenga formato de dinero
        colGasto.setCellFactory(tc -> new TableCell<>() {
            @Override
            protected void updateItem(java.math.BigDecimal item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    // Formato de moneda: $1,500.00
                    setText(String.format("$%.2f", item.doubleValue()));
                }
            }
        });
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
        actualizarGraficoBarras(filtrados);
        actualizarGraficoTorta(filtrados);
        actualizarGraficoHistograma(filtrados);
    }

    // ✅ Gráfico de barras
    private void actualizarGraficoBarras(List<ClienteReporteExtendido> clientes) {
        graficoBarras.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Gasto total por cliente");
        for (ClienteReporteExtendido c : clientes) {
            double gasto = c.getGastoTotal() != null ? c.getGastoTotal().doubleValue() : 0.0;
            serie.getData().add(new XYChart.Data<>(c.getNombreCompleto(), gasto));
        }
        graficoBarras.getData().add(serie);
    }

    // ✅ Gráfico de torta
    private void actualizarGraficoTorta(List<ClienteReporteExtendido> clientes) {
        graficoTorta.getData().clear();
        for (ClienteReporteExtendido c : clientes) {
            double gasto = c.getGastoTotal() != null ? c.getGastoTotal().doubleValue() : 0.0;
            graficoTorta.getData().add(new PieChart.Data(c.getNombreCompleto(), gasto));
        }
    }

    // ✅ Histograma (visitas)
    private void actualizarGraficoHistograma(List<ClienteReporteExtendido> clientes) {
        graficoHistograma.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Distribución de visitas");
        for (ClienteReporteExtendido c : clientes) {
            serie.getData().add(new XYChart.Data<>(c.getNombreCompleto(), c.getCantidadVisitas()));
        }
        graficoHistograma.getData().add(serie);
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
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(archivo));
                document.open();

                // Tabla de clientes
                PdfPTable table = new PdfPTable(3);
                table.addCell("Nombre");
                table.addCell("Visitas");
                table.addCell("Gasto acumulado");
                for (ClienteReporteExtendido c : tablaClientes.getItems()) {
                    table.addCell(c.getNombreCompleto());
                    table.addCell(String.valueOf(c.getCantidadVisitas()));
                    table.addCell(c.getGastoTotal().toString());
                }
                document.add(table);

                // Capturar y agregar gráficos
                agregarGraficoPDF(document, graficoBarras, "barras.png");
                agregarGraficoPDF(document, graficoTorta, "torta.png");
                agregarGraficoPDF(document, graficoHistograma, "histograma.png");

                document.close();
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

        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo CSV de Excel (*.csv)", "*.csv"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {

            try (java.io.PrintWriter pw = new java.io.PrintWriter(new FileOutputStream(archivo))) {


                pw.write('\ufeff');


                pw.println("Nombre completo;Teléfono;Email;Cantidad Visitas;Gasto Total;Estado Último Turno");


                for (ClienteReporteExtendido c : tablaClientes.getItems()) {
                    pw.println(String.format("%s;%s;%s;%d;%s;%s",
                            c.getNombreCompleto(),
                            c.getTelefono() != null ? c.getTelefono() : "",
                            c.getEmail() != null ? c.getEmail() : "",
                            c.getCantidadVisitas(),
                            c.getGastoTotal() != null ? c.getGastoTotal().toString() : "0.0",
                            c.getEstadoUltimoTurno() != null ? c.getEstadoUltimoTurno() : ""
                    ));
                }

                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación exitosa", null, "El reporte se exportó a Excel correctamente.");
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null, "Ocurrió un error al generar el archivo de Excel.");
                e.printStackTrace();
            }
        }
    }

    private void agregarGraficoPDF(Document document, javafx.scene.Node grafico, String nombreArchivo) throws Exception {
        WritableImage snapshot = grafico.snapshot(new SnapshotParameters(), null);
        File file = new File(nombreArchivo);
        ImageIO.write(SwingFXUtils.fromFXImage(snapshot, null), "png", file);
        Image img = Image.getInstance(nombreArchivo);
        document.add(img);
    }

}
