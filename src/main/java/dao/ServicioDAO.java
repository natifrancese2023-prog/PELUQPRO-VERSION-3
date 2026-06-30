package dao;

import claseslogicas.Servicio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServicioDAO {

    private static final ConexionBD conexionBD = new ConexionBD();

    public static List<Servicio> obtenerTodos() throws SQLException {
        List<Servicio> servicios = new ArrayList<>();
        String sql = "SELECT id_tipo_servicio, nombre_servicio, descripcion, duracion_minutos, precio FROM servicios";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                Servicio s = new Servicio(
                        rs.getInt("id_tipo_servicio"),
                        rs.getString("nombre_servicio"),
                        rs.getString("descripcion"),
                        rs.getInt("duracion_minutos"),
                        rs.getDouble("precio")
                );
                servicios.add(s);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener todos los servicios: " + e.getMessage());
            throw e;
        }
        return servicios;
    }

    public List<Servicio> obtenerServiciosPorTurno(int idTurno) throws SQLException {
        List<Servicio> servicios = new ArrayList<>();

        String sql = "SELECT s.id_tipo_servicio, s.nombre_servicio, s.descripcion, s.duracion_minutos, s.precio " +
                "FROM servicios s JOIN turno_servicios ts ON s.id_tipo_servicio = ts.id_tipo_servicio " +
                "WHERE ts.id_turno = ?";

        try (Connection conn = conexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTurno);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Servicio s = new Servicio(
                        rs.getInt("id_tipo_servicio"),
                        rs.getString("nombre_servicio"),
                        rs.getString("descripcion"),
                        rs.getInt("duracion_minutos"),
                        rs.getDouble("precio")
                );
                servicios.add(s);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener servicios por turno: " + e.getMessage());
            throw e;
        }
        return servicios;
    }
}