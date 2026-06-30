package dao;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

// Importación de la clase de modelo, que está en 'claseslogicas'
import claseslogicas.Usuario;

public class UsarioDAOTest {

    // Instancia de la clase a probar
    private static UsuarioDao usuarioDao;

    // --- DATOS DE PRUEBA ESTATICOS ---
    // AJUSTA ESTOS VALORES para un usuario que SÍ exista en tu base de datos de XAMPP
    private static final String USUARIO_VALIDO = "admin";
    private static final String CONTRASENA_VALIDA = "1234";

    private static final String USUARIO_INVALIDO = "usuario_fake_123";

    /**
     * Configuración: Se ejecuta una sola vez antes de que comiencen todos los tests.
     * Inicializa la instancia del DAO.
     */
    @BeforeAll
    static void setUp() {
        // Inicialización del DAO
        usuarioDao = new UsuarioDao();
    }

    /**
     * Prueba 1: Caso Exitoso (Credenciales Correctas)
     * Espera: Un objeto Usuario no nulo y con el nombre de usuario correcto.
     */
    @Test
    void testValidarUsuario_CredencialesValidas() {
        System.out.println("Ejecutando: testValidarUsuario_CredencialesValidas");

        Usuario resultado = usuarioDao.validarUsuario(USUARIO_VALIDO, CONTRASENA_VALIDA);

        // 1. Verifica que el resultado NO es nulo (el usuario fue encontrado)
        assertNotNull(resultado, "El método debe retornar un objeto Usuario para credenciales válidas.");

        // 2. Verifica que los datos del usuario coinciden
        assertEquals(USUARIO_VALIDO, resultado.getUsuario(), "El nombre de usuario devuelto debe coincidir con el consultado.");
        // Opcional: Asegúrate de que la ID es mayor a 0 si la ID es un campo de DB
        assertTrue(resultado.getId() > 0, "El ID del usuario debe ser mayor a 0.");
    }

    /**
     * Prueba 2: Caso Fallido (Credenciales Inexistentes)
     * Espera: Un resultado nulo (no se encontró el usuario).
     */
    @Test
    void testValidarUsuario_UsuarioInexistente() {
        System.out.println("Ejecutando: testValidarUsuario_UsuarioInexistente");

        Usuario resultado = usuarioDao.validarUsuario(USUARIO_INVALIDO, CONTRASENA_VALIDA);

        // Verifica que el resultado es nulo
        assertNull(resultado, "El método debe retornar null cuando el usuario no existe en la DB.");
    }

    /**
     * Prueba 3: Caso Fallido (Contraseña Incorrecta)
     * Espera: Un resultado nulo (no se encontró el registro con esa combinación).
     */
    @Test
    void testValidarUsuario_ContrasenaIncorrecta() {
        System.out.println("Ejecutando: testValidarUsuario_ContrasenaIncorrecta");

        Usuario resultado = usuarioDao.validarUsuario(USUARIO_VALIDO, "contrasena_erronea_y_larga");

        // Verifica que el resultado es nulo, ya que la contraseña es incorrecta
        assertNull(resultado, "El método debe retornar null cuando la contraseña es incorrecta para el usuario válido.");
    }

    /**
     * Limpieza (Opcional): Se ejecuta una sola vez al final.
     */
    @AfterAll
    static void tearDown() {
        // Aquí puedes realizar limpieza si tus tests modifican la base de datos (por ejemplo, eliminando registros temporales).
    }
}