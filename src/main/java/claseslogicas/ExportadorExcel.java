package claseslogicas;

import javafx.collections.ObservableList;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.math.RoundingMode;
import java.time.LocalDate;

public class ExportadorExcel implements ExportadorReporte {

    @Override
    public void exportarClientesReporte(ObservableList<ClienteReporteExtendido> clientes, File destino) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Reporte de Clientes");

            // Encabezado institucional
            hoja.createRow(0).createCell(0).setCellValue("PeluqPro");
            hoja.createRow(1).createCell(0).setCellValue("Dirección: Av. Central 123, Laguna Larga");
            hoja.createRow(2).createCell(0).setCellValue("Teléfono: 03572-400000");
            hoja.createRow(3).createCell(0).setCellValue("Email: contacto@peluqpro.com");
            hoja.createRow(4).createCell(0).setCellValue("Fecha de generación: " + LocalDate.now());

            // Encabezado de tabla
            Row encabezado = hoja.createRow(6);
            String[] columnas = {"Nombre", "Email", "Visitas", "Gasto total", "Estado último turno"};
            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
            }

            // Datos
            int filaActual = 7;
            for (ClienteReporteExtendido c : clientes) {
                Row fila = hoja.createRow(filaActual++);
                fila.createCell(0).setCellValue(c.getNombreCompleto());
                fila.createCell(1).setCellValue(c.getEmail());
                fila.createCell(2).setCellValue(c.getCantidadVisitas());
                fila.createCell(3).setCellValue(c.getGastoTotal().setScale(2, RoundingMode.HALF_UP).doubleValue());
                fila.createCell(4).setCellValue(c.getEstadoUltimoTurno());
            }

            // Guardar archivo
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }
        }
    }

    @Override
    public void exportarFacturasReporte(ObservableList<FacturaResumen> facturas, File destino) throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Reporte de Facturación");

            hoja.createRow(0).createCell(0).setCellValue("PeluqPro");
            hoja.createRow(1).createCell(0).setCellValue("Fecha de generación: " + LocalDate.now());

            Row encabezado = hoja.createRow(3);
            String[] columnas = {"Fecha", "Total facturado", "Métodos de pago"};
            for (int i = 0; i < columnas.length; i++) {
                encabezado.createCell(i).setCellValue(columnas[i]);
            }

            int filaActual = 4;
            for (FacturaResumen fr : facturas) {
                Row fila = hoja.createRow(filaActual++);
                fila.createCell(0).setCellValue(fr.getFecha().toString());
                fila.createCell(1).setCellValue(fr.getTotalFacturado().toPlainString());

                fila.createCell(2).setCellValue(fr.getResumenMetodosPago());
            }

            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }
        }
    }


    public void exportarTodo(File destino) throws Exception {

    }
}