package controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import utilidades.AlertaUtil;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class GestionTurnosController implements Initializable {

    @FXML private AnchorPane panelContenidoTurnos;
    @FXML private Button btnVolverPanel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // No se carga nada por defecto, ya que el panel está vacío inicialmente
    }

    /**
     * Carga un submódulo dentro del panel central usando la ruta FXML definida en el botón.
     */
    @FXML
    private void cargarSubModulo(javafx.event.ActionEvent event) {
        Button boton = (Button) event.getSource();
        String fxmlPath = (String) boton.getUserData();

        if (fxmlPath == null || fxmlPath.isEmpty()) {
            System.err.println("Error: El userData del botón no contiene una ruta FXML válida.");
            return;
        }

        try {
            URL url = getClass().getResource(fxmlPath);
            if (url == null) throw new IOException("No se encontró el archivo FXML: " + fxmlPath);

            Parent nuevoContenido = FXMLLoader.load(url);
            panelContenidoTurnos.getChildren().setAll(nuevoContenido);

            AnchorPane.setTopAnchor(nuevoContenido, 0.0);
            AnchorPane.setBottomAnchor(nuevoContenido, 0.0);
            AnchorPane.setLeftAnchor(nuevoContenido, 0.0);
            AnchorPane.setRightAnchor(nuevoContenido, 0.0);

        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error al cargar módulo", null, "No se pudo cargar el submódulo solicitado.");
        }
    }

    /**
     * Vuelve al panel principal del sistema.
     */
    @FXML
    private void handleVolverPanelPrincipal(javafx.event.ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/interface/panelPrincipal.fxml"));
            Parent root = loader.load();

            Scene scene = new Scene(root);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();

            System.out.println("✅ Volviendo al panel principal.");

        } catch (IOException e) {
            e.printStackTrace();
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null, "No se pudo volver al panel principal.");
        } catch (NullPointerException npe) {
            System.err.println("❌ No se encontró el archivo MainPanel.fxml en la ruta especificada.");
        }
    }

    @FXML
    private void handleVolverPanelPrincipal() {
        Stage stage = (Stage) btnVolverPanel.getScene().getWindow();
        stage.close();
    }
}
