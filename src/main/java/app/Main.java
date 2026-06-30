package app;

import java.io.IOException;
import java.net.URL;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application {
    @Override
    public void start(Stage primaryStage) {
        try {

            URL fxmlLocation = getClass().getResource("/interface/login.fxml");

            if (fxmlLocation == null) {
                System.err.println("No se encontró el archivo FXML en la ruta especificada.");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            Scene scene = new Scene(root);
            primaryStage.setTitle("Sistema de Peluquería");
            primaryStage.setScene(scene);
            primaryStage.setResizable(false);
            primaryStage.show();
        } catch (IOException e) {
            System.err.println("Error al cargar la interfaz:");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}