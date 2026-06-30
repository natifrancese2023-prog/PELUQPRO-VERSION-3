package app;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class TestJavaFX extends Application {
    @Override
    public void start(Stage stage) {
        Label label = new Label("¡Hola, Nati! JavaFX está funcionando 🎉");
        Scene scene = new Scene(label, 300, 100);
        stage.setScene(scene);
        stage.setTitle("Prueba JavaFX");
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}