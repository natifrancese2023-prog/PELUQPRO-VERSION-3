package controllers;

import claseslogicas.*;
import dao.*;
import java.net.URL;
import java.sql.Connection;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.Alert.AlertType;
import utilidades.AlertaUtil;

public class AltaTurnoController implements Initializable {

    // --- Controles FXML Mapeados ---
    @FXML private ComboBox<String> cbTipoDocumento;
    @FXML private TextField txtDocumento;
    @FXML private Label lblNombreCliente;
    @FXML private ListView<Servicio> lvServicios;
    @FXML private Label lblDuracionTotal;
    @FXML private DatePicker dpFecha;
    @FXML private ComboBox<Empleado> cbEstilista;
    @FXML private Button btnBuscarDisponibilidad;
    @FXML private ListView<BloqueDisponible> lvTurnosDisponibles;
    @FXML private TextArea txtObservaciones;
    @FXML private Button btnAgendar;
    @FXML private Button btnBuscarCliente;

    // --- DAOs ---
    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final EmpleadoDAO empleadoDAO = new EmpleadoDAO();
    private final ServicioDAO servicioDAO = new ServicioDAO();
    private final TurnoDAO turnoDAO = new TurnoDAO();

    // --- Variables de Estado ---
    private Cliente clienteActual;
    private ObservableList<Servicio> serviciosSeleccionados = FXCollections.observableArrayList();
    private int duracionTotalMinutos = 0;
    private BloqueDisponible bloqueSeleccionado;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Configuraciones iniciales
        cargarEstilistas();
        cargarServicios();
        cargarTiposDocumento();

        // Deshabilitar Agendar por defecto y agregar listener al ListView de disponibles
        btnAgendar.setDisable(true);
        lvTurnosDisponibles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            bloqueSeleccionado = newVal;
            btnAgendar.setDisable(newVal == null);
        });

        // Listener para recalcular duración al cambiar la selección de servicios
        lvServicios.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        // 🎯 CORRECCIÓN: Usar ListChangeListener en la lista de ítems seleccionados
        lvServicios.getSelectionModel().getSelectedItems().addListener(
                (javafx.collections.ListChangeListener.Change<? extends Servicio> change) -> {
                    recalcularDuracion();
                });

        // Deshabilitar botón de búsqueda hasta que haya una duración mínima
        btnBuscarDisponibilidad.setDisable(true);

        // Listener para la fecha: disparar búsqueda de disponibilidad si ya hay servicios seleccionados
        dpFecha.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (duracionTotalMinutos > 0) {
                handleBuscarDisponibilidad(null);
            }
        });
    }

    // =========================================================================
    // I. IDENTIFICACIÓN DEL CLIENTE
    // =========================================================================

    private void cargarTiposDocumento() {
        try {
            // Asumo que tienes un método obtenerTiposDocumento() en ClienteDAO
            List<String> tipos = clienteDAO.obtenerTiposDocumento();
            cbTipoDocumento.setItems(FXCollections.observableArrayList(tipos));
            if (!tipos.isEmpty()) {
                cbTipoDocumento.getSelectionModel().select(0);
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error de BD",
                    null,
                    "No se pudieron cargar los tipos de documento: " + e.getMessage()
            );

        }
    }

    @FXML
    private void handleBuscarCliente(ActionEvent event) {
        String tipoDoc = cbTipoDocumento.getSelectionModel().getSelectedItem();
        String numDoc = txtDocumento.getText().trim();

        if (tipoDoc == null || numDoc.isEmpty()) {
            AlertaUtil.mostrarAlerta(
                    AlertType.WARNING,
                    "Advertencia",
                    null,
                    "Debe seleccionar un tipo y número de documento."
            );

            return;
        }

        try {
            clienteActual = clienteDAO.consultarPorDocumentoCompleto(tipoDoc, numDoc);

            if (clienteActual != null) {
                lblNombreCliente.setText("Cliente: " + clienteActual.getNombreCompleto());
                lblNombreCliente.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
            } else {
                lblNombreCliente.setText("Cliente no encontrado. Debe registrarlo primero.");
                lblNombreCliente.setStyle("-fx-text-fill: red;");
                clienteActual = null;
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error de BD",
                    null,
                    "Error al buscar cliente: " + e.getMessage()
            );

        }
    }

    // =========================================================================
    // II. SELECCIÓN DE SERVICIOS Y DURACIÓN
    // =========================================================================

    private void cargarServicios() {
        try {
            List<Servicio> todosServicios = servicioDAO.obtenerTodos();
            lvServicios.setItems(FXCollections.observableArrayList(todosServicios));

            lvServicios.setCellFactory(lv -> new ListCell<Servicio>() {
                @Override
                protected void updateItem(Servicio item, boolean empty) {
                    super.updateItem(item, empty);
                    // Asumo que Servicio tiene getNombreConDuracion()
                    setText(empty ? null : item.getNombreConDuracion());
                }
            });
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error de BD",
                    null,
                    "No se pudieron cargar los servicios: " + e.getMessage()
            );

        }
    }

    private void recalcularDuracion() {
        // Recalculamos la lista completa de seleccionados
        serviciosSeleccionados.setAll(lvServicios.getSelectionModel().getSelectedItems());

        // Recalculamos la duración total
        duracionTotalMinutos = serviciosSeleccionados.stream()
                .mapToInt(Servicio::getDuracionMinutos)
                .sum();

        lblDuracionTotal.setText("Duración Total Requerida: " + duracionTotalMinutos + " minutos.");

        btnBuscarDisponibilidad.setDisable(duracionTotalMinutos == 0);

        // Intentar buscar disponibilidad si ya hay una fecha
        if (dpFecha.getValue() != null && duracionTotalMinutos > 0) {
            handleBuscarDisponibilidad(null);
        }
    }

    // =========================================================================
    // III & IV. AGENDA Y BÚSQUEDA DE DISPONIBILIDAD
    // =========================================================================

    private void cargarEstilistas() {
        try (Connection conn = ConexionBD.getConnection()) {
            System.out.println("🔍 Conexión recibida en AltaTurnoController: " + conn);

            List<Empleado> estilistas = empleadoDAO.obtenerEstilistas();
            System.out.println("🔍 Estilistas encontrados: " + estilistas.size());

            if (estilistas.isEmpty()) {
                System.out.println("⚠️ No se encontraron estilistas en la base de datos.");
                AlertaUtil.mostrarAlerta(
                        Alert.AlertType.WARNING,
                        "Sin estilistas",
                        null,
                        "No hay estilistas registrados."
                );

            } else {
                cbEstilista.setItems(FXCollections.observableArrayList(estilistas));
                System.out.println("✅ ComboBox cargado con estilistas.");
            }

        } catch (SQLException e) {
            e.printStackTrace(); // ✅ muestra el error completo
            AlertaUtil.mostrarAlerta(
                    Alert.AlertType.ERROR,
                    "Error",
                    null,
                    "No se pudo cargar los estilistas: " + e.getMessage()
            );

        }
    }




    @FXML
    private void handleBuscarDisponibilidad(ActionEvent event) {
        if (clienteActual == null || duracionTotalMinutos <= 0 || dpFecha.getValue() == null) {
            lvTurnosDisponibles.getItems().clear();
            if (event != null) {
                AlertaUtil.mostrarAlerta(
                        AlertType.WARNING,
                        "Advertencia",
                        null,
                        "Debe completar Cliente, Servicios y Fecha."
                );

            }
            return;
        }

        LocalDate fecha = dpFecha.getValue();
        Empleado estilistaSeleccionado = cbEstilista.getValue();

        // ✅ Validación para evitar NullPointerException
        if (estilistaSeleccionado == null) {
            AlertaUtil.mostrarAlerta(
                    AlertType.WARNING,
                    "Estilista no seleccionado",
                    null,
                    "Debe seleccionar un estilista antes de buscar disponibilidad."
            );

            return;
        }

        Integer idEstilista = (estilistaSeleccionado.getIdEmpleado() == 0) ? null : estilistaSeleccionado.getIdEmpleado();


        try {
            System.out.println("DEBUG: Duración enviada: " + duracionTotalMinutos + " minutos. Fecha: " + fecha);
            List<BloqueDisponible> disponibles = turnoDAO.obtenerTurnosDisponibles(fecha, duracionTotalMinutos, idEstilista);

            lvTurnosDisponibles.setItems(FXCollections.observableArrayList(disponibles));

            lvTurnosDisponibles.setCellFactory(lv -> new ListCell<BloqueDisponible>() {
                @Override
                protected void updateItem(BloqueDisponible item, boolean empty) {
                    super.updateItem(item, empty);
                    setText(empty ? null : item.getResumenBloque());
                }
            });

            if (disponibles.isEmpty()) {
                AlertaUtil.mostrarAlerta(
                        AlertType.INFORMATION,
                        "Información",
                        null,
                        "No se encontraron turnos disponibles con esos criterios."
                );

            }

        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error de BD",
                    null,
                    "Error al buscar disponibilidad: " + e.getMessage()
            );

        }
    }


    // =========================================================================
    // V. AGENDAMIENTO Y PERSISTENCIA (Gestión de Estado Inicial)
    // =========================================================================


    @FXML
    private void handleAgendarTurno(ActionEvent event) {
        if (bloqueSeleccionado == null || clienteActual == null || serviciosSeleccionados.isEmpty())
        {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error",
                    null,
                    "Debe seleccionar un cliente, servicios y un turno disponible."
            );

            return;
        }

        // 1. Construir el objeto Turno
        Turno nuevoTurno = new Turno();
        nuevoTurno.setIdCliente(clienteActual.getIdCliente()); // ✅

        // Usamos getIdPersona() que es el id_empleado
        nuevoTurno.setIdEmpleado(bloqueSeleccionado.getEstilista().getIdEmpleado()); // ✅

        nuevoTurno.setFecha(dpFecha.getValue());
        nuevoTurno.setHoraInicio(bloqueSeleccionado.getHoraInicio());
        nuevoTurno.setHoraFin(bloqueSeleccionado.getHoraFin());
        nuevoTurno.setServicios(new ArrayList<>(serviciosSeleccionados));
        nuevoTurno.setObservaciones(txtObservaciones.getText());

        // Establece el estado inicial
        nuevoTurno.setEstadoLogico(EstadoTurno.PENDIENTE);

        try {
            // 2. Ejecutar la transacción de inserción
            boolean exito = turnoDAO.insertarTurno(nuevoTurno);

            if (exito) {
                AlertaUtil.mostrarAlerta(
                        AlertType.INFORMATION,
                        "Éxito",
                        null,
                        "Turno agendado exitosamente para " + clienteActual.getNombreCompleto() + "."
                );


                limpiarFormulario();
            }
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(
                    AlertType.ERROR,
                    "Error de Agenda",
                    null,
                    "Fallo al agendar el turno. Detalle: " + e.getMessage()
            );

        }
    }


    @FXML
    private void handleCancelar(ActionEvent event) {
        limpiarFormulario();
    }



    private void limpiarFormulario() {
        txtDocumento.clear();
        lblNombreCliente.setText("Cliente: N/A");
        lblNombreCliente.setStyle("-fx-text-fill: black;");
        lvServicios.getSelectionModel().clearSelection();
        recalcularDuracion();
        lvTurnosDisponibles.getItems().clear();
        txtObservaciones.clear();
        cbEstilista.getSelectionModel().select(0);
        dpFecha.setValue(null);
        clienteActual = null;
        bloqueSeleccionado = null;
        btnAgendar.setDisable(true);
    }

}