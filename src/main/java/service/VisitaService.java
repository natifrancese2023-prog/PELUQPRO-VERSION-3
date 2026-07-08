package service;

import claseslogicas.Cliente;
import claseslogicas.HistorialView;
import claseslogicas.ServicioTemp;
import claseslogicas.Visita;
import dao.visitaDAO;
import javafx.collections.ObservableList;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class VisitaService {

    private static final Logger log = LoggerFactory.getLogger(VisitaService.class);
    private final visitaDAO visitaDAO = new visitaDAO();

    public boolean registrarVisita(Cliente cliente, ObservableList<ServicioTemp> servicios, int idEstilista, int idTurno) {
        log.info("Registrando nueva visita para cliente={} turno={}",
                cliente != null ? cliente.getIdCliente() : null, idTurno);
        return visitaDAO.guardarNuevaVisita(cliente, servicios, idEstilista, idTurno);
    }

    public List<String> obtenerNombresServicios() {
        return visitaDAO.obtenerNombresServicios();
    }

    public List<HistorialView> obtenerHistorialPorCliente(int idCliente) {
        return visitaDAO.obtenerHistorialPorCliente(idCliente);
    }

    public Visita obtenerVisitaPorId(int idVisita) {
        return visitaDAO.obtenerVisitaPorId(idVisita);
    }

    public Visita obtenerVisitaPorTurno(int idTurno) {
        return visitaDAO.obtenerVisitaPorTurno(idTurno);
    }

    public List<Visita> obtenerTodas() {
        return visitaDAO.obtenerTodas();
    }
}
