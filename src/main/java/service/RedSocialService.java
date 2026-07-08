package service;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;
import dao.RedSocialDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class RedSocialService {

    private final RedSocialDAO redSocialDAO = new RedSocialDAO();

    public void actualizarRedSocial(Connection conn, Cliente cliente) throws SQLException {
        redSocialDAO.actualizar(conn, cliente);
    }

    public ClienteRedSocial consultarPorCliente(Connection conn, int idCliente) throws SQLException {
        return redSocialDAO.consultarPorCliente(conn, idCliente);
    }

    public List<String> obtenerTiposRedSocial() throws SQLException {
        return redSocialDAO.obtenerTiposRedSocial();
    }
}
