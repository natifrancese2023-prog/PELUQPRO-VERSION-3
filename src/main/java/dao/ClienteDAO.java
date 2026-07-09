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


public class ClienteDAO {

    private final DocumentoDAO documentoDAO = new DocumentoDAO();
    private final PersonaDAO personaDAO = new PersonaDAO();
    private final RedSocialDAO redSocialDAO = new RedSocialDAO();

    private static final String INSERT_CLIENTE_SQL = "INSERT INTO cliente (id_persona) VALUES (?)";

    private static final String SELECT_CLIENTE_POR_DOCUMENTO_COMPLETO =
            "SELECT c.id_cliente, p.id_persona, p.nombre, p.apellido, p.telefono, p.email, p.calle, p.numero, " +
                    "d.numero_documento, td.tipo_documento, b.nombre_barrio, ciu.nombre_ciudad, pr.nombre_provincia, " +
                    "c.fecha_alta, c.activo " +   // ✅ agregado
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
            Cliente existente = consultarPorDocumentoCompleto(cliente.getNombreTipoDocumento(), cliente.getNumeroDocumento());
            if (existente != null) {
                if (!existente.isActivo()) {
                    // No reactivar acá, solo avisar
                    conn.rollback();
                    return false; // El Service lo interpreta como DUPLICADO_INACTIVO
                } else {
                    conn.rollback();
                    throw new SQLException("Error: Ya existe un cliente activo con ese documento.");
                }
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
                    cliente.setActivo(rs.getBoolean("activo"));


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
    public void reactivarCliente(int idCliente) throws SQLException {
        String sql = "UPDATE cliente SET activo = true WHERE id_cliente = ?";
        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCliente);
            ps.executeUpdate();
        }
    }

    public List<Cliente> obtenerTodos() {
        List<Cliente> lista = new ArrayList<>();
        String sql = "SELECT c.id_cliente, c.fecha_alta, c.activo, " +
                "p.id_persona, p.nombre AS cliente_nombre, p.apellido AS cliente_apellido, " +
                "d.id_documento, d.numero_documento " +
                "FROM cliente c " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "ORDER BY c.id_cliente";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                Cliente cliente = new Cliente();
                cliente.setIdCliente(rs.getInt("id_cliente"));
                cliente.setFechaAlta(rs.getDate("fecha_alta"));
                cliente.setActivo(rs.getBoolean("activo"));

                Persona persona = new Persona();
                persona.setIdPersona(rs.getInt("id_persona"));
                persona.setNombre(rs.getString("cliente_nombre"));
                persona.setApellido(rs.getString("cliente_apellido"));
                persona.setNumeroDocumento(rs.getString("numero_documento"));

                cliente.setPersona(persona);
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
    public List<Cliente> obtenerPorEstado(boolean activo) throws SQLException {
        List<Cliente> clientes = new ArrayList<>();
        String sql = "SELECT c.id_cliente, p.nombre, p.apellido, d.numero_documento, c.fecha_alta, c.activo " +
                "FROM cliente c " +
                "JOIN persona p ON c.id_persona = p.id_persona " +
                "JOIN documento d ON p.id_documento = d.id_documento " +
                "WHERE c.activo = ?";

        try (Connection conn = ConexionBD.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, activo);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Cliente cliente = new Cliente();
                    cliente.setIdCliente(rs.getInt("id_cliente"));
                    cliente.setFechaAlta(rs.getDate("fecha_alta"));
                    cliente.setActivo(rs.getBoolean("activo"));

                    // Persona embebida
                    claseslogicas.Persona persona = new claseslogicas.Persona();
                    persona.setNombre(rs.getString("nombre"));
                    persona.setApellido(rs.getString("apellido"));
                    persona.setNumeroDocumento(rs.getString("numero_documento"));
                    cliente.setPersona(persona);

                    clientes.add(cliente);
                }
            }
        }
        return clientes;
    }

}