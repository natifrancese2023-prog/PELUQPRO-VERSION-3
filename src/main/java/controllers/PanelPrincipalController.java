package controllers;

import claseslogicas.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import utilidades.AlertaUtil;

import java.io.IOException;
import java.util.Objects;

public class PanelPrincipalController {

    @FXML private Label lblUsuario;
    @FXML private AnchorPane apContenidoPrincipal;

    @FXML private Button btnCerrarSesion;

    @FXML private Button btnGestionClientes;
    @FXML private Button btnGestionTurnos;
    @FXML private Button btnReporteCliente;
    @FXML private Button btnListadoFacturas;
    @FXML private Button btnReporteFacturacion;
    @FXML private Button btnExportarTodoPower;

    private Usuario usuarioLogueado;

    public void inicializar(Usuario user) {
        this.usuarioLogueado = user;
        lblUsuario.setText("Bienvenido: " + user.getUsuario()+"   ");
        aplicarPermisos();
    }

    private void aplicarPermisos() {
        boolean esGerente = utilidades.PermisosUtil.esGerente();
        boolean esEstilista = utilidades.PermisosUtil.esEstilista();

        btnGestionClientes.setDisable(false);
        btnGestionTurnos.setDisable(esEstilista);
        btnReporteCliente.setDisable(!esGerente);
        btnReporteFacturacion.setDisable(!esGerente);
        btnExportarTodoPower.setDisable(!esGerente);
        btnListadoFacturas.setDisable(esEstilista);
    }


    private void cargarVista(String rutaFXML) {
        try {
            Parent vista = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(rutaFXML)));
            apContenidoPrincipal.getChildren().setAll(vista);

            AnchorPane.setTopAnchor(vista, 0.0);
            AnchorPane.setBottomAnchor(vista, 0.0);
            AnchorPane.setLeftAnchor(vista, 0.0);
            AnchorPane.setRightAnchor(vista, 0.0);

        } catch (IOException e) {
            System.err.println("❌ Error al cargar la vista FXML: " + rutaFXML);
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo cargar la vista: " + rutaFXML);
        } catch (NullPointerException e) {
            System.err.println("❌ Error: panelContenido es nulo. Verifique el fx:id.");
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "El panelContenido es nulo. Verifique el fx:id.");
        }
    }

    // Botones → todos usan cargarVista()
    @FXML
    public void abrirModuloClientes(ActionEvent event) {
        cargarVista("/interface/moduloPrincipalCliente.fxml");
    }

    @FXML
    private void abrirModuloTurnos(ActionEvent event) {
        cargarVista("/interface/GestionTurnos.fxml");
    }

    @FXML
    private void abrirReporteClientes(ActionEvent event) {
        cargarVista("/interface/Reporte_Cliente.fxml");
    }

    @FXML
    private void abrirReporteFacturacion() {
        cargarVista("/interface/ReporteFacturacion.fxml");
    }

    @FXML
    private void abrirListadoFacturas() {
        cargarVista("/interface/ListadoFacturas.fxml");
    }

    @FXML
    private void exportarReporteGeneral(ActionEvent event) {
        ReporteGeneralController.exportarTodoEnExcel();
    }

    @FXML
    public void cerrarSesion() {
        try {
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            stage.close();
            System.out.println("Sesión cerrada. La ventana ha sido cerrada.");
        } catch (Exception e) {
            System.err.println("Error al intentar cerrar la sesión: " + e.getMessage());
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo cerrar la sesión.");
        }
    }
}
