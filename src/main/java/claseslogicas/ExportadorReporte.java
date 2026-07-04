package claseslogicas;

import javafx.collections.ObservableList;
import java.io.File;

public interface ExportadorReporte {
    void exportarClientesReporte(ObservableList<ClienteReporteExtendido> clientes, File destino) throws Exception;
    void exportarFacturasReporte(ObservableList<FacturaResumen> facturas, File destino) throws Exception;
}