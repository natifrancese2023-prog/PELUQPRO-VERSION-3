package claseslogicas;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.collections.ObservableList;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.math.RoundingMode;

public class ExportadorPDF implements ExportadorReporte {

    private void agregarEncabezadoInstitucional(Document documento) throws DocumentException {
        documento.add(new Paragraph("PeluqPro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
        documento.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
        documento.add(new Paragraph("Teléfono: 03572-400000"));
        documento.add(new Paragraph("Email: contacto@peluqpro.com"));
        documento.add(new Paragraph("Fecha de generación: " + LocalDate.now()));
        documento.add(new Paragraph(" "));
    }

    @Override
    public void exportarClientesReporte(ObservableList<ClienteReporteExtendido> clientes, File destino) throws Exception {
        Document documento = new Document();
        PdfWriter.getInstance(documento, new FileOutputStream(destino));
        documento.open();

        agregarEncabezadoInstitucional(documento);
        documento.add(new Paragraph("Reporte de Clientes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(5);
        tabla.setWidthPercentage(100);
        for (String header : new String[]{"Nombre", "Email", "Visitas", "Gasto total", "Estado último turno"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            tabla.addCell(cell);
        }

        for (ClienteReporteExtendido c : clientes) {
            tabla.addCell(c.getNombreCompleto());
            tabla.addCell(c.getEmail() != null ? c.getEmail() : "-");
            tabla.addCell(String.valueOf(c.getCantidadVisitas()));
            tabla.addCell("$ " + c.getGastoTotal().setScale(2, RoundingMode.HALF_UP));
            tabla.addCell(c.getEstadoUltimoTurno() != null ? c.getEstadoUltimoTurno() : "SIN ESTADO");
        }

        documento.add(tabla);
        documento.close();
    }

    @Override
    public void exportarFacturasReporte(ObservableList<FacturaResumen> facturas, File destino) throws Exception {
        Document documento = new Document();
        PdfWriter.getInstance(documento, new FileOutputStream(destino));
        documento.open();

        agregarEncabezadoInstitucional(documento);
        documento.add(new Paragraph("Reporte de Facturación", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(3);
        tabla.setWidthPercentage(100);
        for (String header : new String[]{"Fecha", "Total facturado", "Métodos de pago"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            tabla.addCell(cell);
        }

        for (FacturaResumen fr : facturas) {
            tabla.addCell(fr.getFecha().toString());
            tabla.addCell(String.format("$ %.2f", fr.getTotalFacturado()));
            tabla.addCell(fr.getResumenMetodosPago());
        }

        documento.add(tabla);
        documento.close();
    }

    /**
     * Exporta el PDF de una factura individual. No forma parte de
     * ExportadorReporte (es específico de factura, no un "reporte" genérico)
     * — antes vivía en la clase ExportadorReportes (con 's'), que fue
     * eliminada/renombrada sin actualizar el llamado en
     * ListadoFacturasController, dejando esa pantalla rota.
     */
    public static void exportarFacturaIndividualPDF(Factura factura, File destino) throws Exception {
        Document documento = new Document();
        PdfWriter.getInstance(documento, new FileOutputStream(destino));
        documento.open();

        documento.add(new Paragraph("PeluqPro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
        documento.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
        documento.add(new Paragraph("Teléfono: 03572-400000"));
        documento.add(new Paragraph("Email: contacto@peluqpro.com"));
        documento.add(new Paragraph("CUIT: 30-12345678-9"));
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("FACTURA", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
        documento.add(new Paragraph("N° Factura: " + factura.getIdFactura()));
        documento.add(new Paragraph("Fecha: " + factura.getFechaHora().toLocalDate()));
        documento.add(new Paragraph("Hora: " + factura.getFechaHora().toLocalTime()));
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("Cliente", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        Cliente c = factura.getCliente();
        if (c != null) {
            documento.add(new Paragraph("Nombre: " + c.getNombre() + " " + c.getApellido()));
            documento.add(new Paragraph("Documento: " + (c.getNumeroDocumento() != null ? c.getNumeroDocumento() : "N/A")));
        }
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("Detalle de servicios", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
        documento.add(new Paragraph(" "));

        PdfPTable tabla = new PdfPTable(4);
        tabla.setWidthPercentage(100);
        tabla.setWidths(new float[]{4f, 1f, 2f, 2f});

        for (String header : new String[]{"Servicio", "Cant.", "Precio unit.", "Subtotal"}) {
            PdfPCell cell = new PdfPCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
            cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            tabla.addCell(cell);
        }

        if (factura.getDetalles() != null && !factura.getDetalles().isEmpty()) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                String desc = detalle.getDescripcionServicio();
                if (desc == null || desc.isBlank()) desc = "Servicio";
                tabla.addCell(desc);
                tabla.addCell(String.valueOf(detalle.getCantidad()));
                tabla.addCell("$ " + detalle.getPrecioUnitario().setScale(2, RoundingMode.HALF_UP));
                tabla.addCell("$ " + detalle.getSubtotal().setScale(2, RoundingMode.HALF_UP));
            }
        } else {
            tabla.addCell("Sin servicios registrados");
            tabla.addCell("-");
            tabla.addCell("-");
            tabla.addCell("-");
        }

        documento.add(tabla);
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("Método de pago: " + factura.getMetodoPago()));
        documento.add(new Paragraph("TOTAL: $ " + factura.getMontoTotal().setScale(2, RoundingMode.HALF_UP),
                FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
        documento.add(new Paragraph(" "));

        documento.add(new Paragraph("¡Gracias por su visita!", FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));

        documento.close();
    }
}