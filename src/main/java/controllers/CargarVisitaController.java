package controllers;

import claseslogicas.*;
import dao.EmpleadoDAO;
import service.TurnoService;
import service.VisitaService;
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

    private final VisitaService visitaService = new VisitaService();
    private final TurnoService turnoService = new TurnoService();
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

        cmbTipoServicio.getItems().addAll(visitaService.obtenerNombresServicios());

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
            List<Turno> turnos = turnoService.obtenerTurnosPorCliente(idCliente);


            List<Turno> turnosConfirmados = turnos.stream()
                    .filter(t -> t.getEstadoTurno() == EstadoTurno.CONFIRMADO)
                    .toList();

            if (turnosConfirmados.isEmpty()) {
                cmbTurnoCliente.setItems(FXCollections.observableArrayList());
                AlertaUtil.mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin turnos confirmados",
                        null,
                        "Este cliente no tiene turnos en estado 'Confirmado'. " +
                                "Debe confirmar un turno antes de poder registrar la visita."
                );
                return;
            }

            cmbTurnoCliente.setItems(FXCollections.observableArrayList(turnosConfirmados));
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
        String nombreServicio = cmbTipoServicio.getSelectionModel().getSelectedItem(); // 👈 esto devuelve un String

        ServicioTemp nuevoServicio = new ServicioTemp(
                LocalDate.now(),
                nombreServicio,
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

        int idEstilista = estilistaLogueado.getIdEmpleadoFk();

        // registrarVisita ya marca el turno como FINALIZADO dentro de su
        // propia transacción (junto con el guardado de la visita) — no hace
        // falta un cambiarEstado() aparte acá; antes había uno redundante
        // que hacía un segundo UPDATE innecesario sobre el mismo turno.
        boolean exito = visitaService.registrarVisita(clienteActual, listaServicios, idEstilista, idTurno);

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