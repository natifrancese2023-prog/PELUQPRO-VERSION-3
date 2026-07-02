package claseslogicas;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import javafx.collections.ObservableList;

import java.io.File;

import static org.apache.poi.poifs.macros.Module.ModuleType.Document;

public class ExportadorPDF implements ExportadorReporte {

    @Override
    public void exportarClientes(ObservableList<FacturaResumen> clientes, File destino) {
        try {
            PdfWriter writer = new PdfWriter(destino);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Encabezado institucional
            document.add(new Paragraph("PeluqPro").setBold().setFontSize(16));
            document.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
            document.add(new Paragraph("Teléfono: 03572-400000"));
            document.add(new Paragraph("Email: contacto@peluqpro.com"));
            document.add(new Paragraph("Fecha de generación: " + java.time.LocalDate.now()));
            document.add(new Paragraph("\nReporte de Clientes\n").setBold());

            // Tabla de clientes
            float[] columnWidths = {200F, 200F, 80F, 100F, 150F};
            Table table = new Table(columnWidths);

            table.addCell("Nombre");
            table.addCell("Email");
            table.addCell("Visitas");
            table.addCell("Gasto total");
            table.addCell("Estado último turno");

            for (ClienteReporteExtendido c : clientes) {
                table.addCell(c.getNombreCompleto());
                table.addCell(c.getEmail());
                table.addCell(String.valueOf(c.getCantidadVisitas()));
                table.addCell("$" + c.getGastoTotal().setScale(2, java.math.RoundingMode.HALF_UP));
                table.addCell(c.getEstadoUltimoTurno() != null ? c.getEstadoUltimoTurno() : "SIN ESTADO");
            }

            document.add(table);
            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void exportarTodo(File destino) {
        try {
            PdfWriter writer = new PdfWriter(destino);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("PeluqPro - Reporte General").setBold().setFontSize(16));
            document.add(new Paragraph("Fecha de generación: " + java.time.LocalDate.now()));
            document.add(new Paragraph("\nEste reporte incluye Clientes, Turnos, Facturas y Visitas.\n"));

            // 👉 Acá podés replicar la lógica de Excel pero en tablas PDF.
            // Ejemplo: tabla de clientes, tabla de turnos, etc.
            // Cada sección se arma con un Table y se agrega al documento.

            document.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
