package controllers;

import claseslogicas.Cliente;
import claseslogicas.HistorialView;
import dao.visitaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class HistorialClienteController implements Initializable, ConsultaClienteController.ClienteDependiente {

    private Cliente clienteActual;
    private final visitaDAO visitaDAO = new visitaDAO();
    private final ObservableList<HistorialView> historialData = FXCollections.observableArrayList();

    @FXML private Label lblNombreCliente;
    @FXML private TableView<HistorialView> tvHistorial;
    @FXML private TableColumn<HistorialView, LocalDateTime> colFecha;
    @FXML private TableColumn<HistorialView, String> colEstilista;
    @FXML private TableColumn<HistorialView, String> colServicio;
    @FXML private TableColumn<HistorialView, String> colObservaciones;
    @FXML private Button btnCerrar;
    @FXML private Button btnExportar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarTabla();
        btnExportar.setDisable(true);
    }

    @Override
    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        if (cliente != null) {
            lblNombreCliente.setText("Historial de: " + cliente.getNombreCompleto() + " (ID: " + cliente.getIdCliente() + ")");
            cargarHistorial(cliente.getIdCliente());
        }
    }

    private void configurarTabla() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fechaHora"));
        colEstilista.setCellValueFactory(new PropertyValueFactory<>("nombreEstilista"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("nombreServicio"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        colFecha.setCellFactory(column -> new TableCell<HistorialView, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                setText((empty || item == null) ? null : formatter.format(item));
            }
        });

        tvHistorial.setItems(historialData);
    }

    private void cargarHistorial(int idCliente) {
        historialData.clear();
        historialData.addAll(visitaDAO.obtenerHistorialPorCliente(idCliente));
        btnExportar.setDisable(historialData.isEmpty());
    }

    @FXML
    private void handleExportarHistorial() {
        if (historialData.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Exportación Fallida", "No hay datos para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Guardar Historial como CSV");

        String nombreCliente = clienteActual.getNombreCompleto().replace(" ", "_");
        fileChooser.setInitialFileName("historial_" + nombreCliente + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivos CSV (*.csv)", "*.csv"));

        File archivoElegido = fileChooser.showSaveDialog(btnExportar.getScene().getWindow());

        if (archivoElegido != null) {
            try (FileWriter writer = new FileWriter(archivoElegido)) {
                DateTimeFormatter csvFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                writer.append("Fecha;Estilista;Servicio;Observaciones\n");

                for (HistorialView item : historialData) {
                    String fechaStr = (item.getFechaHora() != null) ? item.getFechaHora().format(csvFormatter) : "N/A";
                    writer.append(fechaStr).append(";")
                            .append(item.getNombreEstilista()).append(";")
                            .append(item.getNombreServicio()).append(";")
                            .append(item.getObservaciones().replace('\n', ' ')).append("\n");
                }

                mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación Exitosa", "El historial ha sido guardado como CSV.");
                System.out.println("✅ Historial exportado a: " + archivoElegido.getAbsolutePath());

            } catch (IOException e) {
                mostrarAlerta(Alert.AlertType.ERROR, "Error de E/S", "No se pudo guardar el archivo.");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void handleCerrarVentana() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}