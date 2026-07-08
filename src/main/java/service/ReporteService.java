package service;

import claseslogicas.ClienteReporteExtendido;
import dao.ReporteDAO;

import java.sql.SQLException;
import java.util.List;

public class ReporteService {

    private final ReporteDAO reporteDAO = new ReporteDAO();

    public List<ClienteReporteExtendido> obtenerDatosClientesExtendido() throws SQLException {
        return reporteDAO.obtenerDatosClientesExtendido();
    }
}
