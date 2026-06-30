package controllers;

import claseslogicas.Cliente;
import controllers.EliminarClienteController;
import controllers.HistorialClienteController;
import controllers.ModificarClienteController;
import controllers.CargarVisitaController;
import dao.ClienteDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

public class FichaClienteController {

    @FXML private ComboBox<String> cmbTipoDocumento;
    @FXML private TextField txtDocumento;
    @FXML private TextField txtNombre;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtFechaAlta;

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private Cliente clienteActual;

    @FXML
    public void initialize() {
        cmbTipoDocumento.getItems().addAll("DNI", "CUIT", "Pasaporte");
        cmbTipoDocumento.getSelectionModel().selectFirst();
    }

    @FXML
    private void buscarCliente() {
        String tipoDoc = cmbTipoDocumento.getValue();
        String nroDoc = txtDocumento.getText().trim();

        if (nroDoc.isEmpty()) {
            mostrarAlerta("Debe ingresar un número de documento.");
            return;
        }

        try {
            clienteActual = clienteDAO.consultarPorDocumentoCompleto(tipoDoc, nroDoc);

            if (clienteActual != null) {
                txtNombre.setText(clienteActual.getNombreCompleto());
                txtTelefono.setText(clienteActual.getTelefono());
                txtFechaAlta.setText(clienteActual.getFechaAlta() != null ? clienteActual.getFechaAlta().toString() : "Sin fecha");
            } else {
                mostrarAlerta("Cliente no encontrado.");
                limpiarCampos();
            }
        } catch (Exception e) {
            mostrarAlerta("Error al consultar el cliente. Verifique los datos.");
            e.printStackTrace();
        }
    }

    @FXML
    private void modificarCliente() {
        if (clienteActual == null) {
            mostrarAlerta("Debe buscar un cliente primero.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/ModificarCliente.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            ModificarClienteController controller = loader.getController();
            controller.setCliente(clienteActual);
            stage.setTitle("Modificar Cliente");
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("No se pudo abrir la ventana de modificación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarCliente() {
        if (clienteActual == null) {
            mostrarAlerta("Debe buscar un cliente primero.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/EliminarCliente.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            EliminarClienteController controller = loader.getController();
            controller.setCliente(clienteActual);
            stage.setTitle("Eliminar Cliente");
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("No se pudo abrir la ventana de eliminación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void verHistorial() {
        if (clienteActual == null) {
            mostrarAlerta("Debe buscar un cliente primero.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/HistorialCliente.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            HistorialClienteController controller = loader.getController();
            controller.setCliente(clienteActual);
            stage.setTitle("Historial del Cliente");
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("No se pudo abrir el historial del cliente.");
            e.printStackTrace();
        }
    }

    @FXML
    private void registrarVisita() {
        if (clienteActual == null) {
            mostrarAlerta("Debe buscar un cliente primero.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/CargarVisita.fxml"));
            Stage stage = new Stage();
            stage.setScene(new Scene(loader.load()));
            CargarVisitaController controller = loader.getController();
            controller.setCliente(clienteActual);
            stage.setTitle("Registrar Visita");
            stage.show();
        } catch (Exception e) {
            mostrarAlerta("No se pudo abrir el registro de visita.");
            e.printStackTrace();
        }
    }

    private void mostrarAlerta(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Información");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtTelefono.clear();
        txtFechaAlta.clear();
    }
}