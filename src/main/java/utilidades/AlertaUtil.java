package utilidades;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

public class AlertaUtil {

    private AlertaUtil() {
        // Constructor privado para evitar instanciación
    }
    public static void mostrarAlerta(AlertType tipo, String titulo, String header, String mensaje) {
        Alert alert = new Alert(tipo);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    public static boolean mostrarConfirmacion(String titulo, String header, String contenido) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(header);
        alert.setContentText(contenido);

        ButtonType btnSi = new ButtonType("Sí", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(btnSi, btnNo);

        return alert.showAndWait().orElse(btnNo) == btnSi;
    }


}
