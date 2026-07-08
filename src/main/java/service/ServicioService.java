package service;

import claseslogicas.Servicio;
import dao.ServicioDAO;

import java.util.List;

public class ServicioService {

    private final ServicioDAO servicioDAO = new ServicioDAO();

    public List<Servicio> listarServicios() {
        return servicioDAO.obtenerTodos();
    }

    public List<Servicio> listarServiciosPorTurno(int idTurno) {
        return servicioDAO.obtenerServiciosPorTurno(idTurno);
    }
}
