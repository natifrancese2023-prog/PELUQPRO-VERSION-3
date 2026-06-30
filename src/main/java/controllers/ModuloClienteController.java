package controllers;

import java.io.IOException;
import java.net.URL;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.layout.AnchorPane;
import javafx.scene.Parent;
import javafx.stage.Stage;

public class ModuloClienteController {

    @FXML
    private AnchorPane panelContenidoCliente;

    private Stage stage;

    public void setStage(Stage stage) {
        if (stage == null) {
            System.err.println("❌ ERROR: Stage recibido es null.");
            return;
        }
        this.stage = stage;
    }

    @FXML
    private void volverAPanelPrincipal() {
        if (this.stage != null) {
            this.stage.close();
            System.out.println("✅ Módulo Cliente cerrado. Volviendo al Panel Principal.");
        } else {
            System.err.println("❌ ERROR: El Stage no fue inyectado en ModuloClienteController.");
        }
    }

    @FXML
    private void cargarSubModulo(ActionEvent event) {
        Button sourceButton = (Button) event.getSource();
        String fxmlPath = (String) sourceButton.getUserData();

        if (fxmlPath == null || fxmlPath.isEmpty()) {
            System.err.println("❌ ERROR: El botón no tiene userData configurado.");
            return;
        }

        URL location = getClass().getResource(fxmlPath);

        if (location == null) {
            System.err.println("❌ ERROR: No se encontró el recurso FXML en la ruta: " + fxmlPath);
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(location);
            Parent root = loader.load();

            panelContenidoCliente.getChildren().clear();
            panelContenidoCliente.getChildren().add(root);

            AnchorPane.setTopAnchor(root, 0.0);
            AnchorPane.setBottomAnchor(root, 0.0);
            AnchorPane.setLeftAnchor(root, 0.0);
            AnchorPane.setRightAnchor(root, 0.0);

            System.out.println("✅ Submódulo cargado: " + fxmlPath);

        } catch (IOException e) {
            System.err.println("❌ ERROR FATAL al cargar el FXML: " + fxmlPath);
            System.err.println("Causa: " + e.getMessage());
            e.printStackTrace();
        }
    }

}