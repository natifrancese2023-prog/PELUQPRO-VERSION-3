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
import utilidades.AlertaUtil;

import java.io.IOException;
import java.util.function.Consumer;

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
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Debe ingresar un número de documento.");
            return;
        }

        try {
            clienteActual = clienteDAO.consultarPorDocumentoCompleto(tipoDoc, nroDoc);

            if (clienteActual != null) {
                txtNombre.setText(clienteActual.getNombreCompleto());
                txtTelefono.setText(clienteActual.getTelefono());
                txtFechaAlta.setText(clienteActual.getFechaAlta() != null ? clienteActual.getFechaAlta().toString() : "Sin fecha");
            } else {
                AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Cliente no encontrado.");
                limpiarCampos();
            }
        } catch (Exception e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "Error al consultar el cliente. Verifique los datos.");
            e.printStackTrace();
        }
    }

    private <T> void abrirVentana(String fxml, String titulo, Consumer<T> configurarControlador) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(fxml));
        Stage stage = new Stage();
        stage.setScene(new Scene(loader.load()));
        T controller = loader.getController();
        configurarControlador.accept(controller);
        stage.setTitle(titulo);
        stage.show();
    }

    @FXML
    private void modificarCliente() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Debe buscar un cliente primero.");
            return;
        }

        try {
            abrirVentana("/interface/ModificarCliente.fxml", "Modificar Cliente",
                    (ModificarClienteController controller) -> controller.setCliente(clienteActual));
        } catch (Exception e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir la ventana de modificación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void eliminarCliente() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Debe buscar un cliente primero.");
            return;
        }

        try {
            abrirVentana("/interface/EliminarCliente.fxml", "Eliminar Cliente",
                    (EliminarClienteController controller) -> controller.setCliente(clienteActual));
        } catch (Exception e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir la ventana de eliminación.");
            e.printStackTrace();
        }
    }

    @FXML
    private void verHistorial() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Debe buscar un cliente primero.");
            return;
        }

        try {
            abrirVentana("/interface/HistorialCliente.fxml", "Historial del Cliente",
                    (HistorialClienteController controller) -> controller.setCliente(clienteActual));
        } catch (Exception e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir el historial del cliente.");
            e.printStackTrace();
        }
    }

    @FXML
    private void registrarVisita() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Aviso", null, "Debe buscar un cliente primero.");
            return;
        }

        try {
            abrirVentana("/interface/CargarVisita.fxml", "Registrar Visita",
                    (CargarVisitaController controller) -> controller.setCliente(clienteActual));
        } catch (Exception e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir el registro de visita.");
            e.printStackTrace();
        }
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtTelefono.clear();
        txtFechaAlta.clear();
    }
}
