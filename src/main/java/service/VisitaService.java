package service;

import claseslogicas.Cliente;
import claseslogicas.HistorialView;
import claseslogicas.ServicioTemp;
import claseslogicas.Visita;
import dao.visitaDAO;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Punto único de acceso al dominio Visita.
 * <p>
 * A diferencia de ClienteService/TurnoService/FacturaService, acá no había
 * grandes reglas de negocio sueltas en los controllers para extraer — el
 * método más importante (guardarNuevaVisita) ya vivía bien encapsulado y
 * transaccional dentro de visitaDAO. El valor principal de este service es
 * arquitectónico: que ningún controller instancie visitaDAO directo, mismo
 * criterio que el resto de los dominios ya migrados.
 * <p>
 * Nota: dentro de {@code visitaDAO.guardarNuevaVisita()} el turno se marca
 * como FINALIZADO en la MISMA transacción que la visita (no se puede separar
 * eso a una llamada aparte a TurnoService sin romper la atomicidad
 * visita+turno) — por eso esta clase no expone un método separado para eso;
 * queda encapsulado adentro del guardado de la visita.
 */
public class VisitaService {

    private final visitaDAO visitaDAO = new visitaDAO();

    public boolean registrarVisita(Cliente cliente, ObservableList<ServicioTemp> servicios, int idEstilista, int idTurno) {
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
        return dao.visitaDAO.obtenerTodas();
    }
}