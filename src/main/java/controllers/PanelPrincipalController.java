package controllers;

import claseslogicas.Rol;
import claseslogicas.Usuario;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import utilidades.AlertaUtil;
import java.io.IOException;
import java.util.Objects;
import java.util.function.BiConsumer;

public class PanelPrincipalController {

    @FXML private Label lblUsuario;
    @FXML private AnchorPane panelContenido;
    @FXML private Button btnCerrarSesion;

    @FXML private Button btnGestionTurnos;
    @FXML private Button btnReporteCliente;
    @FXML private Button btnListadoFacturas;
    @FXML private Button btnReporteFacturacion;
    @FXML private Button btnExportarTodoPower;

    @FXML private Button btnAlta;
    @FXML private Button btnFinalizar;
    @FXML private Button btnFacturar;
    @FXML private Button btnCancelar;
    @FXML private Button btnModificar;
    @FXML private Button btnEliminar;
    @FXML private Button btnListar;
    @FXML private Button btnConfirmar;


    private Usuario usuarioLogueado;
    public void inicializar(Usuario user) {
        this.usuarioLogueado = user;
        lblUsuario.setText("Bienvenido: " + user.getUsuario());
        aplicarPermisos(); // sin parámetros
    }
    private void aplicarPermisos() {
        Rol rol = usuarioLogueado.getRol();
        String nombreRol = rol.getNombre();

        if ("Estilista".equalsIgnoreCase(nombreRol) || rol.isEsEstilista()) {
            // 🔒 Estilista: acceso muy limitado

            btnGestionTurnos.setDisable(true);
            btnReporteCliente.setDisable(true);
            btnListadoFacturas.setDisable(true);
            btnReporteFacturacion.setDisable(true);
            btnExportarTodoPower.setDisable(true);

            // Módulo Cliente
            btnAlta.setDisable(true);          // no registra clientes
            btnListar.setDisable(true);
            btnModificar.setDisable(true);
            btnEliminar.setDisable(true);
        // no listado


            // Turnos
            btnAlta.setDisable(true);          // no registra turnos

            btnConfirmar.setDisable(true);
            btnFinalizar.setDisable(true);
            btnFacturar.setDisable(true);
            btnCancelar.setDisable(true);



        } else if ("Recepcionista".equalsIgnoreCase(nombreRol)) {
            btnReporteCliente.setDisable(true);
            btnListadoFacturas.setDisable(true);
            btnReporteFacturacion.setDisable(true);
            btnExportarTodoPower.setDisable(true);

        } else if ("Gerente".equalsIgnoreCase(nombreRol)) {
            // ✅ Gerente: acceso completo
            // No se deshabilita nada
        }
    }


    @FXML
    public void abrirModuloClientes(ActionEvent event) {
        try {
            cargarVentana("/interface/moduloPrincipalCliente.fxml", "Gestión de Clientes",
                    (ModuloClienteController controller, Stage stage) -> controller.setStage(stage));
        } catch (IOException e) {
            System.err.println("❌ ERROR: No se pudo cargar la ventana del Módulo Cliente.");
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo cargar la ventana del Módulo Cliente.");
        }
    }

    @FXML
    private void exportarReporteGeneral(ActionEvent event) {
        ReporteGeneralController.exportarTodoEnExcel();
    }

    private void cargarVista(String rutaFXML) {
        try {
            Parent vista = FXMLLoader.load(Objects.requireNonNull(getClass().getResource(rutaFXML)));
            panelContenido.getChildren().setAll(vista);

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

    @FXML
    public void cerrarSesion() {
        if (btnCerrarSesion == null) {
            System.err.println("Error FXML: btnCerrarSesion no está inicializado.");
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "El botón de cerrar sesión no está inicializado.");
            return;
        }

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

    @FXML
    private void abrirReporteClientes(ActionEvent event) {
        try {
            cargarVentana("/interface/Reporte_Cliente.fxml", "Reporte de Clientes");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir el reporte de clientes.");
        }
    }

    private void cargarVentana(String rutaFXML, String tituloVentana) throws IOException {
        cargarVentana(rutaFXML, tituloVentana, (Object controller, Stage stage) -> { /* sin configuración extra */ });
    }

    private <T> void cargarVentana(String rutaFXML, String tituloVentana, BiConsumer<T, Stage> configurarControlador) throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
        Parent root = loader.load();

        Stage stage = new Stage();
        stage.setTitle(tituloVentana);
        stage.setScene(new Scene(root));
        stage.setResizable(false);

        T controller = loader.getController();
        configurarControlador.accept(controller, stage);

        stage.show();
    }

    @FXML
    private void abrirModuloTurnos(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/interface/GestionTurnos.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("PeluqPro - Módulo Gestión de Turnos");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar datos", null, "No se pudieron obtener los datos de clientes.");
        }
    }

    @FXML
    private void abrirReporteFacturacion() {
        try {
            cargarVentana("/interface/ReporteFacturacion.fxml", "Reporte de Facturación");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir el reporte de facturación.");
        }
    }

    @FXML
    private void abrirListadoFacturas() {
        try {
            cargarVentana("/interface/ListadoFacturas.fxml", "Listado de Facturas");
        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo abrir el listado de facturas.");
        }
    }
}
