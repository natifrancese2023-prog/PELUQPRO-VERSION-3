package service;

import claseslogicas.Cliente;
import dao.ClienteDAO;

import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Reglas de negocio del dominio Cliente: validación de formato de datos y
 * coordinación de alta/duplicado.
 * <p>
 * Antes, estas reglas vivían mezcladas con código de UI (JavaFX) directo en
 * los controllers:
 * <ul>
 *   <li>{@code AltaClienteController} validaba email/teléfono/documento con
 *       regex, y chequeaba duplicado antes de insertar.</li>
 *   <li>{@code ModificarClienteController} NO aplicaba las mismas regex al
 *       editar — un cliente podía tener un email válido al darlo de alta y
 *       después quedar con cualquier texto en ese campo al modificarlo, sin
 *       que nada lo detectara. Esa inconsistencia se cierra acá: ambos
 *       controllers ahora llaman a los mismos validadores.</li>
 * </ul>
 * Los controllers siguen siendo responsables de leer los campos de la UI y
 * mostrar los mensajes de error (con AlertaUtil); este service no conoce
 * JavaFX en absoluto, así que también se puede testear sin levantar una UI.
 */
public class ClienteService {

    private final ClienteDAO clienteDAO = new ClienteDAO();

    private static final Pattern EMAIL_REGEX = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    private static final Pattern TELEFONO_REGEX = Pattern.compile("^[0-9]{7,15}$");
    private static final Pattern DOCUMENTO_REGEX = Pattern.compile("^[0-9]{6,12}$");

    public enum ResultadoAlta { OK, DUPLICADO, ERROR_INSERCION }

    /** @return null si el email es válido, o el mensaje de error a mostrar. */
    public String validarEmail(String email) {
        return (email != null && EMAIL_REGEX.matcher(email).matches())
                ? null
                : "Ingrese un email válido (ejemplo: usuario@dominio.com).";
    }

    /** @return null si el teléfono es válido, o el mensaje de error a mostrar. */
    public String validarTelefono(String telefono) {
        return (telefono != null && TELEFONO_REGEX.matcher(telefono).matches())
                ? null
                : "El teléfono debe contener solo dígitos y tener entre 7 y 15 caracteres.";
    }

    /** @return null si el documento es válido, o el mensaje de error a mostrar. */
    public String validarDocumento(String numeroDocumento) {
        return (numeroDocumento != null && DOCUMENTO_REGEX.matcher(numeroDocumento).matches())
                ? null
                : "El documento debe contener solo dígitos y tener entre 6 y 12 caracteres.";
    }

    /**
     * Da de alta un cliente nuevo, verificando primero que no exista ya un
     * cliente activo con el mismo tipo+número de documento.
     * <p>
     * Nota: el chequeo de duplicado y el insert son dos llamadas separadas
     * a la base (no una transacción atómica) — en teoría dos altas
     * simultáneas con el mismo documento podrían "pisarse" en esa ventana.
     * Es un riesgo bajo dado el uso real de la app (alta manual, no
     * concurrente), pero queda documentado para quien retome esto.
     */
    public ResultadoAlta registrarCliente(Cliente cliente) throws SQLException {
        Cliente existente = clienteDAO.consultarPorDocumentoCompleto(
                cliente.getNombreTipoDocumento(), cliente.getNumeroDocumento());

        if (existente != null) {
            return ResultadoAlta.DUPLICADO;
        }

        boolean insertado = clienteDAO.insertar(cliente);
        return insertado ? ResultadoAlta.OK : ResultadoAlta.ERROR_INSERCION;
    }

    public boolean actualizarCliente(Cliente cliente) throws SQLException {
        return clienteDAO.actualizar(cliente);
    }

    public boolean eliminarCliente(Cliente cliente) throws SQLException {
        return clienteDAO.eliminar(cliente);
    }

    /**
     * Búsqueda de cliente por tipo+número de documento. Antes esta misma
     * consulta se llamaba directo desde 3 controllers distintos
     * (ConsultaClienteController, AltaTurnoController, FichaClienteController),
     * cada uno con su propia instancia de ClienteDAO.
     */
    public Cliente buscarPorDocumento(String tipoDocumento, String numeroDocumento) throws SQLException {
        return clienteDAO.consultarPorDocumentoCompleto(tipoDocumento, numeroDocumento);
    }

    /** Resuelve un cliente por id — usado para vincular un cliente ya existente a una visita/factura. */
    public Cliente obtenerPorId(int idCliente) throws SQLException {
        return clienteDAO.obtenerPorId(idCliente);
    }

    public java.util.List<Cliente> obtenerTodos() {
        return clienteDAO.obtenerTodos();
    }

    public int contarVisitasPorIdCliente(int idCliente) {
        return clienteDAO.contarVisitasPorIdCliente(idCliente);
    }
}