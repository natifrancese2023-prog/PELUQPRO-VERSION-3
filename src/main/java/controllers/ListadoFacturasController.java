package controllers;

import claseslogicas.*;
import service.EstadoFacturaInvalidoException;
import service.FacturaService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.FileChooser;
import utilidades.AlertaUtil;

// FIX (flujo de estado): el import original era java.awt.event.ActionEvent,
// que no es compatible con los métodos @FXML de JavaFX. Esto hacía que el
// FXMLLoader no pudiera resolver correctamente los handlers de los botones
// "Marcar como pagada" / "Cancelar factura" (fallaba al cargar el FXML o
// simplemente no se disparaban los eventos).
import javafx.event.ActionEvent;

import java.io.File;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ListadoFacturasController {

    private final FacturaService facturaService = new FacturaService();

    @FXML private DatePicker dpDesde;
    @FXML private DatePicker dpHasta;
    @FXML private TextField txtFiltroCliente;
    @FXML private ComboBox<String> cmbFiltroMetodo;
    @FXML private Button btnBuscar;
    @FXML private Button btnVerPDF;

    @FXML private TableView<Factura> tblFacturas;
    @FXML private TableColumn<Factura, Integer> colId;
    @FXML private TableColumn<Factura, String> colCliente;
    @FXML private TableColumn<Factura, String> colFecha;
    @FXML private TableColumn<Factura, String> colMetodo;
    @FXML private TableColumn<Factura, Double> colTotal;
    @FXML private TableColumn<Factura, String> colEstado;
    @FXML
    private ComboBox<EstadoFactura> cmbFiltroEstado;

    private ObservableList<Factura> todasLasFacturas = FXCollections.observableArrayList();
    private FilteredList<Factura> facturasFiltradas;

    @FXML
    public void initialize() {
        colId.setCellValueFactory(new PropertyValueFactory<>("idFactura"));
        colFecha.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getFechaHora().toLocalDate().toString()));
        colCliente.setCellValueFactory(data -> {
            Cliente c = data.getValue().getCliente();
            if (c == null) return new javafx.beans.property.SimpleStringProperty("");
            return new javafx.beans.property.SimpleStringProperty(
                    c.getNombre() + " " + c.getApellido());
        });
        colMetodo.setCellValueFactory(new PropertyValueFactory<>("metodoPago"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("montoTotal"));
        colEstado.setCellValueFactory(data ->
                new javafx.beans.property.SimpleStringProperty(
                        data.getValue().getEstadoFacturaNombre()));

        dpDesde.setValue(LocalDate.now().minusMonths(1));
        dpHasta.setValue(LocalDate.now());

        cmbFiltroMetodo.getItems().addAll("Todos", "Efectivo", "Transferencia", "Débito/Crédito");
        cmbFiltroMetodo.getSelectionModel().selectFirst();

        btnVerPDF.setDisable(true);
        tblFacturas.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, nueva) -> btnVerPDF.setDisable(nueva == null));

        cmbFiltroEstado.getItems().add(null); // "Todos" los estados
        cmbFiltroEstado.getItems().addAll(EstadoFactura.values());
    }

    @FXML
    private void handleBuscar() {
        LocalDate desde = dpDesde.getValue();
        LocalDate hasta = dpHasta.getValue();

        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Rango inválido", null, "Seleccioná un rango de fechas válido.");
            return;
        }

        List<Factura> resultado = facturaService.obtenerPorRango(desde, hasta);

        todasLasFacturas.setAll(resultado);
        facturasFiltradas = new FilteredList<>(todasLasFacturas, f -> true);
        tblFacturas.setItems(facturasFiltradas);
        aplicarFiltrosSecundarios();
    }

    @FXML
    private void handleAplicarFiltros() {
        if (facturasFiltradas == null) return;
        aplicarFiltrosSecundarios();
    }

    /**
     * FIX: el FXML (ListadoFacturas.fxml, línea 47) tiene
     * onAction="#handleFiltrarPorEstado" cableado a cmbFiltroEstado (u otro
     * control). Ese método existía en el controller original pero yo lo había
     * quitado al unificar los tres filtros en aplicarFiltrosSecundarios(),
     * lo que rompía el FXMLLoader con LoadException al no encontrar el
     * handler. Se reincorpora el nombre del método, pero como alias de la
     * lógica unificada -no se vuelve a la implementación vieja que ignoraba
     * el rango de fechas y pisaba el FilteredList-.
     */
    @FXML
    private void handleFiltrarPorEstado(ActionEvent event) {
        handleAplicarFiltros();
    }

    /**
     * FIX (flujo de estado): el filtro por EstadoFactura (cmbFiltroEstado)
     * antes tenía su propio handler (handleFiltrarPorEstado) que ignoraba el
     * rango de fechas ya buscado y reemplazaba directamente los items de la
     * tabla, pisando el FilteredList usado por los filtros de cliente/método.
     * Ahora el filtro de estado se combina acá con los otros dos, sobre el
     * mismo FilteredList, y cmbFiltroEstado dispara este mismo método
     * (ver handleAplicarFiltros, cableado también al ComboBox en el FXML).
     */
    private void aplicarFiltrosSecundarios() {
        String cliente = txtFiltroCliente.getText().toLowerCase().trim();
        String metodo = cmbFiltroMetodo.getValue();
        EstadoFactura estado = cmbFiltroEstado.getValue();

        facturasFiltradas.setPredicate(f -> {
            boolean coincideCliente = cliente.isEmpty() ||
                    (f.getCliente() != null &&
                            f.getCliente().getNombreCompleto().toLowerCase().contains(cliente));

            boolean coincideMetodo = metodo == null || metodo.equals("Todos") ||
                    f.getMetodoPago().equalsIgnoreCase(metodo);

            boolean coincideEstado = estado == null || f.getEstadoFactura() == estado;

            return coincideCliente && coincideMetodo && coincideEstado;
        });
    }

    @FXML
    private void handleVerPDF() {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) return;

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar factura PDF");
        chooser.setInitialFileName("Factura_" + seleccionada.getIdFactura() + ".pdf");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));

        File destino = chooser.showSaveDialog(tblFacturas.getScene().getWindow());
        if (destino != null) {
            try {
                ExportadorPDF.exportarFacturaIndividualPDF(seleccionada, destino);
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "PDF generado", null,
                        "Factura guardada en: " + destino.getAbsolutePath());
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null,
                        "No se pudo generar el PDF de la factura.");
                e.printStackTrace();
            }
        }
    }

    /**
     * FIX (flujo de estado): antes existían dos pares de métodos duplicados
     * (onMarcarPagada/onCancelarFactura y handleMarcarPagada/handleCancelarFactura)
     * con la misma lógica implementada dos veces de forma distinta. Se unifica
     * todo en un solo método privado (cambiarEstadoSeleccionada) que ambos
     * pares de handlers invocan, para que el FXML siga funcionando sin
     * importar cuál de los dos nombres tenga cableado cada botón, sin
     * duplicar lógica que pueda divergir con el tiempo.
     *
     * También se agrega el guard de selección nula que faltaba: antes
     * obtenerIdSeleccionado() podía devolver null y el unboxing a int
     * (int idFactura = obtenerIdSeleccionado()) tiraba NullPointerException.
     */
    @FXML
    private void onMarcarPagada(ActionEvent event) {
        handleMarcarPagada(event);
    }

    @FXML
    private void onCancelarFactura(ActionEvent event) {
        handleCancelarFactura(event);
    }

    @FXML
    private void handleMarcarPagada(ActionEvent event) {
        cambiarEstadoSeleccionada(
                idFactura -> facturaService.marcarComoPagada(idFactura),
                "Factura marcada como Pagada",
                "Error al marcar factura como pagada"
        );
    }

    @FXML
    private void handleCancelarFactura(ActionEvent event) {
        cambiarEstadoSeleccionada(
                idFactura -> facturaService.cancelarFactura(idFactura),
                "Factura cancelada",
                "Error al cancelar factura"
        );
    }

    @FunctionalInterface
    private interface OperacionEstado {
        void ejecutar(int idFactura) throws SQLException;
    }

    private void cambiarEstadoSeleccionada(OperacionEstado operacion, String mensajeOk, String mensajeErrorGenerico) {
        Factura seleccionada = tblFacturas.getSelectionModel().getSelectedItem();
        if (seleccionada == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Sin selección", null,
                    "Seleccioná una factura de la tabla primero.");
            return;
        }

        try {
            operacion.ejecutar(seleccionada.getIdFactura());
            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Factura", null, mensajeOk);
            refrescarTabla();
        } catch (EstadoFacturaInvalidoException e) {
            // FIX (flujo de estado): transición no permitida (ej: pagar una
            // factura ya anulada). Antes no existía esta validación y el
            // UPDATE se ejecutaba igual, dejando datos inconsistentes.
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Operación no permitida", null, e.getMessage());
        } catch (SQLException e) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, mensajeErrorGenerico);
        }
    }

    private void refrescarTabla() {
        List<Factura> facturas = facturaService.obtenerTodas();
        tblFacturas.setItems(FXCollections.observableArrayList(facturas));
        facturasFiltradas = new FilteredList<>(FXCollections.observableArrayList(facturas), f -> true);
        tblFacturas.setItems(facturasFiltradas);
        aplicarFiltrosSecundarios();
    }
}