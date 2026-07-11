package controllers;

import claseslogicas.Usuario;
import dao.UsuarioDAO;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import utilidades.SesionManager;
import utilidades.AlertaUtil;

import java.io.IOException;

public class LoginController {

    @FXML
    private TextField txtUsuario;

    @FXML
    private TextField txtPassword;

    @FXML
    private Button btnIngresar;

    private UsuarioDAO usuarioDao = new UsuarioDAO();

    @FXML
    private void loginAction() {
        System.out.println("Botón presionado");

        String usuario = txtUsuario.getText().trim();
        String contraseña = txtPassword.getText().trim();

        if (usuario.isEmpty() || contraseña.isEmpty()) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Campos vacíos", null, "Por favor, ingrese usuario y contraseña.");
            return;
        }

        Usuario user = usuarioDao.validarUsuario(usuario, contraseña);

        if (user != null) {
            // 🏆 1. INICIAR SESIÓN GLOBALMENTE
            SesionManager.getInstance().iniciarSesion(user);

            AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Bienvenido", null, "Acceso concedido. Rol: " + user.getRol());

            // 2. Cargar la página principal
            cargarPanelPrincipal(user);

            // 3. Cerrar la ventana de Login
            cerrarVentanaActual();
        } else {
            System.out.println("Usuario ingresado: '" + usuario + "'");
            System.out.println("Contraseña ingresada: '" + contraseña + "'");
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de acceso", null, "Usuario o contraseña incorrectos.");
        }
    }

    private void cargarPanelPrincipal(Usuario user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/panelPrincipal.fxml"));
            Parent root = loader.load();

            PanelPrincipalController panelController = loader.getController();
            panelController.inicializar(user);

            Stage stage = new Stage();
            stage.setTitle("PeluqPro - Panel Principal");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            System.err.println("Error al cargar el Panel Principal. Revisa la ruta.");
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de Carga", null, "No se pudo iniciar la aplicación principal.");
        }
    }

    private void cerrarVentanaActual() {
        btnIngresar.getScene().getWindow().hide();
    }

    public Button getBtnIngresar() {
        return btnIngresar;
    }

    public void setBtnIngresar(Button btnIngresar) {
        this.btnIngresar = btnIngresar;
    }
}
