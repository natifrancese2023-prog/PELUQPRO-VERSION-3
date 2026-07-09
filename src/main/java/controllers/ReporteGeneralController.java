package controllers;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.stage.FileChooser;
import java.io.File;

import utilidades.AlertaUtil;
import claseslogicas.ExportadorExcel;

public class ReporteGeneralController {

    @FXML
    public static void exportarTodoEnExcel() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exportar datos completos");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Archivo Excel", "*.xlsx"));
        File archivo = fileChooser.showSaveDialog(null);

        if (archivo != null) {
            try {

                ExportadorExcel exportador = new ExportadorExcel();
                exportador.exportarTodo(archivo);
                AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Exportación completa", null,
                        "Los datos fueron exportados correctamente.");
            } catch (Exception e) {
                AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de exportación", null,
                        "Ocurrió un error al generar el archivo Excel.");
                e.printStackTrace();
            }
        }
    }
}