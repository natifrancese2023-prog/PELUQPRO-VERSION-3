package controllers;

import claseslogicas.*;
import dao.EmpleadoDAO;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.sql.SQLException;
import java.io.IOException;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableRow;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.beans.property.SimpleStringProperty;
import utilidades.AlertaUtil;
import service.TurnoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GestionDiariaController implements Initializable {

    private static final Logger log = LoggerFactory.getLogger(GestionDiariaController.class);

    private boolean esGerente;

    @FXML
    private DatePicker dpFecha;
    @FXML
    private ComboBox<Empleado> cbEstilistaFiltro;
    @FXML
    private TableView<Turno> tvTurnos;

    @FXML
    private TableColumn<Turno, LocalDate> colFecha;
    @FXML
    private TableColumn<Turno, String> colHora;
    @FXML
    private TableColumn<Turno, String> colCliente;
    @FXML
    private TableColumn<Turno, String> colEstilista;
    @FXML
    private TableColumn<Turno, String> colServicios;
    @FXML
    private TableColumn<Turno, String> colEstado;
    @FXML
    private TableColumn<Turno, String> colMotivoLog;

    @FXML
    private Button btnFinalizar;
    @FXML
    private Button btnCancelar;
    @FXML
    private Button btnConfirmar;

    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final TurnoService turnoService = new TurnoService();
    private ObservableList<Turno> listaTurnos = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        aplicarPermisos();
        configurarColumnas();
        tvTurnos.setItems(listaTurnos);
        cargarEstilistasFiltro();
        dpFecha.setValue(LocalDate.now());

        dpFecha.valueProperty().addListener((obs, oldDate, newDate) -> {
            if (newDate != null) cargarAgenda();
        });
        cbEstilistaFiltro.valueProperty().addListener((obs, oldEmp, newEmp) -> {
            cargarAgenda();
        });
        tvTurnos.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            actualizarEstadoBotones(newSel);
        });

        tvTurnos.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(Turno turno, boolean empty) {
                super.updateItem(turno, empty);
                if (turno == null || empty) {
                    setStyle("");
                } else {
                    String estado = turno.getEstadoTurno() != null ? turno.getEstadoTurno().getNombre() : "";
                    switch (estado) {
                        case "Pendiente" -> setStyle("-fx-background-color: #fffde7;");
                        case "Cancelado" -> setStyle("-fx-background-color: #ffebee;");
                        case "Finalizado" -> setStyle("-fx-background-color: #e8f5e9;");
                        case "Confirmado" -> setStyle("-fx-background-color: #e3f2fd;");
                        case "Facturado (Pago Recibido)" -> setStyle("-fx-background-color: #f3e5f5;");
                        default -> setStyle("");
                    }
                }
            }
        });

        cargarAgenda();
    }

    private void configurarColumnas() {
        colFecha.setCellValueFactory(new PropertyValueFactory<>("fecha"));
        colHora.setCellValueFactory(new PropertyValueFactory<>("horaInicio"));
        colCliente.setCellValueFactory(new PropertyValueFactory<>("nombreCliente"));
        colEstilista.setCellValueFactory(new PropertyValueFactory<>("nombreEstilista"));
        colServicios.setCellValueFactory(new PropertyValueFactory<>("resumenServicios"));
        colEstado.setCellValueFactory(cellData -> {
            EstadoTurno estado = cellData.getValue().getEstadoTurno();
            String nombre = (estado != null) ? estado.getNombre() : "Sin estado";
            return new SimpleStringProperty(nombre);
        });
        colMotivoLog.setCellValueFactory(new PropertyValueFactory<>("motivoLog"));
    }

    private void cargarEstilistasFiltro() {
        try {
            List<Empleado> estilistas = empleadoDAO.obtenerEstilistas();

            Empleado todos = new Empleado();
            todos.setIdPersona(0);
            todos.setNombre("Todos los Estilistas");
            todos.setApellido("");

            ObservableList<Empleado> items = FXCollections.observableArrayList();
            items.add(todos);
            items.addAll(estilistas);

            cbEstilistaFiltro.setItems(items);
            cbEstilistaFiltro.getSelectionModel().select(0);
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de BD", null, "No se pudieron cargar los estilistas.");
        }
    }

    @FXML
    private void cargarAgenda(ActionEvent event) {
        cargarAgenda();
    }

    private void cargarAgenda() {
        LocalDate fecha = dpFecha.getValue();
        Empleado estilista = cbEstilistaFiltro.getSelectionModel().getSelectedItem();
        Integer idEmpleado = (estilista != null && estilista.getIdPersona() != 0) ? estilista.getIdPersona() : null;

        if (fecha == null) {
            listaTurnos.clear();
            return;
        }

        try {
            List<Turno> turnos = turnoService.obtenerAgenda(fecha, idEmpleado);
            listaTurnos.clear();
            listaTurnos.addAll(turnos);
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de Conexión", null, "No se pudo cargar la agenda: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleFinalizarTurno(ActionEvent event) {
        cambiarEstadoTurno(EstadoTurno.FINALIZADO, "Finalizar Turno", "Cliente atendido");
    }

    @FXML
    private void handleCancelarTurno(ActionEvent event) {
        cambiarEstadoTurno(EstadoTurno.CANCELADO, "Cancelación manual", "Cancelación manual");
    }

    @FXML
    private void handleConfirmarTurno(ActionEvent event) {
        cambiarEstadoTurno(EstadoTurno.CONFIRMADO, "Confirmar Turno", "Turno confirmado por recepción");
    }
    private void cambiarEstadoTurno(EstadoTurno nuevoEstado, String accion, String motivo) {
        Turno turnoSeleccionado = tvTurnos.getSelectionModel().getSelectedItem();

        if (turnoSeleccionado == null) {
            AlertaUtil.mostrarAlerta(AlertType.WARNING, "Sin selección", null, "Debe seleccionar un turno.");
            return;
        }

        try {
            turnoService.cambiarEstado(turnoSeleccionado, nuevoEstado, motivo);

            // 🔄 FIX 2: Al cambiar con éxito, actualizamos el objeto en memoria para que la tabla cambie de color
            turnoSeleccionado.setEstadoTurno(nuevoEstado);
            turnoSeleccionado.setEstadoLogico(nuevoEstado);

            tvTurnos.refresh();

            // 🔄 FIX 3: Forzar a los botones a recalcularse con el nuevo estado del turno
            actualizarEstadoBotones(turnoSeleccionado);

            AlertaUtil.mostrarAlerta(AlertType.INFORMATION, "Éxito", null, "Turno actualizado a " + nuevoEstado.getNombre());
        } catch (IllegalStateException e) {
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Transición inválida", null, e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(AlertType.ERROR, "Error de BD", null, "No se pudo actualizar el estado del turno.");
            e.printStackTrace();
        }
    }
    /**
     * HU14 Filtrar turnos por estilista: Gerente únicamente.
     * HU17 Estado de turno (Confirmar/Finalizar/Facturar/Cancelar): Gerente únicamente.
     * Recepcionista solo puede "Consultar" la agenda (HU12), sin actuar sobre los turnos.
     */
    private void aplicarPermisos() {
        esGerente = utilidades.PermisosUtil.esGerente();
        cbEstilistaFiltro.setDisable(!esGerente);
    }
    private void actualizarEstadoBotones(Turno turno) {
        if (!esGerente) {
            btnFinalizar.setDisable(true);
            btnCancelar.setDisable(true);
            btnConfirmar.setDisable(true);
            return;
        }

        if (turno == null || turno.getEstadoTurno() == null) {
            btnFinalizar.setDisable(true);
            btnCancelar.setDisable(true);
            btnConfirmar.setDisable(true);
            return;
        }

        try {

            Turno turnoFresco = turnoService.obtenerPorId(turno.getIdTurno());
            if (turnoFresco == null) {
                turnoFresco = turno; // Fallback por seguridad
            }

            // Activación según lógica de transición clásica
            btnFinalizar.setDisable(!turnoFresco.puedeCambiarA(EstadoTurno.FINALIZADO));
            btnCancelar.setDisable(!turnoFresco.puedeCambiarA(EstadoTurno.CANCELADO));
            btnConfirmar.setDisable(!turnoFresco.puedeCambiarA(EstadoTurno.CONFIRMADO));

        } catch (SQLException e) {

            log.error("Error de BD al actualizar estado de botones para turno {}", turno.getIdTurno(), e);
        } catch (RuntimeException e) {

            log.error("Excepción inesperada al evaluar estado de botones para turno {}", turno.getIdTurno(), e);
        }
    }

}