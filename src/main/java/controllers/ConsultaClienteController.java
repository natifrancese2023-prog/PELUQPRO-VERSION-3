package controllers;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;
import dao.ClienteDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class ConsultaClienteController implements Initializable {

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private Cliente clienteActual = null;

    @FXML private ComboBox<String> cmbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
    @FXML private Button btnBuscar;

    // === FXML: Datos del cliente ===
    @FXML private VBox vboxDatosCliente;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtCalle;
    @FXML private TextField txtNumero;
    @FXML private ComboBox<String> cmbProvincia;
    @FXML private ComboBox<String> cmbCiudad;
    @FXML private ComboBox<String> cmbBarrio;
    @FXML private ComboBox<String> cmbTipoRedSocial;
    @FXML private TextField txtUsuarioRedSocial;

    // === FXML: Botones de acción ===
    @FXML private Button btnCerrar;
    @FXML private Button btnVerHistorial;
    @FXML private Button btnCargarVisita;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        try {
            cmbTipoDocumento.setItems(FXCollections.observableArrayList(clienteDAO.obtenerTiposDocumento()));
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Conexión", "No se pudieron cargar los tipos de documento.");
        }

        setCamposBloqueados(true);
        setBotonesAccionHabilitados(false);
    }

    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        rellenarCampos(clienteActual);
        setCamposBloqueados(false);
        setBotonesAccionHabilitados(true);


        if (cmbTipoDocumento != null) cmbTipoDocumento.setVisible(false);
        if (txtNumeroDocumento != null) txtNumeroDocumento.setVisible(false);
        if (btnBuscar != null) btnBuscar.setVisible(false);
    }


    @FXML
    private void handleBuscarCliente() {
        String tipoDoc = cmbTipoDocumento.getValue();
        String numDoc = txtNumeroDocumento.getText().trim();

        if (tipoDoc == null || tipoDoc.isEmpty() || numDoc.isEmpty()) {
            mostrarAlerta(Alert.AlertType.WARNING, "Búsqueda Incompleta", "Debe seleccionar un Tipo y Número de Documento.");
            return;
        }

        limpiarCampos();
        setBotonesAccionHabilitados(false);
        clienteActual = null;

        try {
            clienteActual = clienteDAO.consultarPorDocumentoCompleto(tipoDoc, numDoc);

            if (clienteActual != null) {
                mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Cliente encontrado.");
                rellenarCampos(clienteActual);
                setCamposBloqueados(false);
                setBotonesAccionHabilitados(true);
            } else {
                mostrarAlerta(Alert.AlertType.WARNING, "Sin Resultados", "No se encontró ningún cliente con ese documento.");
                setCamposBloqueados(true);
            }
        } catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Base de Datos", "Ocurrió un error al consultar la base de datos.");
            e.printStackTrace();
            setCamposBloqueados(true);
        }
    }

    @FXML
    private void handleVerHistorial() {
        if (clienteActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Cliente", "Primero debe buscar un cliente para ver su historial.");
            return;
        }

        abrirSubModulo("/interface/HistorialCliente.fxml", "Historial de Visitas");
    }

    @FXML
    private void handleCargarVisita() {
        if (clienteActual == null) {
            mostrarAlerta(Alert.AlertType.WARNING, "Sin Cliente", "Primero debe buscar un cliente para cargar una visita.");
            return;
        }

        abrirSubModulo("/interface/CargarVisita.fxml", "Cargar Nueva Visita");
    }

    private void abrirSubModulo(String rutaFXML, String tituloVentana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();

            Object controller = loader.getController();
            if (controller instanceof ClienteDependiente) {
                ((ClienteDependiente) controller).setCliente(clienteActual);
            }

            Stage stage = new Stage();
            stage.setTitle(tituloVentana);
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Carga", "No se pudo abrir el submódulo.");
            e.printStackTrace();
        }
    }

    private void rellenarCampos(Cliente c) {
        txtNombre.setText(c.getNombre());
        txtApellido.setText(c.getApellido());
        txtTelefono.setText(c.getTelefono());
        txtEmail.setText(c.getEmail());
        txtCalle.setText(c.getCalle());
        txtNumero.setText(c.getNumero());

        try {
            cmbProvincia.setItems(FXCollections.observableArrayList(clienteDAO.obtenerProvincias()));
            cmbProvincia.getSelectionModel().select(c.getNombreProvincia());

            cmbCiudad.setItems(FXCollections.observableArrayList(clienteDAO.obtenerCiudadesPorProvincia(c.getNombreProvincia())));
            cmbCiudad.getSelectionModel().select(c.getNombreCiudad());

            cmbBarrio.setItems(FXCollections.observableArrayList(clienteDAO.obtenerBarriosPorCiudad(c.getNombreCiudad())));
            cmbBarrio.getSelectionModel().select(c.getNombreBarrio());

            cmbTipoRedSocial.setItems(FXCollections.observableArrayList(clienteDAO.obtenerTiposRedSocial()));
        }
        catch (SQLException e) {
            mostrarAlerta(Alert.AlertType.ERROR, "Error de Carga", "No se pudieron cargar los datos de ubicación del cliente.");
            e.printStackTrace();
        }

        ClienteRedSocial rs = c.getRedSocial();
        if (rs != null && rs.getNombreUsuario() != null) {
            cmbTipoRedSocial.getSelectionModel().select(rs.getNombreTipoRedSocial());
            txtUsuarioRedSocial.setText(rs.getNombreUsuario());
        } else {
            limpiarCamposRedSocial();
        }
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtCalle.clear();
        txtNumero.clear();
        cmbProvincia.getSelectionModel().clearSelection();
        cmbCiudad.getSelectionModel().clearSelection();
        cmbBarrio.getSelectionModel().clearSelection();
        limpiarCamposRedSocial();
    }

    private void limpiarCamposRedSocial() {
        cmbTipoRedSocial.getSelectionModel().clearSelection();
        txtUsuarioRedSocial.clear();
    }

    private void setCamposBloqueados(boolean bloqueado) {
        vboxDatosCliente.setDisable(bloqueado);
    }

    private void setBotonesAccionHabilitados(boolean habilitado) {
        btnVerHistorial.setDisable(!habilitado);
        btnCargarVisita.setDisable(!habilitado);
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


    public interface ClienteDependiente {
        void setCliente(Cliente cliente);
    }
}