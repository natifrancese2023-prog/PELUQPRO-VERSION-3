package service;

import claseslogicas.Cliente;
import dao.PersonaDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

public class PersonaService {

    private final PersonaDAO personaDAO = new PersonaDAO();

    public int insertarPersona(Connection conn, Cliente cliente, int idDocumento, int idBarrio) throws SQLException {
        return personaDAO.insertar(conn, cliente, idDocumento, idBarrio);
    }

    public void actualizarPersona(Connection conn, Cliente cliente, int idBarrio) throws SQLException {
        personaDAO.actualizar(conn, cliente, idBarrio);
    }

    public void marcarPersonaInactiva(Connection conn, int idPersona) throws SQLException {
        personaDAO.marcarInactiva(conn, idPersona);
    }

    public List<String> obtenerProvincias() throws SQLException {
        return personaDAO.obtenerProvincias();
    }

    public List<String> obtenerCiudadesPorProvincia(String nombreProvincia) throws SQLException {
        return personaDAO.obtenerCiudadesPorProvincia(nombreProvincia);
    }

    public List<String> obtenerBarriosPorCiudad(String nombreCiudad) throws SQLException {
        return personaDAO.obtenerBarriosPorCiudad(nombreCiudad);
    }
}
