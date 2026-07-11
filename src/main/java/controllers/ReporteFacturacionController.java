package controllers;

import com.itextpdf.io.exceptions.IOException;
import dao.ReporteFacturacionDAO;
import javafx.beans.property.SimpleStringProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import claseslogicas.FacturaResumen;
import claseslogicas.ExportadorReporte;
import claseslogicas.ExportadorPDF;

import java.awt.event.ActionEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

import javafx.stage.Window;
import utilidades.AlertaUtil;

import javax.imageio.ImageIO;

public class ReporteFacturacionController {

    @FXML private DatePicker fechaInicio;
    @FXML private DatePicker fechaFin;
    @FXML private LineChart<String, Number> graficoFacturacion;
    @FXML private StackedBarChart<String, Number> graficoMetodosPago; // ✅ Cambiado a StackedBarChart
    @FXML private Button btnGenerar;
    @FXML private Button btnExportar;

    // Etiquetas del período
    @FXML private Label lblTotalPeriodo;
    @FXML private Label lblTotalFacturas;
    @FXML private Label lblTotalPagadas;

    @FXML private TableView<FacturaResumenModificada> tablaResumen; // Utiliza un modelo preparado para recibir los nuevos datos
    @FXML private TableColumn<FacturaResumenModificada, String> colFecha;
    @FXML private TableColumn<FacturaResumenModificada, String> colTotal;
    @FXML private TableColumn<FacturaResumenModificada, String> colMetodos;
    @FXML private TableColumn<FacturaResumenModificada, String> colCantidad;
    @FXML private TableColumn<FacturaResumenModificada, String> colPagadas;
    @FXML
    private PieChart graficoTortaMetodos;// ✅ Nueva columna

    private final ReporteFacturacionDAO dao = new ReporteFacturacionDAO();

    @FXML
    public void initialize() {
        graficoFacturacion.setTitle("Evolución diaria de facturación ($)");
        graficoMetodosPago.setTitle("Cantidad de Facturas por Día (Por Método)");
        graficoFacturacion.setAnimated(false);
        graficoMetodosPago.setAnimated(false);
        graficoFacturacion.setLegendVisible(false);
        graficoMetodosPago.setLegendVisible(true);
        graficoFacturacion.setCreateSymbols(true);

        graficoFacturacion.setVisible(false);
        graficoMetodosPago.setVisible(false);
        tablaResumen.setVisible(false);

        // Mapeo de columnas
        colFecha.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha().toString()));
        colTotal.setCellValueFactory(data -> {
            BigDecimal total = data.getValue().getTotalFacturado();
            return new SimpleStringProperty(NumberFormat.getCurrencyInstance().format(total));
        });
        colCantidad.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCantidadFacturas())));
        colPagadas.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getCantidadPagadas())));
        colMetodos.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getResumenMetodos()));
    }

    @FXML
    public void generarReporte() {
        LocalDate inicio = fechaInicio.getValue();
        LocalDate fin = fechaFin.getValue();

        if (inicio == null || fin == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Fechas inválidas", null, "Debés seleccionar ambas fechas.");
            return;
        }
        if (fin.isBefore(inicio)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Rango inválido", null, "La fecha fin no puede ser anterior a la fecha inicio.");
            return;
        }

        Map<LocalDate, BigDecimal> facturacion = dao.obtenerFacturacionPorDia(inicio, fin);
        Map<LocalDate, List<String>> metodosPorDia = dao.obtenerMetodosPorFacturaPorDia(inicio, fin);

        // 1. Cargar Gráfico de línea (Monto $ por día)
        cargarGraficoFacturacion(facturacion);

        // 2. Cargar Gráfico Apilado (Cantidades, Métodos y cálculo de Pagadas)
        cargarDatosYGrficoApilado(facturacion, metodosPorDia);

        graficoFacturacion.setVisible(true);
        graficoMetodosPago.setVisible(true);
        tablaResumen.setVisible(true);
    }

    private void cargarGraficoFacturacion(Map<LocalDate, BigDecimal> datos) {
        graficoFacturacion.getData().clear();
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        serie.setName("Total facturado ($)");

        datos.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    double total = entry.getValue() != null
                            ? entry.getValue().setScale(2, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;
                    serie.getData().add(new XYChart.Data<>(entry.getKey().toString(), total));
                });

        graficoFacturacion.getData().add(serie);
    }
    private void cargarDatosYGrficoApilado(Map<LocalDate, BigDecimal> facturacion, Map<LocalDate, List<String>> metodosPorDia) {
        graficoMetodosPago.getData().clear();
        tablaResumen.getItems().clear();
        if (graficoTortaMetodos != null) {
            graficoTortaMetodos.getData().clear();
        }

        // Estructura para el gráfico apilado (Por día): Método -> (Fecha -> Cantidad)
        Map<String, Map<String, Integer>> conteoPorMetodoYDia = new HashMap<>();

        // Estructura para el gráfico de torta (Total Período): Método -> Cantidad Total
        Map<String, Integer> acumuladoTorta = new HashMap<>();

        // Variables para los totales globales del PERÍODO
        final BigDecimal[] totalMontoPeriodo = {BigDecimal.ZERO};
        int totalFacturasPeriodo = 0;
        int totalPagadasPeriodo = 0;

        // Ordenamos las fechas cronológicamente
        List<LocalDate> fechasOrdenadas = new ArrayList<>(facturacion.keySet());
        Collections.sort(fechasOrdenadas);

        for (LocalDate fecha : fechasOrdenadas) {
            BigDecimal montoDia = facturacion.getOrDefault(fecha, BigDecimal.ZERO);
            totalMontoPeriodo[0] = totalMontoPeriodo[0].add(montoDia);

            List<String> metodosDelDia = metodosPorDia.getOrDefault(fecha, Collections.emptyList());
            int cantFacturasDia = metodosDelDia.size();
            int cantPagadasDia = 0;

            // Mapa local para contar los métodos específicos DE ESTE DÍA únicamente
            Map<String, Integer> conteoMetodosDia = new HashMap<>();

            // CORRECCIÓN: Si hay dinero pero la lista de métodos de la BD vino vacía
            if (cantFacturasDia == 0 && montoDia.compareTo(BigDecimal.ZERO) > 0) {
                cantFacturasDia = 1;
                cantPagadasDia = 1;
                String metodoDefecto = "Efectivo"; // Asignación segura por defecto si hay dinero registrado

                conteoPorMetodoYDia.computeIfAbsent(metodoDefecto, k -> new HashMap<>());
                conteoPorMetodoYDia.get(metodoDefecto).put(fecha.toString(), 1);
                conteoMetodosDia.put(metodoDefecto, 1);
                acumuladoTorta.put(metodoDefecto, acumuladoTorta.getOrDefault(metodoDefecto, 0) + 1);

            } else {
                // Clasificamos cada registro del día en las categorías solicitadas
                for (String registro : metodosDelDia) {
                    String metodoNormalizado = "Facturada"; // Por defecto si no está pagada
                    boolean esPagada = false;

                    if (registro != null && !registro.trim().isEmpty()) {
                        String regLower = registro.trim().toLowerCase();

                        if (regLower.contains("efectivo")) {
                            metodoNormalizado = "Efectivo";
                            esPagada = true;
                        } else if (regLower.contains("transferencia")) {
                            metodoNormalizado = "Transferencia";
                            esPagada = true;
                        } else if (regLower.contains("debito") || regLower.contains("débito") || regLower.contains("credito") || regLower.contains("crédito") || regLower.contains("tarjeta")) {
                            metodoNormalizado = "Débito"; // Agrupamos las operaciones con tarjeta/posnet
                            esPagada = true;
                        } else if (regLower.equals("anulada")) {
                            metodoNormalizado = "Anulada";
                        } else if (regLower.equals("facturada")) {
                            metodoNormalizado = "Facturada";
                        } else {
                            // En caso de que venga "Pagada" a secas, lo clasificamos como Efectivo o mantienes el string
                            metodoNormalizado = "Efectivo";
                            esPagada = true;
                        }
                    }

                    if (esPagada) {
                        cantPagadasDia++;
                    }

                    // 1. Agrupamos para el gráfico de barras apiladas (Evolución diaria)
                    conteoPorMetodoYDia.computeIfAbsent(metodoNormalizado, k -> new HashMap<>());
                    conteoPorMetodoYDia.get(metodoNormalizado).put(fecha.toString(),
                            conteoPorMetodoYDia.get(metodoNormalizado).getOrDefault(fecha.toString(), 0) + 1);

                    // 2. Agrupamos para la columna de la tabla (Conteo diario)
                    conteoMetodosDia.put(metodoNormalizado, conteoMetodosDia.getOrDefault(metodoNormalizado, 0) + 1);

                    // 3. Agrupamos para el gráfico de Torta del período (Solo métodos de pago reales, excluimos Anuladas/Facturadas si deseas)
                    if (esPagada) {
                        acumuladoTorta.put(metodoNormalizado, acumuladoTorta.getOrDefault(metodoNormalizado, 0) + 1);
                    }
                }
            }

            // Totales del período entero
            totalFacturasPeriodo += cantFacturasDia;
            totalPagadasPeriodo += cantPagadasDia;

            // Construimos la cadena exacta para la columna "Desglose de Métodos" (Ej: "Efectivo(2) Transferencia(1)")
            StringBuilder resumenMetodosTxt = new StringBuilder();
            conteoMetodosDia.forEach((metodo, cantidad) -> {
                resumenMetodosTxt.append(metodo).append(" (").append(cantidad).append(") ");
            });

            // Insertamos la fila en tu TableView
            tablaResumen.getItems().add(new FacturaResumenModificada(
                    fecha,
                    montoDia,
                    cantFacturasDia,
                    cantPagadasDia,
                    resumenMetodosTxt.toString().trim()
            ));
        }

        // Llenamos el gráfico StackedBarChart
        conteoPorMetodoYDia.forEach((metodo, mapaFechas) -> {
            XYChart.Series<String, Number> serie = new XYChart.Series<>();
            serie.setName(metodo);
            mapaFechas.forEach((fechaStr, cantidad) -> {
                serie.getData().add(new XYChart.Data<>(fechaStr, cantidad));
            });
            graficoMetodosPago.getData().add(serie);
        });

        // Llenamos el gráfico de Torta (PieChart) con el acumulado de Efectivo, Transferencia y Débito
        if (graficoTortaMetodos != null) {
            acumuladoTorta.forEach((metodo, totalCantidad) -> {
                graficoTortaMetodos.getData().add(new PieChart.Data(metodo + " (" + totalCantidad + ")", totalCantidad));
            });
        }

        // Actualizamos etiquetas superiores
        lblTotalPeriodo.setText("Total Facturado Período: " + NumberFormat.getCurrencyInstance().format(totalMontoPeriodo[0]));
        lblTotalFacturas.setText("Total Facturas: " + totalFacturasPeriodo);
        lblTotalPagadas.setText("Total Pagadas: " + totalPagadasPeriodo);
    }

    @FXML
    private void exportarReporte(ActionEvent event) {
        // 1. Buscamos la ventana actual para poder anclar el explorador de archivos
        Window ventana = btnExportar.getScene().getWindow();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Reporte como Imagen");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Imagen PNG (*.png)", "*.png")
        );
        fileChooser.setInitialFileName("Reporte_Facturacion_" + LocalDate.now() + ".png");

        // 2. Abrimos el diálogo para que el usuario elija la ruta
        File archivoDestino = fileChooser.showSaveDialog(ventana);

        if (archivoDestino != null) {
            try {
                // 3. CAPTURA: Obtenemos el contenedor que está dentro del ScrollPane en el FXML.
                // Si tu VBox contenedor de gráficos no tiene un fx:id, podés usar el nodo padre del gráfico.
                // En este caso, tomamos el contenedor directo del gráfico de facturación.
                VBox contenedorGraficos = (VBox) graficoFacturacion.getParent();

                // Desactivamos temporalmente el scroll para que la captura salga completa y nítida
                WritableImage snapshot = contenedorGraficos.snapshot(new SnapshotParameters(), null);

                // 4. GUARDADO: Escribimos los píxeles en el archivo físico PNG
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(snapshot, null);
                ImageIO.write(bufferedImage, "png", archivoDestino);

                // Alerta de éxito para avisarle al usuario
                Alert alerta = new Alert(Alert.AlertType.INFORMATION);
                alerta.setTitle("Exportación Exitosa");
                alerta.setHeaderText(null);
                alerta.setContentText("El reporte gráfico se ha guardado correctamente en:\n" + archivoDestino.getAbsolutePath());
                alerta.showAndWait();

            } catch (IOException e) {
                e.printStackTrace();
                Alert alerta = new Alert(Alert.AlertType.ERROR);
                alerta.setTitle("Error al exportar");
                alerta.setHeaderText("No se pudo guardar la imagen");
                alerta.setContentText("Ocurrió un error interno: " + e.getMessage());
                alerta.showAndWait();
            } catch (java.io.IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // ✅ Clase interna auxiliar para no romper tu "FacturaResumen" original si se usa en otra parte
    public static class FacturaResumenModificada {
        private final LocalDate fecha;
        private final BigDecimal totalFacturado;
        private final int cantidadFacturas;
        private final int cantidadPagadas;
        private final String resumenMetodos;

        public FacturaResumenModificada(LocalDate fecha, BigDecimal totalFacturado, int cantidadFacturas, int cantidadPagadas, String resumenMetodos) {
            this.fecha = fecha;
            this.totalFacturado = totalFacturado;
            this.cantidadFacturas = cantidadFacturas;
            this.cantidadPagadas = cantidadPagadas;
            this.resumenMetodos = resumenMetodos;
        }

        public LocalDate getFecha() { return fecha; }
        public BigDecimal getTotalFacturado() { return totalFacturado; }
        public int getCantidadFacturas() { return cantidadFacturas; }
        public int getCantidadPagadas() { return cantidadPagadas; }
        public String getResumenMetodos() { return resumenMetodos; }
    }
}