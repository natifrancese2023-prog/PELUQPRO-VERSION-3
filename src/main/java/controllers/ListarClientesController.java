package controllers;

import claseslogicas.Cliente;
import dao.ClienteDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.Node;
import javafx.event.ActionEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.scene.control.Button;
import javafx.scene.control.TableCell;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class ListarClientesController implements Initializable {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    @FXML private TableView<Cliente> tblClientes;
    @FXML private TableColumn<Cliente, String> colNombre;
    @FXML private TableColumn<Cliente, String> colApellido;
    @FXML private TableColumn<Cliente, String> colDocumento;
    @FXML private TableColumn<Cliente, String> colFechaAlta;
    @FXML private TableColumn<Cliente, Integer> colNumeroVisitas;
    @FXML private TableColumn<Cliente, Void> colAccion; // ✅ columna para el botón "Ver"

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        configurarColumnas();
        cargarDatosClientes();
    }

    private void configurarColumnas() {
        // ✅ columnas normales
        colNombre.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getPersona() != null ? cellData.getValue().getPersona().getNombre() : "-"
                )
        );

        colApellido.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getPersona() != null ? cellData.getValue().getPersona().getApellido() : "-"
                )
        );

        colDocumento.setCellValueFactory(cellData ->
                new SimpleStringProperty(
                        cellData.getValue().getPersona() != null ? cellData.getValue().getPersona().getNumeroDocumento() : "-"
                )
        );

        colFechaAlta.setCellValueFactory(new PropertyValueFactory<>("fechaAltaString"));

        // 🔎 columna de visitas calculada dinámicamente por id_cliente
        colNumeroVisitas.setCellValueFactory(cellData -> {
            Cliente cliente = cellData.getValue();
            int cantidadVisitas = clienteDAO.contarVisitasPorIdCliente(cliente.getIdCliente());
            return new SimpleIntegerProperty(cantidadVisitas).asObject();
        });

        // ✅ columna de acción con botón "Ver"
        colAccion.setCellFactory(col -> new TableCell<>() {
            private final Button btnVer = new Button("Ver");

            {
                btnVer.setOnAction(event -> {
                    Cliente cliente = getTableView().getItems().get(getIndex());
                    if (cliente.getPersona() != null) {
                        String documento = cliente.getPersona().getNumeroDocumento();
                        abrirHistorialPorDocumento(cliente, documento);
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnVer);
                }
            }
        });
    }

    public void cargarDatosClientes() {
        try {
            ObservableList<Cliente> listaClientes = FXCollections.observableArrayList(clienteDAO.obtenerTodos());
            tblClientes.setItems(listaClientes);
        } catch (Exception e) {
            mostrarAlerta(AlertType.ERROR, "Error", "Carga de clientes",
                    "No se pudieron cargar los clientes: " + e.getMessage(), null);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleRefrescarTabla(ActionEvent event) {
        cargarDatosClientes();
        System.out.println("Tabla de clientes refrescada.");
    }

    @FXML
    private void handleCerrar(ActionEvent event) {
        try {
            Node source = (Node) event.getSource();
            source.getScene().getWindow().hide();
            System.out.println("✅ Submódulo Listar Clientes cerrado.");
        } catch (Exception e) {
            System.err.println("❌ Error al intentar cerrar el submódulo: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirHistorialPorDocumento(Cliente cliente, String documento) {
        try {
            System.out.println("📖 Abriendo historial del cliente con documento: " + documento);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/historialCliente.fxml"));
            Parent root = loader.load();

            // ✅ obtenemos el controller del historial
            HistorialClienteController controller = loader.getController();
            controller.setCliente(cliente); // le pasamos el cliente completo

            Stage stage = new Stage();
            stage.setTitle("Historial del Cliente");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            mostrarAlerta(AlertType.ERROR, "Error", "Historial",
                    "No se pudo abrir el historial del cliente con documento " + documento, null);
        }
    }

    private void mostrarAlerta(AlertType tipo, String titulo, String encabezado, String contenido, String header) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(encabezado);
        alerta.setContentText(contenido);
        alerta.showAndWait();
    }
}
