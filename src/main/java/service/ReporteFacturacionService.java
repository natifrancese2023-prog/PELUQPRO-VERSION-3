package service;

import dao.ReporteFacturacionDAO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public class ReporteFacturacionService {

    private final ReporteFacturacionDAO reporteFacturacionDAO = new ReporteFacturacionDAO();

    public Map<LocalDate, BigDecimal> obtenerFacturacionPorDia(LocalDate inicio, LocalDate fin) {
        return reporteFacturacionDAO.obtenerFacturacionPorDia(inicio, fin);
    }

    public Map<String, Integer> obtenerUsoMetodosPago(LocalDate inicio, LocalDate fin) {
        return reporteFacturacionDAO.obtenerUsoMetodosPago(inicio, fin);
    }
}
