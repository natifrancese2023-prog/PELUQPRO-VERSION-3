package controllers;

import claseslogicas.Cliente;
import service.ClienteService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.Optional;
import java.util.ResourceBundle;
import utilidades.AlertaUtil;

public class EliminarClienteController implements Initializable {

    private final ClienteService clienteService = new ClienteService();
    private Cliente clienteActual = null;

    @FXML private VBox vboxDatosCliente;
    @FXML private TextField txtNombre;
    @FXML private TextField txtApellido;
    @FXML private Button btnEliminar;
    @FXML private Button btnCancelar;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        vboxDatosCliente.setDisable(true);
        btnEliminar.setDisable(true);
    }

    // ✅ Método para recibir cliente desde la Ficha
    public void setCliente(Cliente cliente) {
        this.clienteActual = cliente;
        txtNombre.setText(cliente.getNombre());
        txtApellido.setText(cliente.getApellido());
        vboxDatosCliente.setDisable(false);
        btnEliminar.setDisable(false);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnEliminar.getScene().getWindow();
        stage.close();
    }


    @FXML
    private void handleEliminarCliente() {
        if (clienteActual == null) {
            AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error", null,"Debe seleccionar un cliente antes de eliminar.");
            System.out.println("⚠️ No se seleccionó ningún cliente para eliminar.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmar Eliminación");
        confirm.setHeaderText("¡ADVERTENCIA: Acción Irreversible!");
        confirm.setContentText("¿Está seguro que desea eliminar al cliente "
                + clienteActual.getNombre() + " " + clienteActual.getApellido() + "?");

        Optional<ButtonType> resultado = confirm.showAndWait();

        if (resultado.isPresent() && resultado.get() == ButtonType.OK) {
            try {
                System.out.println("🔍 Intentando eliminar cliente con ID: " + clienteActual.getIdCliente());

                boolean eliminado = clienteService.eliminarCliente(clienteActual);

                if (eliminado) {
                    AlertaUtil.mostrarAlerta(Alert.AlertType.INFORMATION, "Éxito", null,"Cliente eliminado con éxito del sistema.");
                    System.out.println("✅ Cliente eliminado correctamente.");
                } else {
                    AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Fallo en la Eliminación", null,"La eliminación del cliente no se pudo completar.");
                    System.out.println("❌ El método eliminar devolvió false. Posible ID inexistente o bloqueo por integridad.");
                }

            } catch (SQLException e) {
                System.out.println("💥 SQLException detectada:");
                e.printStackTrace(); // muestra traza completa
                System.out.println("🧾 Mensaje de error: " + e.getMessage());

                if (e.getMessage().contains("a foreign key constraint fails")) {
                    AlertaUtil.mostrarAlerta(Alert.AlertType.WARNING, "Eliminación bloqueada",
                            null,"Este cliente tiene registros asociados. Elimine primero sus visitas, turnos o facturas.");
                    System.out.println("🔒 Eliminación bloqueada por restricción de clave foránea.");
                } else {
                    AlertaUtil.mostrarAlerta(Alert.AlertType.ERROR, "Error de Base de Datos",
                            null,"Ocurrió un error al eliminar el cliente.");
                    System.out.println("⚠️ Error inesperado al eliminar cliente.");
                }
            } finally {
                cerrarVentana();
                System.out.println("🔚 Ventana de eliminación cerrada.");
            }
        } else {
            System.out.println("❎ Eliminación cancelada por el usuario.");
        }
    }

    @FXML
    private void handleCancelar() {
        limpiarCampos();
        vboxDatosCliente.setDisable(true);
        btnEliminar.setDisable(true);
        clienteActual = null;
    }

    private void limpiarCampos() {
        txtNombre.clear();
        txtApellido.clear();
    }

}