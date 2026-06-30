
package claseslogicas;

import clasesreportes.ClienteReporteExtendido;
import dao.*;
import dao.FacturaDAO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDate;
import java.util.List;

public class ExportadorExcel {

    public static void exportarClientesXLSX(List<ClienteReporteExtendido> clientes, File destino) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet hoja = workbook.createSheet("Reporte de Clientes");

            // Estilos
            CellStyle estiloTitulo = workbook.createCellStyle();
            Font fuenteTitulo = workbook.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 14);
            estiloTitulo.setFont(fuenteTitulo);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuenteEncabezado = workbook.createFont();
            fuenteEncabezado.setBold(true);
            estiloEncabezado.setFont(fuenteEncabezado);

            // Encabezado institucional
            Row fila0 = hoja.createRow(0);
            fila0.createCell(0).setCellValue("PeluqPro");
            fila0.getCell(0).setCellStyle(estiloTitulo);

            hoja.createRow(1).createCell(0).setCellValue("Dirección: Av. Central 123, Laguna Larga");
            hoja.createRow(2).createCell(0).setCellValue("Teléfono: 03572-400000");
            hoja.createRow(3).createCell(0).setCellValue("Email: contacto@peluqpro.com");
            hoja.createRow(4).createCell(0).setCellValue("Fecha de generación: " + LocalDate.now());

            // Título del reporte
            hoja.createRow(6).createCell(0).setCellValue("Reporte de Clientes");

            // Encabezado de tabla
            Row encabezado = hoja.createRow(8);
            String[] columnas = {"Nombre", "Email", "Visitas", "Gasto total", "Estado último turno"};
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = encabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            // Datos
            int filaActual = 9;
            for (ClienteReporteExtendido c : clientes) {
                Row fila = hoja.createRow(filaActual++);
                fila.createCell(0).setCellValue(c.getNombreCompleto());
                fila.createCell(1).setCellValue(c.getEmail());
                fila.createCell(2).setCellValue(c.getCantidadVisitas());
                fila.createCell(3).setCellValue(c.getGastoTotal());
                fila.createCell(4).setCellValue(c.getEstadoUltimoTurno());
            }

            // Ajuste de ancho
            for (int i = 0; i < columnas.length; i++) {
                hoja.autoSizeColumn(i);
            }

            // Guardar archivo
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void exportarTodoEnExcel(File destino) {
        try (Workbook workbook = new XSSFWorkbook()) {

            int fila;

            // CLIENTES (con datos heredados de Persona + cantidad de visitas)
            Sheet hojaClientes = workbook.createSheet("Clientes");
            List<Cliente> clientes = ClienteDAO.obtenerTodos();
            Row encabezadoClientes = hojaClientes.createRow(0);
            String[] columnasClientes = {"ID Cliente", "Nombre", "Apellido", "Email", "Teléfono", "Fecha Alta", "Visitas"};
            for (int i = 0; i < columnasClientes.length; i++) {
                encabezadoClientes.createCell(i).setCellValue(columnasClientes[i]);
            }
            fila = 1;
            for (Cliente c : clientes) {
                Row row = hojaClientes.createRow(fila++);
                row.createCell(0).setCellValue(c.getIdCliente());
                row.createCell(1).setCellValue(c.getPersona() != null ? c.getPersona().getNombre() : "SIN NOMBRE");
                row.createCell(2).setCellValue(c.getPersona() != null ? c.getPersona().getApellido() : "SIN APELLIDO");
                row.createCell(3).setCellValue(c.getPersona() != null ? c.getPersona().getEmail() : "SIN EMAIL");
                row.createCell(4).setCellValue(c.getPersona() != null ? c.getPersona().getTelefono() : "SIN TELÉFONO");
                row.createCell(5).setCellValue(c.getFechaAlta() != null ? c.getFechaAlta().toString() : "SIN FECHA");

                // ✅ Nueva columna: cantidad de visitas
                int visitas = ClienteDAO.contarVisitasPorIdCliente(c.getIdCliente());
                row.createCell(6).setCellValue(visitas);
            }

            // TURNOS (con cliente y estilista)
            Sheet hojaTurnos = workbook.createSheet("Turnos");
            List<Turno> turnos = TurnoDAO.obtenerTodos();
            Row encabezadoTurnos = hojaTurnos.createRow(0);
            String[] columnasTurnos = {"ID Turno", "Fecha", "Hora Inicio", "Hora Fin", "Documento Cliente", "Documento Estilista", "Servicios", "Motivo", "Fecha Creación"};
            for (int i = 0; i < columnasTurnos.length; i++) {
                encabezadoTurnos.createCell(i).setCellValue(columnasTurnos[i]);
            }
            fila = 1;
            for (Turno t : turnos) {
                Row row = hojaTurnos.createRow(fila++);
                row.createCell(0).setCellValue(t.getIdTurno());
                row.createCell(1).setCellValue(t.getFecha() != null ? t.getFecha().toString() : "SIN FECHA");
                row.createCell(2).setCellValue(t.getHoraInicio() != null ? t.getHoraInicio().toString() : "SIN HORA");
                row.createCell(3).setCellValue(t.getHoraFin() != null ? t.getHoraFin().toString() : "SIN HORA");
                row.createCell(4).setCellValue(t.getClienteDocumento() != null ? t.getClienteDocumento() : "SIN CLIENTE");
                row.createCell(5).setCellValue(t.getEmpleadoDocumento() != null ? t.getEmpleadoDocumento() : "SIN ESTILISTA");
                row.createCell(6).setCellValue((t.getServicios() != null && !t.getServicios().isEmpty()) ? t.getResumenServicios() : "SIN SERVICIOS");
                row.createCell(7).setCellValue(t.getMotivoLog() != null ? t.getMotivoLog() : "SIN MOTIVO");
                row.createCell(8).setCellValue(t.getFechaCreacion() != null ? t.getFechaCreacion().toString() : "SIN FECHA");
            }

            // FACTURAS (con nombre del cliente)
            Sheet hojaFacturas = workbook.createSheet("Facturas");
            List<Factura> facturas = FacturaDAO.obtenerTodas();
            Row encabezadoFacturas = hojaFacturas.createRow(0);
            String[] columnasFacturas = {"ID Factura", "Fecha", "Cliente", "Total", "Método de Pago"};
            for (int i = 0; i < columnasFacturas.length; i++) {
                encabezadoFacturas.createCell(i).setCellValue(columnasFacturas[i]);
            }
            fila = 1;
            for (Factura f : facturas) {
                Row row = hojaFacturas.createRow(fila++);
                row.createCell(0).setCellValue(f.getIdFactura());
                row.createCell(1).setCellValue(f.getFechaHora() != null ? f.getFechaHora().toString() : "SIN FECHA");

                if (f.getCliente() != null && f.getCliente().getPersona() != null) {
                    Persona p = f.getCliente().getPersona();
                    String nombreCompleto = (p.getNombre() != null ? p.getNombre() : "")
                            + " "
                            + (p.getApellido() != null ? p.getApellido() : "");
                    String documento = (p.getNumeroDocumento() != null) ? p.getNumeroDocumento() : "";
                    row.createCell(2).setCellValue(nombreCompleto.trim() + " (" + documento + ")");
                } else {
                    row.createCell(2).setCellValue("SIN CLIENTE");
                }

                row.createCell(3).setCellValue(f.getMontoTotal());
                row.createCell(4).setCellValue(f.getMetodoPago() != null ? f.getMetodoPago() : "SIN MÉTODO");
            }

            // VISITAS (frecuencia por cliente y estilista)
            Sheet hojaVisitas = workbook.createSheet("Visitas");
            List<Visita> visitas = visitaDAO.obtenerTodas();
            Row encabezadoVisitas = hojaVisitas.createRow(0);
            String[] columnasVisitas = {"ID Visita", "Fecha", "Cliente", "Estilista", "ID Turno"};
            for (int i = 0; i < columnasVisitas.length; i++) {
                encabezadoVisitas.createCell(i).setCellValue(columnasVisitas[i]);
            }
            fila = 1;
            for (Visita v : visitas) {
                Row row = hojaVisitas.createRow(fila++);
                row.createCell(0).setCellValue(v.getIdVisita());
                row.createCell(1).setCellValue(v.getFechaHoraCierre() != null ? v.getFechaHoraCierre().toString() : "SIN FECHA");
                row.createCell(2).setCellValue(
                        v.getCliente() != null && v.getCliente().getPersona() != null
                                ? v.getCliente().getPersona().getNombre()
                                : "SIN CLIENTE"
                );
                row.createCell(3).setCellValue(
                        v.getEmpleado() != null && v.getEmpleado().getPersona() != null
                                ? v.getEmpleado().getPersona().getNombre()
                                : "SIN ESTILISTA"
                );
                row.createCell(4).setCellValue(v.getIdTurno());
            }

            // GUARDAR ARCHIVO
            try (FileOutputStream fos = new FileOutputStream(destino)) {
                workbook.write(fos);
                workbook.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
