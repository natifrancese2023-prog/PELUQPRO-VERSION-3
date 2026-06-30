package claseslogicas;
import clasesreportes.ClienteReporteExtendido;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.FontFactory;

import com.itextpdf.text.Document; // ✅ CORRECTO
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.WritableImage;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class ExportadorReportes {


    public static void exportarFacturacionPDF(
            List<FacturaResumen> resumenes,
            File destino,
            LocalDate desde,
            LocalDate hasta,
            WritableImage graficoFacturacion,
            WritableImage graficoMetodos) {

        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, new FileOutputStream(destino));
            documento.open();

            // Encabezado institucional
            documento.add(new Paragraph("PeluqPro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            documento.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
            documento.add(new Paragraph("Teléfono: 03572-400000"));
            documento.add(new Paragraph("Email: contacto@peluqpro.com"));
            documento.add(new Paragraph("CUIT: 30-12345678-9"));
            documento.add(new Paragraph("Fecha de generación: " + LocalDate.now()));
            documento.add(new Paragraph(" "));
            documento.add(new Paragraph("Reporte de Facturación", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            documento.add(new Paragraph("Período: " + desde + " a " + hasta));
            documento.add(new Paragraph(" "));

            // Tabla resumen
            PdfPTable tabla = new PdfPTable(3);
            tabla.setWidthPercentage(100);
            tabla.addCell("Fecha");
            tabla.addCell("Total facturado");
            tabla.addCell("Métodos de pago");

            for (FacturaResumen fr : resumenes) {
                tabla.addCell(fr.getFecha().toString());
                tabla.addCell(String.format("$ %.2f", fr.getTotalFacturado()));
                tabla.addCell(fr.getResumenMetodosPago());
            }

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            // Insertar gráfico de facturación
            if (graficoFacturacion != null) {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(graficoFacturacion, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                Image imagenPDF = Image.getInstance(baos.toByteArray());
                imagenPDF.scaleToFit(500, 300);
                imagenPDF.setAlignment(Image.ALIGN_CENTER);
                documento.add(new Paragraph("Gráfico: Evolución diaria de facturación"));
                documento.add(imagenPDF);
                documento.add(new Paragraph(" "));
            }

            // Insertar gráfico de métodos de pago
            if (graficoMetodos != null) {
                BufferedImage bufferedImage2 = SwingFXUtils.fromFXImage(graficoMetodos, null);
                ByteArrayOutputStream baos2 = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage2, "png", baos2);
                Image imagenPDF2 = Image.getInstance(baos2.toByteArray());
                imagenPDF2.scaleToFit(500, 300);
                imagenPDF2.setAlignment(Image.ALIGN_CENTER);
                documento.add(new Paragraph("Gráfico: Distribución de métodos de pago"));
                documento.add(imagenPDF2);
            }

            documento.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void exportarFacturaIndividualPDF(Factura factura, File destino) {
        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, new FileOutputStream(destino));
            documento.open();

            // ─── ENCABEZADO DEL NEGOCIO ───────────────────────────────────────
            documento.add(new Paragraph("PeluqPro",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18)));
            documento.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
            documento.add(new Paragraph("Teléfono: 03572-400000"));
            documento.add(new Paragraph("Email: contacto@peluqpro.com"));
            documento.add(new Paragraph("CUIT: 30-12345678-9"));
            documento.add(new Paragraph(" "));

            // ─── DATOS DE LA FACTURA ──────────────────────────────────────────
            documento.add(new Paragraph("FACTURA",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            documento.add(new Paragraph("N° Factura: " + factura.getIdFactura()));
            documento.add(new Paragraph("Fecha: " + factura.getFechaHora().toLocalDate()));
            documento.add(new Paragraph("Hora: " + factura.getFechaHora().toLocalTime()));
            documento.add(new Paragraph(" "));

            // ─── DATOS DEL CLIENTE ────────────────────────────────────────────
            documento.add(new Paragraph("Cliente",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            Cliente c = factura.getCliente();
            if (c != null) {
                documento.add(new Paragraph("Nombre: " + c.getNombre() + " " + c.getApellido()));
                documento.add(new Paragraph("Documento: " + (c.getNumeroDocumento() != null ? c.getNumeroDocumento() : "N/A")));
            }
            documento.add(new Paragraph(" "));

            // ─── DETALLE DE SERVICIOS ─────────────────────────────────────────
            documento.add(new Paragraph("Detalle de servicios",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            documento.add(new Paragraph(" "));

            PdfPTable tabla = new PdfPTable(4);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{4f, 1f, 2f, 2f});

            // Encabezados de tabla
            for (String header : new String[]{"Servicio", "Cant.", "Precio unit.", "Subtotal"}) {
                com.itextpdf.text.pdf.PdfPCell cell = new com.itextpdf.text.pdf.PdfPCell(
                        new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
                cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
                tabla.addCell(cell);
            }

            // Filas de detalle
            if (factura.getDetalles() != null && !factura.getDetalles().isEmpty()) {
                for (DetalleFactura detalle : factura.getDetalles()) {
                    String desc = detalle.getDescripcionServicio();
                    if (desc == null || desc.isBlank()) desc = "Servicio";
                    tabla.addCell(desc);
                    tabla.addCell(String.valueOf(detalle.getCantidad()));
                    tabla.addCell(String.format("$ %.2f", detalle.getPrecioUnitario()));
                    tabla.addCell(String.format("$ %.2f", detalle.getSubtotal()));
                }
            } else {
                // Fila vacía si no hay detalles
                tabla.addCell("Sin servicios registrados");
                tabla.addCell("-");
                tabla.addCell("-");
                tabla.addCell("-");
            }

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            // ─── TOTALES ──────────────────────────────────────────────────────
            documento.add(new Paragraph("Método de pago: " + factura.getMetodoPago()));
            documento.add(new Paragraph("TOTAL: $ " + String.format("%.2f", factura.getMontoTotal()),
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 13)));
            documento.add(new Paragraph(" "));

            // ─── PIE ──────────────────────────────────────────────────────────
            documento.add(new Paragraph("¡Gracias por su visita!",
                    FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 10)));

            documento.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void exportarClientesPDF(
            List<ClienteReporteExtendido> clientes,
            File destino,
            String tipoReporte,
            WritableImage graficoClientes
    ) {
        Document documento = new Document();
        try {
            PdfWriter.getInstance(documento, new FileOutputStream(destino));
            documento.open();

            // Encabezado institucional
            documento.add(new Paragraph("PeluqPro", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            documento.add(new Paragraph("Dirección: Av. Central 123, Laguna Larga"));
            documento.add(new Paragraph("Teléfono: 03572-400000"));
            documento.add(new Paragraph("Email: contacto@peluqpro.com"));
            documento.add(new Paragraph("Fecha de generación: " + LocalDate.now()));
            documento.add(new Paragraph("Tipo de reporte: " + tipoReporte));
            documento.add(new Paragraph(" "));

            // Título del reporte
            documento.add(new Paragraph("Reporte de Clientes", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
            documento.add(new Paragraph(" "));

            // Tabla de clientes
            PdfPTable tabla = new PdfPTable(5);
            tabla.setWidthPercentage(100);
            tabla.addCell("Nombre");
            tabla.addCell("Email");
            tabla.addCell("Visitas");
            tabla.addCell("Gasto total");
            tabla.addCell("Estado último turno");

            for (ClienteReporteExtendido c : clientes) {
                tabla.addCell(c.getNombreCompleto());
                tabla.addCell(c.getEmail());
                tabla.addCell(String.valueOf(c.getCantidadVisitas()));
                tabla.addCell(String.format("$ %.2f", c.getGastoTotal()));
                tabla.addCell(c.getEstadoUltimoTurno());
            }

            documento.add(tabla);
            documento.add(new Paragraph(" "));

            // Insertar gráfico si existe
            if (graficoClientes != null) {
                BufferedImage bufferedImage = SwingFXUtils.fromFXImage(graficoClientes, null);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ImageIO.write(bufferedImage, "png", baos);
                Image imagenPDF = Image.getInstance(baos.toByteArray());
                imagenPDF.scaleToFit(500, 300);
                imagenPDF.setAlignment(Image.ALIGN_CENTER);
                documento.add(new Paragraph("Gráfico: Gasto total por cliente"));
                documento.add(imagenPDF);
            }

            documento.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
