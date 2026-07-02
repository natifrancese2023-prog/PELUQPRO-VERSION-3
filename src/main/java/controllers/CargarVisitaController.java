package controllers;

import claseslogicas.*;
import dao.EmpleadoDAO;
import dao.TurnoDAO;
import dao.visitaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import utilidades.SesionManager;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import utilidades.AlertaUtil;

import static utilidades.AlertaUtil.mostrarAlerta;

public class CargarVisitaController implements Initializable, ConsultaClienteController.ClienteDependiente {

    private final visitaDAO visitaDAO = new visitaDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();

    private Cliente clienteActual;
    private Usuario estilistaLogueado;
    private String nombreEstilistaTabla;
    private int idTurno;

    private final ObservableList<ServicioTemp> listaServicios = FXCollections.observableArrayList();

    @FXML private TextField txtDocumentoCliente;
    @FXML private TextField txtNombreCliente;
    @FXML private ComboBox<String> cmbTipoServicio;
    @FXML private TextField txtObservacionesServicio;
    @FXML private ComboBox<Turno> cmbTurnoCliente;

    @FXML private TableView<ServicioTemp> tblServicios;
    @FXML private TableColumn<ServicioTemp, LocalDate> colFecha;
    @FXML private TableColumn<ServicioTemp, String> colServicio;
    @FXML private TableColumn<ServicioTemp, String> colEstilista;
    @FXML private TableColumn<ServicioTemp, String> colObservaciones;
    @FXML private Button btnGuardarVisita;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        estilistaLogueado = SesionManager.getInstance().getUsuarioLogueado();

        if (estilistaLogueado == null) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de Sesión",
                    null,
                    "No hay un estilista logueado. Cierre e ingrese de nuevo."
            );

            btnGuardarVisita.setDisable(true);
            return;
        }

        nombreEstilistaTabla = estilistaLogueado.getUsuario();

        cmbTipoServicio.getItems().addAll(visitaDAO.obtenerNombresServicios());

        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colServicio.setCellValueFactory(new PropertyValueFactory<>("servicio"));
        colEstilista.setCellValueFactory(new PropertyValueFactory<>("estilista"));
        colObservaciones.setCellValueFactory(new PropertyValueFactory<>("observaciones"));

        tblServicios.setItems(listaServicios);
    }

    @Override
    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        if (cliente != null) {
            txtDocumentoCliente.setText(cliente.getNombreTipoDocumento() + ": " + cliente.getNumeroDocumento());
            txtNombreCliente.setText(cliente.getNombreCompleto());
            cargarTurnosDelCliente(cliente.getIdCliente());
        }
    }

    private void cargarTurnosDelCliente(int idCliente) {
        try {
            List<Turno> turnos = turnoDAO.obtenerTurnosPorCliente(idCliente);
            cmbTurnoCliente.setItems(FXCollections.observableArrayList(turnos));
            cmbTurnoCliente.getSelectionModel().selectFirst();
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de BD",
                    null,
                    "No se pudieron cargar los turnos del cliente."
            );


            e.printStackTrace();
        }
    }

    @FXML
    private void handleAgregarServicio() {
        String servicio = cmbTipoServicio.getValue();
        String observaciones = txtObservacionesServicio.getText();

        if (servicio == null || servicio.isEmpty()) {
            mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Servicio Faltante",
                    null,
                    "Debe seleccionar un tipo de servicio."
            );

            return;
        }

        ServicioTemp nuevoServicio = new ServicioTemp(
                LocalDate.now(),
                servicio,
                nombreEstilistaTabla,
                observaciones,
                "Pendiente"
        );

        listaServicios.add(nuevoServicio);
        cmbTipoServicio.getSelectionModel().clearSelection();
        txtObservacionesServicio.clear();
    }

    @FXML
    private void handleGuardarVisita() {
        if (clienteActual == null || estilistaLogueado == null) {
            AlertaUtil.mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error de Datos",
                    null,
                    "Falta el cliente o el estilista logueado."
            );

            return;
        }

        if (listaServicios.isEmpty()) {
            AlertaUtil.mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Sin Servicios",
                    null,
                    "Debe agregar al menos un servicio a la visita."
            );


            return;
        }

        Turno turnoSeleccionado = cmbTurnoCliente.getValue();
        if (turnoSeleccionado == null) {
            AlertaUtil.mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Turno no seleccionado",
                    null,
                    "Debe seleccionar un turno para registrar la visita."
            );


            return;
        }

        if (turnoSeleccionado.getEstadoTurno() != EstadoTurno.CONFIRMADO) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Estado inválido", null, "El turno debe estar en estado 'Confirmado' para registrar la visita.");
            return;
        }

        idTurno = turnoSeleccionado.getIdTurno();

        try {
            turnoDAO.actualizarEstado(idTurno, EstadoTurno.FINALIZADO, "Visita registrada");
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error al finalizar turno", null,"No se pudo actualizar el estado del turno.");
            e.printStackTrace();
            return;
        }

        int idEstilista = estilistaLogueado.getIdEmpleadoFk();

        boolean exito = visitaDAO.guardarNuevaVisita(clienteActual, listaServicios, idEstilista, idTurno);

        if (exito) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", null,"Visita y servicios guardados correctamente.");
            cerrarVentana();
        } else {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"Ocurrió un error al guardar la visita.");
        }
    }

    @FXML
    private void handleVolver() {
        cerrarVentana();
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnGuardarVisita.getScene().getWindow();
        stage.close();
    }

}
