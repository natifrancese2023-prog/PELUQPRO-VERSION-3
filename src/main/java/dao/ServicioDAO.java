package dao;

import claseslogicas.Servicio;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ServicioDAO {

    private static final Logger log = LoggerFactory.getLogger(ServicioDAO.class);

    public List<Servicio> obtenerTodos() {
        List<Servicio> servicios = new ArrayList<>();
        String sql = "SELECT id_servicio, nombre_servicio, descripcion, duracion_minutos, precio FROM servicios";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                servicios.add(mapearServicio(rs));
            }

        } catch (SQLException e) {
            log.error("Error al obtener todos los servicios", e);
        }
        return servicios;
    }

    public List<Servicio> obtenerServiciosPorTurno(int idTurno) {
        List<Servicio> servicios = new ArrayList<>();
        String sql = "SELECT s.id_servicio, s.nombre_servicio, s.descripcion, s.duracion_minutos, s.precio " +
                "FROM servicios s JOIN turno_servicios ts ON s.id_servicio = ts.id_servicio " +
                "WHERE ts.id_turno = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idTurno);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                servicios.add(mapearServicio(rs));
            }

        } catch (SQLException e) {
            log.error("Error al obtener servicios por turno id={}", idTurno, e);
        }
        return servicios;
    }


    private Servicio mapearServicio(ResultSet rs) throws SQLException {
        return new Servicio(
                rs.getInt("id_servicio"),
                rs.getString("nombre_servicio"),
                rs.getString("descripcion"),
                rs.getInt("duracion_minutos"),
                rs.getDouble("precio")
        );
    }
}

