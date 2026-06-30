package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.stage.FileChooser;

import java.awt.event.ActionEvent;
import java.io.File;
import claseslogicas.ExportadorExcel;

public class ReporteGeneralController {



    @FXML
    public static void exportarTodoEnExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar datos completos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo Excel", "*.xlsx"));
        File archivo = fileChooser.showSaveDialog(null);
        if (archivo != null) {

            ExportadorExcel.exportarTodoEnExcel(archivo);
            mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación completa", "Los datos fueron exportados correctamente.");
        }
    }

    private static void mostrarAlerta(Alert.AlertType tipo, String titulo, String mensaje) {
        Alert alerta = new Alert(tipo);
        alerta.setTitle(titulo);
        alerta.setHeaderText(null);
        alerta.setContentText(mensaje);
        alerta.showAndWait();
    }
}
