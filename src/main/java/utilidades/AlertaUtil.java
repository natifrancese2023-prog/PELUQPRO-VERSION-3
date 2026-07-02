package utilidades;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;

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

}
