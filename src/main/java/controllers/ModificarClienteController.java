package controllers;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;
import dao.ClienteDAO;
import service.ClienteService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.stage.Stage;
import utilidades.AlertaUtil;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class ModificarClienteController implements Initializable {

    @FXML private TextField txtNombre, txtApellido, txtTelefono, txtEmail, txtCalle, txtNumero, txtUsuarioRedSocial;
    @FXML private ComboBox<String> cmbProvincia, cmbCiudad, cmbBarrio, cmbTipoRedSocial;

    // ClienteDAO se mantiene para las consultas de combos (lecturas puras).
    private final ClienteDAO clienteDAO = new ClienteDAO();
    // ClienteService centraliza la validación de formato y la actualización.
    private final ClienteService clienteService = new ClienteService();
    private Cliente clienteActual = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        cargarComboboxesIniciales();

        cmbProvincia.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) cargarCiudades(newV);
        });
        cmbCiudad.getSelectionModel().selectedItemProperty().addListener((obs, oldV, newV) -> {
            if (newV != null) cargarBarrios(newV);
        });
    }

    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        cargarComboboxesIniciales();
        cargarFormulario(clienteActual);
    }

    @FXML
    private void handleGuardarCambios() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(AlertType.WARNING, "Error de Modificación", "No hay cliente seleccionado", "Primero debe seleccionar un cliente válido.");
            return;
        }

        if (!validarCampos()) {
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Validación Fallida", "Campos Requeridos Incompletos", "Por favor, complete todos los campos obligatorios antes de guardar.");
            return;
        }

        String errorEmail = clienteService.validarEmail(txtEmail.getText().trim());
        if (errorEmail != null) {
            AlertaUtil.mostrarAlerta(AlertType.WARNING, "Validación Email", "Formato inválido", errorEmail);
            return;
        }

        String errorTelefono = clienteService.validarTelefono(txtTelefono.getText().trim());
        if (errorTelefono != null) {
            AlertaUtil.mostrarAlerta(AlertType.WARNING, "Validación Teléfono", "Formato inválido", errorTelefono);
            return;
        }

        clienteActual.setNombre(txtNombre.getText().trim());
        clienteActual.setApellido(txtApellido.getText().trim());
        clienteActual.setTelefono(txtTelefono.getText().trim());
        clienteActual.setEmail(txtEmail.getText().trim());
        clienteActual.setCalle(txtCalle.getText().trim());
        clienteActual.setNumero(txtNumero.getText().trim());
        clienteActual.setNombreProvincia(cmbProvincia.getValue());
        clienteActual.setNombreCiudad(cmbCiudad.getValue());
        clienteActual.setNombreBarrio(cmbBarrio.getValue());

        String tipoRS = cmbTipoRedSocial.getValue();
        String usuarioRS = txtUsuarioRedSocial.getText().trim();

        if (tipoRS != null && !usuarioRS.isEmpty()) {
            ClienteRedSocial rs = new ClienteRedSocial();
            rs.setNombreTipoRedSocial(tipoRS);
            rs.setNombreUsuario(usuarioRS);
            clienteActual.setRedSocial(rs);
        } else {
            clienteActual.setRedSocial(null);
        }

        try {
            boolean exito = clienteService.actualizarCliente(clienteActual);

            if (exito) {
                AlertaUtil.mostrarAlerta(AlertType.INFORMATION, "Modificación Exitosa", "Cliente Actualizado", "Los datos del cliente han sido guardados correctamente.");
                limpiarFormularioEdicion();
                clienteActual = null;
                Stage stage = (Stage) txtNombre.getScene().getWindow();
                stage.close();
            } else {
                AlertaUtil.mostrarAlerta(AlertType.ERROR, "Fallo Lógico", "Error al Modificar", "No se pudo actualizar el cliente. Revise los datos e inténtelo de nuevo.");
            }
        } catch (SQLException e) {
            System.err.println("❌ Error de BD al actualizar cliente: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de Base de Datos", "Fallo al Guardar", "Hubo un error al intentar modificar el cliente. Verifique la conexión.");
        }
    }

    @FXML
    private void handleCancelar() {
        limpiarFormularioEdicion();
        clienteActual = null;
    }

    private void cargarComboboxesIniciales() {
        try {
            cmbProvincia.setItems(FXCollections.observableArrayList(clienteDAO.obtenerProvincias()));
            cmbTipoRedSocial.setItems(FXCollections.observableArrayList(clienteDAO.obtenerTiposRedSocial()));
        } catch (SQLException e) {
            System.err.println("❌ Error de BD al cargar combos iniciales: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de Conexión", "Error de Base de Datos", "No se pudieron cargar los datos iniciales de la aplicación. Verifique la conexión.");
        }
    }

    private void cargarCiudades(String provincia) {
        cmbCiudad.getItems().clear();
        cmbBarrio.getItems().clear();
        try {
            List<String> ciudades = clienteDAO.obtenerCiudadesPorProvincia(provincia);
            cmbCiudad.setItems(FXCollections.observableArrayList(ciudades));
        } catch (SQLException e) {
            System.err.println("❌ Error de BD al cargar ciudades: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de Conexión", "Fallo al Cargar Ciudades", "No se pudieron cargar las ciudades para la provincia seleccionada.");
        }
    }

    private void cargarBarrios(String ciudad) {
        cmbBarrio.getItems().clear();
        try {
            List<String> barrios = clienteDAO.obtenerBarriosPorCiudad(ciudad);
            cmbBarrio.getItems().addAll(barrios);
        } catch (SQLException e) {
            System.err.println("❌ Error de BD al cargar barrios: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de Conexión", "Fallo al Cargar Barrios", "No se pudieron cargar los barrios para la ciudad seleccionada.");
        }
    }

    private boolean validarCampos() {
        return !txtNombre.getText().trim().isEmpty() &&
                !txtApellido.getText().trim().isEmpty() &&
                cmbProvincia.getValue() != null &&
                cmbCiudad.getValue() != null &&
                cmbBarrio.getValue() != null;
    }

    private void limpiarFormularioEdicion() {
        txtNombre.clear();
        txtApellido.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtCalle.clear();
        txtNumero.clear();
        txtUsuarioRedSocial.clear();

        cmbProvincia.getSelectionModel().clearSelection();
        cmbCiudad.getItems().clear();
        cmbBarrio.getItems().clear();
        cmbTipoRedSocial.getSelectionModel().clearSelection();
    }

    private void cargarFormulario(Cliente cliente) {
        txtNombre.setText(cliente.getNombre());
        txtApellido.setText(cliente.getApellido());
        txtTelefono.setText(cliente.getTelefono());
        txtEmail.setText(cliente.getEmail());
        txtCalle.setText(cliente.getCalle());
        txtNumero.setText(cliente.getNumero());

        cmbProvincia.getSelectionModel().select(cliente.getNombreProvincia());
        cargarCiudades(cliente.getNombreProvincia());
        cmbCiudad.getSelectionModel().select(cliente.getNombreCiudad());
        cargarBarrios(cliente.getNombreCiudad());
        cmbBarrio.getSelectionModel().select(cliente.getNombreBarrio());

        ClienteRedSocial rs = cliente.getRedSocial();
        if (rs != null) {
            cmbTipoRedSocial.getSelectionModel().select(rs.getNombreTipoRedSocial());
            txtUsuarioRedSocial.setText(rs.getNombreUsuario());
        } else {
            cmbTipoRedSocial.getSelectionModel().clearSelection();
            txtUsuarioRedSocial.clear();
        }
    }
}