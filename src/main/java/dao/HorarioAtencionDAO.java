package dao;

import claseslogicas.HorarioAtencion;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.sql.Time;
import java.time.LocalTime;

public class HorarioAtencionDAO {

    private final ConexionBD conexionBD = new ConexionBD();

    public HorarioAtencion obtenerHorarioPorDia(LocalDate fecha) throws SQLException {

        DayOfWeek dayOfWeek = fecha.getDayOfWeek();
        String diaSemanaSQL = mapearDiaASQL(dayOfWeek);


        String sql = "SELECT id, dia_semana, hora_apertura, hora_cierre FROM horario WHERE dia_semana = ?";

        HorarioAtencion horario = null;

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, diaSemanaSQL);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                horario = new HorarioAtencion(
                        rs.getInt("id"),
                        rs.getString("dia_semana"),
                        // Conversión de java.sql.Time a java.time.LocalTime
                        rs.getTime("hora_apertura").toLocalTime(),
                        rs.getTime("hora_cierre").toLocalTime()
                );
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener horario por día: " + e.getMessage());
            throw e;
        }
        return horario;
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