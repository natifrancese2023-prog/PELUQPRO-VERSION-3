package controllers;

import claseslogicas.*;
import dao.TurnoDAO;
import dao.EmpleadoDAO;
import dao.visitaDAO;
import dao.FacturaDAO;

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

public class GestionDiariaController implements Initializable {

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
    @FXML
    private Button btnFacturar;

    private final TurnoDAO turnoDAO = new TurnoDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final visitaDAO visitaDAO = new visitaDAO();
    private ObservableList<Turno> listaTurnos = FXCollections.observableArrayList();
    private final FacturaDAO facturaDAO = new FacturaDAO();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
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
            mostrarAlerta(AlertType.ERROR, "Error de BD", "No se pudieron cargar los estilistas.");
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
            List<Turno> turnos = turnoDAO.obtenerTurnosFiltrados(fecha, idEmpleado);
            listaTurnos.clear();
            listaTurnos.addAll(turnos);
        } catch (SQLException e) {
            mostrarAlerta(AlertType.ERROR, "Error de Conexión", "No se pudo cargar la agenda: " + e.getMessage());
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
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Debe seleccionar un turno.");
            return;
        }

        if (!turnoSeleccionado.puedeCambiarA(nuevoEstado)) {
            mostrarAlerta(AlertType.ERROR, "Transición inválida", "No se puede cambiar de " + turnoSeleccionado.getEstadoTurno().getNombre() + " a " + nuevoEstado.getNombre());
            return;
        }

        try {
            turnoDAO.actualizarEstado(turnoSeleccionado.getIdTurno(), nuevoEstado, motivo);

            turnoSeleccionado.setEstadoTurno(nuevoEstado);
            turnoSeleccionado.setMotivoLog(motivo);
            tvTurnos.refresh();
            mostrarAlerta(AlertType.INFORMATION, "Éxito", "Turno actualizado a " + nuevoEstado.getNombre());
        } catch (SQLException e) {
            mostrarAlerta(AlertType.ERROR, "Error de BD", "No se pudo actualizar el estado del turno.");
            e.printStackTrace();
        }
    }
    private void actualizarEstadoBotones(Turno turno) {
        if (turno == null || turno.getEstadoTurno() == null) {
            btnFinalizar.setDisable(true);
            btnCancelar.setDisable(true);
            btnConfirmar.setDisable(true);
            btnFacturar.setDisable(true);
            return;
        }

        // Activación según lógica de transición
        btnFinalizar.setDisable(!turno.puedeCambiarA(EstadoTurno.FINALIZADO));
        btnCancelar.setDisable(!turno.puedeCambiarA(EstadoTurno.CANCELADO));
        btnConfirmar.setDisable(!turno.puedeCambiarA(EstadoTurno.CONFIRMADO));

        // Activación del botón Facturar solo si está FINALIZADO, tiene visita y NO tiene factura
        if (turno.getEstadoTurno() == EstadoTurno.FINALIZADO) {
            System.out.println("🔍 Turno en estado FINALIZADO: " + turno.getIdTurno());

            Visita visita = visitaDAO.obtenerVisitaPorTurno(turno.getIdTurno());
            boolean tieneVisita = visita != null;
            System.out.println("🔍 ¿Visita encontrada?: " + tieneVisita);

            boolean sinFactura = true;
            try {
                Factura facturaExistente = facturaDAO.obtenerPorTurno(turno.getIdTurno());
                sinFactura = (facturaExistente == null);
                System.out.println("🔍 ¿Factura ya existe?: " + (facturaExistente != null));
            } catch (SQLException e) {
                System.err.println("🧨 Error al verificar factura existente: " + e.getMessage());
            }

            btnFacturar.setDisable(!(tieneVisita && sinFactura));
            System.out.println("🔍 ¿Botón Facturar activado?: " + !btnFacturar.isDisable());
        } else {
            System.out.println("🔍 Turno NO está finalizado. Estado actual: " + turno.getEstadoTurno());
            btnFacturar.setDisable(true);
        }
    }


    @FXML
    private void handleFacturarTurno() throws SQLException {
        Turno turnoSeleccionado = tvTurnos.getSelectionModel().getSelectedItem();

        if (turnoSeleccionado == null) {
            mostrarAlerta(AlertType.WARNING, "Sin selección", "Debe seleccionar un turno para facturar.");
            return;
        }

        if (turnoSeleccionado.getEstadoTurno() != EstadoTurno.FINALIZADO) {
            mostrarAlerta(AlertType.ERROR, "Estado inválido", "Solo se puede facturar un turno que esté Finalizado.");
            return;
        }

        Visita visita = visitaDAO.obtenerVisitaPorTurno(turnoSeleccionado.getIdTurno());

        if (visita != null) {
            abrirPantallaFacturacion(visita.getIdVisita());
        } else {
            mostrarAlerta(AlertType.ERROR, "Sin visita", "Este turno no tiene una visita registrada.");
        }
    }

    private void abrirPantallaFacturacion(int idVisita) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/Factura.fxml"));
            Parent root = loader.load();

            // Accede al controlador de la vista Factura
            FacturaController controller = loader.getController();
            controller.cargarFacturaDesdeVisita(idVisita); // Método que vos definís en FacturaController

            Stage stage = new Stage();
            stage.setTitle("Facturación");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL); // Bloquea la ventana anterior si querés
            stage.show();

        } catch (IOException e) {
            mostrarAlerta(AlertType.ERROR, "Error al abrir pantalla", "No se pudo cargar la pantalla de facturación.");
            e.printStackTrace();
        }
    }


    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

}