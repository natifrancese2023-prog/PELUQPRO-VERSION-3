package dao;

import claseslogicas.HorarioAtencion;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HorarioAtencionDAO {

    private static final Logger log = LoggerFactory.getLogger(HorarioAtencionDAO.class);

    public HorarioAtencion obtenerHorarioPorDia(LocalDate fecha) throws SQLException {
        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        String diaSemanaSQL = mapearDiaASQL(dayOfWeek);

        String sql = "SELECT id, dia_semana, hora_apertura, hora_cierre FROM horario WHERE dia_semana = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, diaSemanaSQL);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new HorarioAtencion(
                            rs.getInt("id"),
                            rs.getString("dia_semana"),
                            rs.getTime("hora_apertura").toLocalTime(),
                            rs.getTime("hora_cierre").toLocalTime()
                    );
                }
            }

        } catch (SQLException e) {
            log.error("Error al obtener horario para día {} ({})", diaSemanaSQL, fecha, e);
            throw e;
        }
        return null;
    }

    private String mapearDiaASQL(DayOfWeek dayOfWeek) {
        switch (dayOfWeek) {
            case MONDAY: return "Lunes";
            case TUESDAY: return "Martes";
            case WEDNESDAY: return "Miercoles";
            case THURSDAY: return "Jueves";
            case FRIDAY: return "Viernes";
            case SATURDAY: return "Sabado";
            case SUNDAY: return "Domingo";
            default: return "";
        }
    }
}
