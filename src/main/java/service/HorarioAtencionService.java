package service;

import claseslogicas.HorarioAtencion;
import dao.HorarioAtencionDAO;

import java.sql.SQLException;
import java.time.LocalDate;

public class HorarioAtencionService {

    private final HorarioAtencionDAO horarioDAO = new HorarioAtencionDAO();

    public HorarioAtencion obtenerHorarioPorDia(LocalDate fecha) throws SQLException {
        return horarioDAO.obtenerHorarioPorDia(fecha);
    }
}
