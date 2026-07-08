package service;

import claseslogicas.MetodoPago;
import dao.MetodoPagoDAO;

import java.sql.SQLException;

public class MetodoPagoService {

    private final MetodoPagoDAO metodoPagoDAO = new MetodoPagoDAO();

    public MetodoPago obtenerPorNombre(String nombreMetodo) throws SQLException {
        return metodoPagoDAO.obtenerPorNombre(nombreMetodo);
    }
}
