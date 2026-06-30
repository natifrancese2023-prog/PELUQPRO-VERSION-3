package controllers;

import claseslogicas.Usuario;
import dao.UsuarioDao;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;
import utilidades.SesionManager; // 🏆 Importación del SesionManager

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private TextField txtPassword;

    @FXML
    private Button btnIngresar;

    private UsuarioDao usuarioDao = new UsuarioDao();

    @FXML
    private void loginAction() {
        System.out.println("Botón presionado");

        String usuario = txtUsuario.getText().trim();
        String contraseña = txtPassword.getText().trim();

        if (usuario.isEmpty() || contraseña.isEmpty()) {
            mostrarAlerta("Campos vacíos", "Por favor, ingrese usuario y contraseña.");
            return;
        }

        Usuario user = usuarioDao.validarUsuario(usuario, contraseña);

        if (user != null) {
            // 🏆 1. INICIAR SESIÓN GLOBALMENTE
            SesionManager.getInstance().iniciarSesion(user);

            mostrarAlerta("Bienvenido", "Acceso concedido. Rol: " + user.getRol());

            // 2. Cargar la página principal
            cargarPanelPrincipal(user);

            // 3. Cerrar la ventana de Login
            cerrarVentanaActual();
        } else {
            System.out.println("Usuario ingresado: '" + usuario + "'");
            System.out.println("Contraseña ingresada: '" + contraseña + "'");
            mostrarAlerta("Error de acceso", "Usuario o contraseña incorrectos.");
        }
    }

    private void mostrarAlerta(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void cargarPanelPrincipal(Usuario user) {
        try {
            // Cuidado con la ruta. Debe ser relativa al classpath.
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/panelPrincipal.fxml"));
            Parent root = loader.load();

            // Obtener el controlador del Panel Principal.
            PanelPrincipalController panelController = loader.getController();

            // Llamamos al método para pasar el Usuario (necesario para la gestión de permisos)
            panelController.inicializar(user);

            // Crear y mostrar la nueva ventana (Stage).
            Stage stage = new Stage();
            stage.setTitle("PeluqPro - Panel Principal");
            stage.setScene(new Scene(root));
            stage.show();

        } catch (IOException e) {
            System.err.println("Error al cargar el Panel Principal. Revisa la ruta.");
            e.printStackTrace();
            mostrarAlerta("Error de Carga", "No se pudo iniciar la aplicación principal.");
        }
    }

    private void cerrarVentanaActual() {
        btnIngresar.getScene().getWindow().hide();
    }

    // Estos métodos son opcionales y a menudo no son necesarios en controladores:
    public Button getBtnIngresar() {
        return btnIngresar;
    }

    public void setBtnIngresar(Button btnIngresar) {
        this.btnIngresar = btnIngresar;
    }
}