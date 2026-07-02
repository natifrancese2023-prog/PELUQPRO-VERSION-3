package controllers;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;

import dao.ClienteDAO;

import javafx.collections.FXCollections; // Necesario para setAll/getItems
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import java.net.URL;

import java.sql.SQLException; // Importación necesaria
import java.util.List;
import java.util.ResourceBundle;
import utilidades.AlertaUtil;






public class AltaClienteController implements Initializable {

    // ======================================
    // DAO
    // ======================================
    private final ClienteDAO clienteDAO = new ClienteDAO();

    // ======================================
    // FXML Componentes (Se mantienen igual)
    // ======================================
    @FXML private ComboBox<String> cmbTipoDocumento;
    @FXML private TextField txtNumeroDocumento;
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
    @FXML private Button btnAltaCliente;
    @FXML private Button btnCancelar;


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // CORREGIDO: Carga inicial envuelta en try-catch
        cargarComboboxesIniciales();

        // Configurar Listeners para carga dinámica de Ciudad y Barrio
        configurarListenersComboBox();
    }

    /**
     * ✅ CORREGIDO: Nuevo método para encapsular la carga inicial con manejo de excepciones.
     */
    private void cargarComboboxesIniciales() {
        try {
            var documentos = clienteDAO.obtenerTiposDocumento();
            System.out.println("📄 Documentos cargados: " + documentos.size());
            cmbTipoDocumento.setItems(FXCollections.observableArrayList(documentos));
        } catch (SQLException e) {
            System.err.println("❌ Error al cargar documentos: " + e.getMessage());
        }

        try {
            var provincias = clienteDAO.obtenerProvincias();
            System.out.println("🌎 Provincias cargadas: " + provincias.size());
            cmbProvincia.setItems(FXCollections.observableArrayList(provincias));
        } catch (SQLException e) {
            System.err.println("❌ Error al cargar provincias: " + e.getMessage());
        }

        try {
            var redes = clienteDAO.obtenerTiposRedSocial();
            System.out.println("🔗 Redes cargadas: " + redes.size());
            cmbTipoRedSocial.setItems(FXCollections.observableArrayList(redes));
        } catch (SQLException e) {
            System.err.println("❌ Error al cargar redes sociales: " + e.getMessage());
        }
    }


    /**
     * ✅ CORREGIDO: Las llamadas al DAO dentro de los Listeners deben manejar la SQLException.
     */
    private void configurarListenersComboBox() {
        // Listener para cargar CIUDADES cuando se selecciona una PROVINCIA
        cmbProvincia.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                cmbCiudad.getItems().clear();
                cmbBarrio.getItems().clear();

                try {
                    List<String> ciudades = clienteDAO.obtenerCiudadesPorProvincia(newVal);
                    cmbCiudad.setItems(FXCollections.observableArrayList(ciudades));
                } catch (SQLException e) {
                    System.err.println("❌ Error de BD al cargar ciudades: " + e.getMessage());
                    AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de Conexión", "Fallo al Cargar Ciudades", "No se pudieron cargar las ciudades.");
                }
            }
        });

        // Listener para cargar BARRIOS cuando se selecciona una CIUDAD
        cmbCiudad.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.equals(oldVal)) {
                cmbBarrio.getItems().clear();

                try {
                    List<String> barrios = clienteDAO.obtenerBarriosPorCiudad(newVal);
                    cmbBarrio.setItems(FXCollections.observableArrayList(barrios));
                } catch (SQLException e) {
                    System.err.println("❌ Error de BD al cargar barrios: " + e.getMessage());
                    AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de Conexión", "Fallo al Cargar Barrios", "No se pudieron cargar los barrios.");
                }
            }
        });
    }

    @FXML
    private void handleGuardarCliente(ActionEvent event) {
        if (!validarCampos()) {
            return;
        }

        Cliente nuevoCliente = new Cliente();

        nuevoCliente.setNombre(txtNombre.getText().trim());
        nuevoCliente.setApellido(txtApellido.getText().trim());
        nuevoCliente.setTelefono(txtTelefono.getText().trim());
        nuevoCliente.setEmail(txtEmail.getText().trim());
        nuevoCliente.setNombreTipoDocumento(cmbTipoDocumento.getValue());
        nuevoCliente.setNumeroDocumento(txtNumeroDocumento.getText().trim());
        nuevoCliente.setCalle(txtCalle.getText().trim());
        nuevoCliente.setNumero(txtNumero.getText().trim());
        nuevoCliente.setNombreProvincia(cmbProvincia.getValue());
        nuevoCliente.setNombreCiudad(cmbCiudad.getValue());
        nuevoCliente.setNombreBarrio(cmbBarrio.getValue());

        String usuarioRed = txtUsuarioRedSocial.getText().trim();
        String tipoRed = cmbTipoRedSocial.getValue();

        if (!usuarioRed.isEmpty() && tipoRed != null) {
            ClienteRedSocial rs = new ClienteRedSocial();
            rs.setNombreUsuario(usuarioRed);
            rs.setNombreTipoRedSocial(tipoRed);
            nuevoCliente.setRedSocial(rs);
        } else {
            nuevoCliente.setRedSocial(null);
        }

        try {
            boolean insertado = clienteDAO.insertar(nuevoCliente);

            if (insertado) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", "Alta Exitosa",
                        "El nuevo cliente ha sido registrado en la base de datos.");
                handleCancelar(); // Limpiar campos
            } else {
                // Si retorna false, es un fallo lógico detectado por el DAO (ej: ID no encontrado)
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Fallo de Lógica", "Error de Inserción",
                        "No se pudo registrar el cliente. Posiblemente faltan IDs de FKs (Tipo Documento, Barrio).");
            }

        } catch (SQLException e) {
            // Manejamos la SQLException específica delegada por el DAO
            System.err.println("❌ ERROR DE TRANSACCIÓN BD: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Fallo de Base de Datos", "Error de Conexión o Duplicidad",
                    "Ocurrió un error al intentar registrar el cliente. Verifique la conexión o si el documento ya existe.");
        }
    }


    @FXML
    private void handleCancelar() {
        limpiarCampos();
    }
    // Regex de validación
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
    private static final String PHONE_REGEX = "^[0-9]{7,15}$";       // Teléfono: solo dígitos, 7-15 caracteres
    private static final String DOCUMENT_REGEX = "^[0-9]{6,12}$";    // Documento: solo dígitos, 6-12 caracteres

    private boolean validarCampos() {
        // Validación de campos obligatorios
        if (txtNombre.getText().trim().isEmpty() ||
                txtApellido.getText().trim().isEmpty() ||
                txtNumeroDocumento.getText().trim().isEmpty() ||
                cmbTipoDocumento.getValue() == null ||
                cmbProvincia.getValue() == null || cmbCiudad.getValue() == null || cmbBarrio.getValue() == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Validación Incompleta", "Campos Obligatorios Vacíos",
                    "Por favor, complete Nombre, Apellido, Documento y la Dirección completa.");
            return false;
        }

        // Validación de email
        if (!txtEmail.getText().trim().matches(EMAIL_REGEX)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Validación Email", "Formato inválido",
                    "Ingrese un email válido (ejemplo: usuario@dominio.com).");
            return false;
        }

        // Validación de teléfono
        if (!txtTelefono.getText().trim().matches(PHONE_REGEX)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Validación Teléfono", "Formato inválido",
                    "El teléfono debe contener solo dígitos y tener entre 7 y 15 caracteres.");
            return false;
        }

        // Validación de documento
        if (!txtNumeroDocumento.getText().trim().matches(DOCUMENT_REGEX)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Validación Documento", "Formato inválido",
                    "El documento debe contener solo dígitos y tener entre 6 y 12 caracteres.");
            return false;
        }

        // Validación de red social
        boolean usuarioVacio = txtUsuarioRedSocial.getText().trim().isEmpty();
        boolean tipoVacio = cmbTipoRedSocial.getValue() == null;
        if ((!usuarioVacio && tipoVacio) || (usuarioVacio && !tipoVacio)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Validación Red Social", "Información Incompleta",
                    "Debe seleccionar el Tipo de Red Social Y escribir el Usuario, o dejar ambos campos vacíos.");
            return false;
        }

        return true;
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
        txtTelefono.clear();
        txtEmail.clear();
        txtNumeroDocumento.clear();
        txtCalle.clear();
        txtNumero.clear();
        txtUsuarioRedSocial.clear();

        cmbTipoDocumento.getSelectionModel().clearSelection();
        cmbProvincia.getSelectionModel().clearSelection();
        cmbCiudad.getItems().clear();
        cmbBarrio.getItems().clear();
        cmbTipoRedSocial.getSelectionModel().clearSelection();
    }



}