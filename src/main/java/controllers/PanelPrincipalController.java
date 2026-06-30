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
import java.io.IOException;
import java.util.Objects;

public class PanelPrincipalController {

    @FXML private Label lblUsuario;
    @FXML private AnchorPane panelContenido;

    @FXML private Button btnCerrarSesion;

    private Usuario usuarioLogueado;


    public void inicializar(Usuario user) {
        this.usuarioLogueado = user;
        lblUsuario.setText("Bienvenido: " + user.getUsuario());
        aplicarPermisos(user.getRol());
    }

    private void aplicarPermisos(Rol rol) {
        if (rol.isEsEstilista()) {

        } else if ("Administrador".equals(rol.getNombre())) {

        }
    }




    @FXML
    public void abrirModuloClientes(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/moduloPrincipalCliente.fxml"));
            Parent root = loader.load();
            ModuloClienteController clienteController = loader.getController();
            Stage stage = new Stage();
            stage.setTitle("Gestión de Clientes");
            stage.setScene(new Scene(root));
            clienteController.setStage(stage);
            stage.show();

        } catch (IOException e) {
            System.err.println("❌ ERROR: No se pudo cargar la ventana del Módulo Cliente.");
            e.printStackTrace();
        }
    }
    @FXML
    private void exportarReporteGeneral(ActionEvent event) {
        ReporteGeneralController.exportarTodoEnExcel(); // ✅ llamado directo
    }


    private void cargarVista(String rutaFXML) {
        // Este método ya no se llama para el módulo Cliente, pero se mantiene si lo necesitas.
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
        } catch (NullPointerException e) {
            System.err.println("❌ Error: panelContenido es nulo. Verifique el fx:id.");
        }
    }

    @FXML
    public void cerrarSesion() {
        if (btnCerrarSesion == null) {
            System.err.println("Error FXML: btnCerrarSesion no está inicializado.");
            return;
        }

        try {
            Stage stage = (Stage) btnCerrarSesion.getScene().getWindow();
            stage.close();
            System.out.println("Sesión cerrada. La ventana ha sido cerrada.");
        } catch (Exception e) {
            System.err.println("Error al intentar cerrar la sesión: " + e.getMessage());
            e.printStackTrace();
        }
    }
    @FXML
    private void abrirReporteClientes(ActionEvent event) {
        cargarVentana("/interface/Reporte_Cliente.fxml", "Reporte de Clientes");
    }

    private void cargarVentana(String rutaFXML, String tituloVentana) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(rutaFXML));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setTitle(tituloVentana);
            stage.setScene(new Scene(root));
            stage.setResizable(false); // Opcional: evitar que se redimensione
            stage.show();
        } catch (IOException e) {
            System.err.println("🧨 Error al cargar la ventana: " + e.getMessage());
            mostrarAlerta(Alert.AlertType.ERROR, "Error", "No se pudo abrir la ventana: " );
        }
    }

    private void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }


    @FXML
    private void abrirModuloTurnos(ActionEvent event) {
        try {
            // Asegúrate de que esta ruta sea correcta
            Parent root = FXMLLoader.load(getClass().getResource("/interface/GestionTurnos.fxml"));

            // Obtener la Stage actual
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            // Crear una nueva Scene y asignarla
            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("PeluqPro - Módulo Gestión de Turnos");
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar datos", "No se pudieron obtener los datos de clientes.");
        }
    }

    @FXML
    private void abrirReporteFacturacion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/ReporteFacturacion.fxml"));
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Reporte de Facturación");
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            mostrarAlerta("Error", "No se pudo abrir el reporte de facturación.");
        }
    }
    @FXML
    private void abrirListadoFacturas() {
        cargarVentana("/interface/ListadoFacturas.fxml", "Listado de Facturas");
    }


    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }




}