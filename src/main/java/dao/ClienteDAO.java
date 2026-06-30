package dao;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;
import claseslogicas.Persona;
import claseslogicas.Documento;
import claseslogicas.Provincia;
import claseslogicas.Barrio;
import claseslogicas.Ciudad;
import claseslogicas.ClienteRedSocial;


import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ClienteDAO {

    private static final String SELECT_TIPO_DOCUMENTO_ID = "SELECT id_tipo_documento FROM tipo_documento WHERE tipo_documento = ?";
    private static final String SELECT_BARRIO_ID = "SELECT id_barrio FROM barrio WHERE nombre_barrio = ?";
    private static final String SELECT_TIPOS_RED_SOCIAL_ID = "SELECT id_tipo_red_social FROM tipos_red_social WHERE tipo_red_social = ?";

    private static final String INSERT_DOCUMENTO_SQL = "INSERT INTO documento (numero_documento, id_tipo_documento) VALUES (?, ?)";
    private static final String INSERT_PERSONA_SQL = "INSERT INTO persona (nombre, apellido, telefono, email, calle, numero, id_documento, id_barrio) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_CLIENTE_SQL = "INSERT INTO cliente (id_persona) VALUES (?)";
    private static final String INSERT_RED_SOCIAL_SQL = "INSERT INTO red_social (nombre_usuario, id_tipo_red_social, id_cliente) VALUES (?, ?, ?)";

    private static final String SELECT_PROVINCIAS = "SELECT nombre_provincia FROM provincia ORDER BY nombre_provincia";
    private static final String SELECT_TIPOS_DOCUMENTO = "SELECT tipo_documento FROM tipo_documento ORDER BY tipo_documento";
    private static final String SELECT_TIPOS_RED_SOCIAL = "SELECT tipo_red_social FROM tipos_red_social ORDER BY tipo_red_social";
    private static final String SELECT_CIUDADES_POR_PROVINCIA = "SELECT c.nombre_ciudad FROM ciudad c INNER JOIN provincia p ON c.id_provincia = p.id_provincia WHERE p.nombre_provincia = ? ORDER BY c.nombre_ciudad";
    private static final String SELECT_BARRIOS_POR_CIUDAD = "SELECT b.nombre_barrio FROM barrio b INNER JOIN ciudad c ON b.id_ciudad = c.id_ciudad WHERE c.nombre_ciudad = ? ORDER BY b.nombre_barrio";

    private static final String SELECT_CLIENTE_POR_DOCUMENTO_COMPLETO =
            "SELECT c.id_cliente, p.id_persona, p.nombre, p.apellido, p.telefono, p.email, p.calle, p.numero, " +
                    "d.numero_documento, td.tipo_documento, b.nombre_barrio, ciu.nombre_ciudad, pr.nombre_provincia, " +
                    "c.fecha_alta " +
                    "FROM cliente c " +
                    "JOIN persona p ON c.id_persona = p.id_persona " +
                    "JOIN documento d ON p.id_documento = d.id_documento " +
                    "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento " +
                    "JOIN barrio b ON p.id_barrio = b.id_barrio " +
                    "JOIN ciudad ciu ON b.id_ciudad = ciu.id_ciudad " +
                    "JOIN provincia pr ON ciu.id_provincia = pr.id_provincia " +
                    "WHERE td.tipo_documento = ? AND d.numero_documento = ?";

    private static final String SELECT_CLIENTE_POR_ID_COMPLETO =
            "SELECT c.id_cliente, p.id_persona, p.nombre, p.apellido, p.telefono, p.email, p.calle, p.numero, " +
                    "d.numero_documento, td.tipo_documento, b.nombre_barrio, ciu.nombre_ciudad, pr.nombre_provincia " +
                    "FROM cliente c " +
                    "JOIN persona p ON c.id_persona = p.id_persona " +
                    "JOIN documento d ON p.id_documento = d.id_documento " +
                    "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento " +
                    "JOIN barrio b ON p.id_barrio = b.id_barrio " +
                    "JOIN ciudad ciu ON b.id_ciudad = ciu.id_ciudad " +
                    "JOIN provincia pr ON ciu.id_provincia = pr.id_provincia " +
                    "WHERE c.id_cliente = ?";

    private static final String SELECT_REDES_SOCIAL_POR_CLIENTE =
            "SELECT rs.nombre_usuario, trs.tipo_red_social " +
                    "FROM red_social rs " +
                    "JOIN tipos_red_social trs ON rs.id_tipo_red_social = trs.id_tipo_red_social " +
                    "WHERE rs.id_cliente = ?";

    private static final String SELECT_TODOS_CLIENTES_REPORTE =
            "SELECT c.id_cliente, p.id_persona, p.nombre, p.apellido, d.numero_documento, td.tipo_documento, c.fecha_alta, COUNT(v.id_visita) AS numero_visitas " +
                    "FROM cliente c " +
                    "JOIN persona p ON c.id_persona = p.id_persona " +
                    "JOIN documento d ON p.id_documento = d.id_documento " +
                    "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento " +
                    "LEFT JOIN visita v ON c.id_cliente = v.id_cliente " +
                    "GROUP BY c.id_cliente, p.id_persona, p.nombre, p.apellido, d.numero_documento, td.tipo_documento, c.fecha_alta " +
                    "ORDER BY p.apellido, p.nombre";

    private static final String UPDATE_PERSONA_SQL =
            "UPDATE persona SET nombre = ?, apellido = ?, telefono = ?, email = ?, calle = ?, numero = ?, id_barrio = ? WHERE id_persona = ?";

    private static final String UPDATE_RED_SOCIAL_SQL =
            "UPDATE red_social SET nombre_usuario = ?, id_tipo_red_social = ? WHERE id_cliente = ?";

    private static final String SELECT_RED_SOCIAL_EXISTENTE =
            "SELECT id_cliente FROM red_social WHERE id_cliente = ?";

    private static final String DELETE_DETALLE_SERVICIOS_SQL =
            "DELETE FROM detalle_servicio WHERE id_visita IN (SELECT id_visita FROM visita WHERE id_cliente = ?)";

    private static final String DELETE_VISITA_SQL =
            "DELETE FROM visita WHERE id_cliente = ?";

    private static final String DELETE_RED_SOCIAL_SQL =
            "DELETE FROM red_social WHERE id_cliente = ?";

    private static final String DELETE_CLIENTE_SQL =
            "DELETE FROM cliente WHERE id_cliente = ?";

    private static final String DELETE_PERSONA_SQL =
            "DELETE FROM persona WHERE id_persona = ?";

    private static final String SELECT_DOCUMENTO_ID_BY_PERSONA_ID =
            "SELECT id_documento FROM persona WHERE id_persona = ?";

    private static final String DELETE_DOCUMENTO_SQL =
            "DELETE FROM documento WHERE id_documento = ?";

    private static final ConexionBD conexionBD = new ConexionBD();


    public boolean insertar(Cliente cliente) throws SQLException {
        Connection conn = null;
        try {
            conn = conexionBD.getConnection();
            conn.setAutoCommit(false);

            int idTipoDocumento = obtenerIdPorNombre(conn, SELECT_TIPO_DOCUMENTO_ID, cliente.getNombreTipoDocumento());
            int idBarrio = obtenerIdPorNombre(conn, SELECT_BARRIO_ID, cliente.getNombreBarrio());

            if (idTipoDocumento == -1 || idBarrio == -1) {
                conn.rollback();
                throw new SQLException("Error: No se encontró Tipo Documento o Barrio.");
            }

            // PASO 1: Insertar Documento
            int idDocumento = -1;
            try (PreparedStatement psDoc = conn.prepareStatement(INSERT_DOCUMENTO_SQL, Statement.RETURN_GENERATED_KEYS)) {
                psDoc.setString(1, cliente.getNumeroDocumento());
                psDoc.setInt(2, idTipoDocumento);
                if (psDoc.executeUpdate() > 0) {
                    try (ResultSet rs = psDoc.getGeneratedKeys()) {
                        if (rs.next()) idDocumento = rs.getInt(1);
                    }
                }
            }
            if (idDocumento == -1) { conn.rollback(); return false; }

            // PASO 2: Insertar Persona
            int idPersona = -1;
            try (PreparedStatement psPer = conn.prepareStatement(INSERT_PERSONA_SQL, Statement.RETURN_GENERATED_KEYS)) {
                psPer.setString(1, cliente.getNombre());
                psPer.setString(2, cliente.getApellido());
                psPer.setString(3, cliente.getTelefono());
                psPer.setString(4, cliente.getEmail());
                psPer.setString(5, cliente.getCalle());
                psPer.setString(6, cliente.getNumero());
                psPer.setInt(7, idDocumento);
                psPer.setInt(8, idBarrio);
                if (psPer.executeUpdate() > 0) {
                    try (ResultSet rs = psPer.getGeneratedKeys()) {
                        if (rs.next()) idPersona = rs.getInt(1);
                    }
                }
            }
            if (idPersona == -1) { conn.rollback(); return false; }

            // PASO 3: Insertar Cliente y obtener ID generado
            int idCliente = -1;
            try (PreparedStatement psCli = conn.prepareStatement(INSERT_CLIENTE_SQL, Statement.RETURN_GENERATED_KEYS)) {
                psCli.setInt(1, idPersona);
                if (psCli.executeUpdate() > 0) {
                    try (ResultSet rs = psCli.getGeneratedKeys()) {
                        if (rs.next()) idCliente = rs.getInt(1);
                    }
                } else {
                    conn.rollback(); return false;
                }
            }
            if (idCliente == -1) { conn.rollback(); return false; }

            cliente.setIdPersona(idPersona);
            cliente.setIdCliente(idCliente);

            // PASO 4: Insertar Red Social (si existe)
            ClienteRedSocial rsCliente = cliente.getRedSocial();
            if (rsCliente != null && rsCliente.getNombreUsuario() != null && !rsCliente.getNombreUsuario().isEmpty()) {
                int idTipoRedSocial = obtenerIdPorNombre(conn, SELECT_TIPOS_RED_SOCIAL_ID, rsCliente.getNombreTipoRedSocial());
                if (idTipoRedSocial == -1) {
                    conn.rollback();
                    throw new SQLException("Error: Tipo de Red Social no encontrado.");
                }

                try (PreparedStatement psRed = conn.prepareStatement(INSERT_RED_SOCIAL_SQL)) {
                    psRed.setString(1, rsCliente.getNombreUsuario());
                    psRed.setInt(2, idTipoRedSocial);
                    psRed.setInt(3, idCliente); // ahora sí, ID correcto
                    if (psRed.executeUpdate() == 0) {
                        conn.rollback();
                        return false;
                    }
                }
            }

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback fallido: " + ex.getMessage()); }
            System.err.println("❌ ERROR DE TRANSACCIÓN: No se pudo insertar el cliente. Detalle: " + e.getMessage());
            throw e;

        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }




    public Cliente obtenerPorId(int idCliente) throws SQLException {
        Cliente cliente = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexionBD.getConnection();
            ps = conn.prepareStatement(SELECT_CLIENTE_POR_ID_COMPLETO);
            ps.setInt(1, idCliente);
            rs = ps.executeQuery();

            if (rs.next()) {
                cliente = new Cliente();
                int idPersona = rs.getInt("id_persona");

                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setIdPersona(idPersona);
                cliente.setNombre(rs.getString("nombre"));
                cliente.setApellido(rs.getString("apellido"));
                cliente.setTelefono(rs.getString("telefono"));
                cliente.setEmail(rs.getString("email"));
                cliente.setCalle(rs.getString("calle"));
                cliente.setNumero(rs.getString("numero"));
                cliente.setNumeroDocumento(rs.getString("numero_documento"));
                cliente.setNombreTipoDocumento(rs.getString("tipo_documento"));
                cliente.setNombreProvincia(rs.getString("nombre_provincia"));
                cliente.setNombreCiudad(rs.getString("nombre_ciudad"));
                cliente.setNombreBarrio(rs.getString("nombre_barrio"));

                ClienteRedSocial redSocial = consultarRedSocialPorCliente(conn, idPersona);
                cliente.setRedSocial(redSocial);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener Cliente por ID: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return cliente;
    }

    public Cliente consultarPorDocumentoCompleto(String tipoDocumento, String numeroDocumento) throws SQLException {
        Cliente cliente = null;
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        if (tipoDocumento == null || numeroDocumento == null || tipoDocumento.isEmpty() || numeroDocumento.isEmpty()) {
            return null;
        }

        try {
            conn = conexionBD.getConnection();
            ps = conn.prepareStatement(SELECT_CLIENTE_POR_DOCUMENTO_COMPLETO);
            ps.setString(1, tipoDocumento.trim().toUpperCase());
            ps.setString(2, numeroDocumento.trim());
            rs = ps.executeQuery();

            if (rs.next()) {
                cliente = new Cliente();
                int idPersona = rs.getInt("id_persona");

                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setIdPersona(idPersona);
                cliente.setNombre(rs.getString("nombre"));
                cliente.setApellido(rs.getString("apellido"));
                cliente.setTelefono(rs.getString("telefono"));
                cliente.setEmail(rs.getString("email"));
                cliente.setCalle(rs.getString("calle"));
                cliente.setNumero(rs.getString("numero"));
                cliente.setNumeroDocumento(rs.getString("numero_documento"));
                cliente.setNombreTipoDocumento(rs.getString("tipo_documento"));
                cliente.setNombreProvincia(rs.getString("nombre_provincia"));
                cliente.setNombreCiudad(rs.getString("nombre_ciudad"));
                cliente.setNombreBarrio(rs.getString("nombre_barrio"));
                cliente.setFechaAlta(rs.getDate("fecha_alta"));


                ClienteRedSocial redSocial = consultarRedSocialPorCliente(conn, idPersona);
                cliente.setRedSocial(redSocial);
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar Cliente por Documento: " + e.getMessage());
            e.printStackTrace();
            throw e;
        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return cliente;
    }
    public static List<Cliente> obtenerTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT c.id_cliente, c.fecha_alta, p.id_persona, p.nombre AS cliente_nombre, " +
                "p.apellido AS cliente_apellido, p.telefono AS cliente_telefono, p.email AS cliente_email, " +
                "p.calle AS cliente_calle, p.numero AS cliente_numero, d.id_documento, d.numero_documento, " +
                "td.tipo_documento, b.id_barrio, b.nombre_barrio, ci.id_ciudad, ci.nombre_ciudad, " +
                "pr.id_provincia, pr.nombre_provincia, rs.id_red_social, rs.nombre_usuario AS red_social_usuario, " +
                "rs.id_tipo_red_social " +
                "FROM cliente c " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "JOIN tipo_documento td ON d.id_tipo_documento = td.id_tipo_documento " +
                "JOIN barrio b ON p.id_barrio = b.id_barrio " +
                "JOIN ciudad ci ON b.id_ciudad = ci.id_ciudad " +
                "JOIN provincia pr ON ci.id_provincia = pr.id_provincia " +
                "LEFT JOIN red_social rs ON c.id_cliente = rs.id_cliente " +
                "ORDER BY c.id_cliente";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));

                // Manejo seguro de fecha_alta

                Date fechaSql = rs.getDate("fecha_alta");
                cliente.setFechaAlta(fechaSql);

                // Persona
                Persona persona = new Persona();
                persona.setIdPersona(rs.getInt("id_persona"));
                persona.setNombre(rs.getString("cliente_nombre"));
                persona.setApellido(rs.getString("cliente_apellido"));
                persona.setTelefono(rs.getString("cliente_telefono"));
                persona.setEmail(rs.getString("cliente_email"));
                persona.setCalle(rs.getString("cliente_calle"));
                persona.setNumero(rs.getString("cliente_numero"));

                // Documento
                Documento documento = new Documento();
                documento.setIdDocumento(rs.getInt("id_documento"));
                documento.setNumeroDocumento(rs.getString("numero_documento"));
                documento.setTipoDocumento(rs.getString("tipo_documento"));
                persona.setNumeroDocumento(documento.getNumeroDocumento());

                // Barrio / Ciudad / Provincia
                Barrio barrio = new Barrio();
                barrio.setIdBarrio(rs.getInt("id_barrio"));
                barrio.setNombreBarrio(rs.getString("nombre_barrio"));

                Ciudad ciudad = new Ciudad();
                ciudad.setIdCiudad(rs.getInt("id_ciudad"));
                ciudad.setNombreCiudad(rs.getString("nombre_ciudad"));

                Provincia provincia = new Provincia();
                provincia.setIdProvincia(rs.getInt("id_provincia"));
                provincia.setNombreProvincia(rs.getString("nombre_provincia"));

                ciudad.setProvincia(provincia);
                barrio.setCiudad(ciudad);
                persona.setNombreBarrio(String.valueOf(barrio));

                cliente.setPersona(persona);

                // Red social (puede ser null)
                int idRedSocial = rs.getInt("id_red_social");
                if (idRedSocial != 0) {
                    ClienteRedSocial redSocial = new ClienteRedSocial();
                    redSocial.setIdTipoRedSocial(idRedSocial);
                    redSocial.setNombreUsuario(rs.getString("red_social_usuario"));
                    redSocial.setIdTipoRedSocial(rs.getInt("id_tipo_red_social"));
                    cliente.setRedSocial(redSocial);
                }

                lista.add(cliente);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return lista;
    }




    public boolean actualizar(Cliente cliente) throws SQLException {
        Connection conn = null;
        if (cliente == null || cliente.getIdPersona() <= 0) return false;

        try {
            conn = conexionBD.getConnection();
            conn.setAutoCommit(false);

            int idBarrio = obtenerIdPorNombre(conn, SELECT_BARRIO_ID, cliente.getNombreBarrio());
            if (idBarrio == -1) {
                conn.rollback();
                throw new SQLException("Error de Modificación: Barrio no encontrado.");
            }

            try (PreparedStatement psPersona = conn.prepareStatement(UPDATE_PERSONA_SQL)) {
                psPersona.setString(1, cliente.getNombre());
                psPersona.setString(2, cliente.getApellido());
                psPersona.setString(3, cliente.getTelefono());
                psPersona.setString(4, cliente.getEmail());
                psPersona.setString(5, cliente.getCalle());
                psPersona.setString(6, cliente.getNumero());
                psPersona.setInt(7, idBarrio);
                psPersona.setInt(8, cliente.getIdPersona());

                psPersona.executeUpdate();
            }

            actualizarRedSocial(conn, cliente);

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback fallido: " + ex.getMessage()); }
            System.err.println("ERROR DE TRANSACCIÓN: No se pudo actualizar el cliente. Detalle: " + e.getMessage());
            e.printStackTrace();
            throw e;

        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }



    public boolean eliminar(Cliente cliente) throws SQLException {
        Connection conn = null;
        // IMPORTANTE: Asegurarnos de tener ambos IDs
        int idCliente = cliente.getIdCliente();
        int idPersona = cliente.getIdPersona();

        if (idCliente <= 0 || idPersona <= 0) return false;

        try {
            conn = conexionBD.getConnection();
            conn.setAutoCommit(false);

            // PASO 1: Eliminar registros de servicios y visitas usando ID_CLIENTE
            // (No idPersona, porque la FK en visita es id_cliente)
            try (PreparedStatement psVisitas = conn.prepareStatement("DELETE FROM visita WHERE id_cliente = ?")) {
                psVisitas.setInt(1, idCliente);
                psVisitas.executeUpdate();
            }

            // PASO 2: Eliminar Red Social usando ID_CLIENTE
            try (PreparedStatement psRed = conn.prepareStatement(DELETE_RED_SOCIAL_SQL)) {
                psRed.setInt(1, idCliente);
                psRed.executeUpdate();
            }

            // PASO 3: Eliminar de CLIENTE usando ID_CLIENTE
            try (PreparedStatement psCliente = conn.prepareStatement("DELETE FROM cliente WHERE id_cliente = ?")) {
                psCliente.setInt(1, idCliente);
                if (psCliente.executeUpdate() == 0) { conn.rollback(); return false; }
            }

            // PASO 4: Eliminar de PERSONA usando ID_PERSONA
            try (PreparedStatement psPersona = conn.prepareStatement("DELETE FROM persona WHERE id_persona = ?")) {
                psPersona.setInt(1, idPersona);
                psPersona.executeUpdate();
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }



    private int obtenerIdPorNombre(Connection conn, String query, String nombre) throws SQLException {
        if (nombre == null || nombre.isEmpty()) return -1;
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return -1;
    }

    private List<String> obtenerListaGenerica(String query, String parametro) throws SQLException {
        List<String> lista = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {
            conn = conexionBD.getConnection();
            ps = conn.prepareStatement(query);

            if (parametro != null) {
                ps.setString(1, parametro);
            }

            rs = ps.executeQuery();

            while (rs.next()) {
                lista.add(rs.getString(1));
            }

        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return lista;
    }

    private void actualizarRedSocial(Connection conn, Cliente cliente) throws SQLException {
        ClienteRedSocial rsCliente = cliente.getRedSocial();
        int idCliente = cliente.getIdPersona();

        if (rsCliente == null || rsCliente.getNombreUsuario() == null || rsCliente.getNombreUsuario().isEmpty()) {
            try (PreparedStatement psDelete = conn.prepareStatement(DELETE_RED_SOCIAL_SQL)) {
                psDelete.setInt(1, idCliente);
                psDelete.executeUpdate();
            }
            return;
        }

        int idTipoRedSocial = obtenerIdPorNombre(conn, SELECT_TIPOS_RED_SOCIAL_ID, rsCliente.getNombreTipoRedSocial());
        if (idTipoRedSocial == -1) {
            throw new SQLException("Error: Tipo de Red Social '" + rsCliente.getNombreTipoRedSocial() + "' no encontrado.");
        }

        boolean existe = consultarRedSocialExistente(conn, idCliente);

        if (existe) {
            try (PreparedStatement psUpdate = conn.prepareStatement(UPDATE_RED_SOCIAL_SQL)) {
                psUpdate.setString(1, rsCliente.getNombreUsuario());
                psUpdate.setInt(2, idTipoRedSocial);
                psUpdate.setInt(3, idCliente);
                psUpdate.executeUpdate();
            }
        } else {
            try (PreparedStatement psInsert = conn.prepareStatement(INSERT_RED_SOCIAL_SQL)) {
                psInsert.setString(1, rsCliente.getNombreUsuario());
                psInsert.setInt(2, idTipoRedSocial);
                psInsert.setInt(3, idCliente);
                psInsert.executeUpdate();
            }
        }
    }

    private ClienteRedSocial consultarRedSocialPorCliente(Connection conn, int idCliente) throws SQLException {
        ClienteRedSocial redSocial = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        ps = conn.prepareStatement(SELECT_REDES_SOCIAL_POR_CLIENTE);
        ps.setInt(1, idCliente);
        rs = ps.executeQuery();

        if (rs.next()) {
            redSocial = new ClienteRedSocial();
            redSocial.setNombreUsuario(rs.getString("nombre_usuario"));
            redSocial.setNombreTipoRedSocial(rs.getString("tipo_red_social"));
        }

        if (rs != null) rs.close();
        if (ps != null) ps.close();
        return redSocial;
    }

    private boolean consultarRedSocialExistente(Connection conn, int idCliente) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(SELECT_RED_SOCIAL_EXISTENTE)) {
            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public List<String> obtenerTiposDocumento() throws java.sql.SQLException {
        List<String> tipos = new ArrayList<>();
        Connection conn = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        String sql = SELECT_TIPOS_DOCUMENTO;

        try {
            conn = conexionBD.getConnection();
            ps = conn.prepareStatement(sql);
            rs = ps.executeQuery();

            while (rs.next()) {
                tipos.add(rs.getString("tipo_documento"));
            }

        } finally {
            try {
                if (rs != null) rs.close();
                if (ps != null) ps.close();
                if (conn != null) conn.close();
            } catch (SQLException e) {
                System.err.println("Error al cerrar recursos: " + e.getMessage());
            }
        }
        return tipos;
    }


    public List<String> obtenerTiposRedSocial() throws SQLException { return obtenerListaGenerica(SELECT_TIPOS_RED_SOCIAL, null); }
    public List<String> obtenerProvincias() throws SQLException { return obtenerListaGenerica(SELECT_PROVINCIAS, null); }
    public List<String> obtenerCiudadesPorProvincia(String nombreProvincia) throws SQLException { return obtenerListaGenerica(SELECT_CIUDADES_POR_PROVINCIA, nombreProvincia); }
    public List<String> obtenerBarriosPorCiudad(String nombreCiudad) throws SQLException { return obtenerListaGenerica(SELECT_BARRIOS_POR_CIUDAD, nombreCiudad); }
    public int contarVisitasPorDocumento(String documento) {
        int cantidad = 0;
        String sql = "SELECT COUNT(v.id_visita) AS total " +
                "FROM visita v " +
                "JOIN cliente c ON v.id_cliente = c.id_cliente " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "WHERE d.numero_documento = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, documento);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cantidad = rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al contar visitas por documento: " + e.getMessage());
        }

        return cantidad;
    }

    public static int contarVisitasPorIdCliente(int idCliente) {
        int cantidad = 0;
        String sql = "SELECT COUNT(*) AS total FROM visita WHERE id_cliente = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, idCliente);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cantidad = rs.getInt("total");
                }
            }

        } catch (SQLException e) {
            System.err.println("❌ Error al contar visitas por cliente: " + e.getMessage());
        }

        return cantidad;
    }



}