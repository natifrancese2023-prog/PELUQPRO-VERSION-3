package dao;

import claseslogicas.Cliente;
import claseslogicas.ClienteRedSocial;
import claseslogicas.Persona;
import claseslogicas.Documento;
import claseslogicas.Provincia;
import claseslogicas.Barrio;
import claseslogicas.Ciudad;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Coordinador del dominio Cliente. Ya no contiene el SQL de documento,
 * persona ni red_social directamente — eso vive ahora en DocumentoDAO,
 * PersonaDAO y RedSocialDAO respectivamente. Esta clase mantiene la
 * transacción (conexión, commit/rollback) y decide el orden de los pasos,
 * delegando cada paso al DAO especializado correspondiente.
 * <p>
 * La API pública (insertar, actualizar, eliminar, obtenerPorId,
 * consultarPorDocumentoCompleto, obtenerTodos, obtenerTiposDocumento, etc.)
 * no cambió — ni ClienteService ni ningún controller necesitan modificarse.
 * <p>
 * También se resolvió acá la mezcla de métodos static/instancia que tenía
 * la clase original: ahora todos los métodos públicos son de instancia.
 */
public class ClienteDAO {

    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    private final PersonaDAO personaDAO = new PersonaDAO();
    private final RedSocialDAO redSocialDAO = new RedSocialDAO();

    private static final String INSERT_CLIENTE_SQL = "INSERT INTO cliente (id_persona) VALUES (?)";

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
                    "WHERE td.tipo_documento = ? AND d.numero_documento = ? AND c.activo = true";

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

    public boolean insertar(Cliente cliente) throws SQLException {
        Connection conn = null;
        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            int idTipoDocumento = documentoDAO.obtenerIdTipoDocumento(conn, cliente.getNombreTipoDocumento());
            int idBarrio = personaDAO.obtenerIdBarrio(conn, cliente.getNombreBarrio());

            if (idTipoDocumento == -1 || idBarrio == -1) {
                conn.rollback();
                throw new SQLException("Error: No se encontró Tipo Documento o Barrio.");
            }

            // PASO 0: Validar duplicidad de documento
            if (documentoDAO.existeClienteConDocumento(conn, cliente.getNumeroDocumento(), idTipoDocumento)) {
                conn.rollback();
                throw new SQLException("Error: Ya existe un cliente con ese documento.");
            }

            // PASO 1: Insertar Documento
            int idDocumento = documentoDAO.insertar(conn, cliente.getNumeroDocumento(), idTipoDocumento);
            if (idDocumento == -1) { conn.rollback(); return false; }

            // PASO 2: Insertar Persona
            int idPersona = personaDAO.insertar(conn, cliente, idDocumento, idBarrio);
            if (idPersona == -1) { conn.rollback(); return false; }

            // PASO 3: Insertar Cliente
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
                int idTipoRedSocial = redSocialDAO.obtenerIdTipoRedSocial(conn, rsCliente.getNombreTipoRedSocial());
                if (idTipoRedSocial == -1) {
                    conn.rollback();
                    throw new SQLException("Error: Tipo de Red Social no encontrado.");
                }
                redSocialDAO.insertar(conn, rsCliente.getNombreUsuario(), idTipoRedSocial, idCliente);
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

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CLIENTE_POR_ID_COMPLETO)) {

            ps.setInt(1, idCliente);
            try (ResultSet rs = ps.executeQuery()) {
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

                    // 🔧 Antes acá se pasaba idPersona por error (mismo bug que
                    // ya habíamos corregido en consultarPorDocumentoCompleto,
                    // pero se había quedado sin corregir en este método).
                    ClienteRedSocial redSocial = redSocialDAO.consultarPorCliente(conn, cliente.getIdCliente());
                    cliente.setRedSocial(redSocial);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al obtener Cliente por ID: " + e.getMessage());
            throw e;
        }
        return cliente;
    }

    public Cliente consultarPorDocumentoCompleto(String tipoDocumento, String numeroDocumento) throws SQLException {
        if (tipoDocumento == null || numeroDocumento == null || tipoDocumento.isEmpty() || numeroDocumento.isEmpty()) {
            return null;
        }

        Cliente cliente = null;

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_CLIENTE_POR_DOCUMENTO_COMPLETO)) {

            ps.setString(1, tipoDocumento.trim().toUpperCase());
            ps.setString(2, numeroDocumento.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    cliente = new Cliente();
                    int idCliente = rs.getInt("id_cliente");

                    cliente.setIdCliente(idCliente);
                    cliente.setIdPersona(rs.getInt("id_persona"));
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

                    ClienteRedSocial redSocial = redSocialDAO.consultarPorCliente(conn, idCliente);
                    cliente.setRedSocial(redSocial);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error al consultar Cliente por Documento: " + e.getMessage());
            throw e;
        }
        return cliente;
    }

    public List<Cliente> obtenerTodos() {
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
                "WHERE c.activo = true " +
                "ORDER BY c.id_cliente";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setFechaAlta(rs.getDate("fecha_alta"));

                Persona persona = new Persona();
                persona.setIdPersona(rs.getInt("id_persona"));
                persona.setNombre(rs.getString("cliente_nombre"));
                persona.setApellido(rs.getString("cliente_apellido"));
                persona.setTelefono(rs.getString("cliente_telefono"));
                persona.setEmail(rs.getString("cliente_email"));
                persona.setCalle(rs.getString("cliente_calle"));
                persona.setNumero(rs.getString("cliente_numero"));

                Documento documento = new Documento();
                documento.setIdDocumento(rs.getInt("id_documento"));
                documento.setNumeroDocumento(rs.getString("numero_documento"));
                documento.setTipoDocumento(rs.getString("tipo_documento"));
                persona.setNumeroDocumento(documento.getNumeroDocumento());

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
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            int idBarrio = personaDAO.obtenerIdBarrio(conn, cliente.getNombreBarrio());
            if (idBarrio == -1) {
                conn.rollback();
                throw new SQLException("Error de Modificación: Barrio no encontrado.");
            }

            personaDAO.actualizar(conn, cliente, idBarrio);
            redSocialDAO.actualizar(conn, cliente);

            conn.commit();
            return true;

        } catch (SQLException e) {
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { System.err.println("Rollback fallido: " + ex.getMessage()); }
            System.err.println("ERROR DE TRANSACCIÓN: No se pudo actualizar el cliente. Detalle: " + e.getMessage());
            throw e;
        } finally {
            try { if (conn != null) conn.close(); } catch (SQLException e) { e.printStackTrace(); }
        }
    }

    /** Borrado lógico: marca cliente y persona como inactivos, no borra filas físicas. */
    public boolean eliminar(Cliente cliente) throws SQLException {
        Connection conn = null;
        int idCliente = cliente.getIdCliente();

        if (idCliente <= 0) return false;

        try {
            conn = ConexionBD.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement psCliente = conn.prepareStatement(
                    "UPDATE cliente SET activo = false WHERE id_cliente = ?")) {
                psCliente.setInt(1, idCliente);
                if (psCliente.executeUpdate() == 0) { conn.rollback(); return false; }
            }

            personaDAO.marcarInactiva(conn, cliente.getIdPersona());

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) conn.rollback();
            throw e;
        } finally {
            if (conn != null) conn.close();
        }
    }

    public int contarVisitasPorIdCliente(int idCliente) {
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

    public List<String> obtenerTiposDocumento() throws SQLException {
        return documentoDAO.obtenerTiposDocumento();
    }

    public List<String> obtenerTiposRedSocial() throws SQLException {
        return redSocialDAO.obtenerTiposRedSocial();
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