package test;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class TestJPA extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        // Apuntá al FXML de tu módulo clientes
        // Cambiá la ruta según donde tengas tu archivo
        Parent root = FXMLLoader.load(
                getClass().getResource("/interface/moduloPrincipalCliente.fxml")
        );
        stage.setTitle("TEST - Módulo Clientes");
        stage.setScene(new Scene(root));
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}