package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    // ⚠️ Las credenciales reales se leen de variables de entorno (NUNCA se hardcodea
    // la contraseña en el código fuente / git). Los valores de la derecha de "?:" son
    // SOLO un fallback para levantar el proyecto rápido en una máquina de desarrollo
    // local; no deben usarse en ningún ambiente real.
    private static final String URL =
            System.getenv().getOrDefault("PELUQPRO_DB_URL", "jdbc:postgresql://localhost:5432/peluqueria");
    private static final String USER =
            System.getenv().getOrDefault("PELUQPRO_DB_USER", "postgres");
    private static final String PASSWORD =
            System.getenv("PELUQPRO_DB_PASSWORD"); // sin fallback: si falta, debe fallar explícitamente

    public static Connection getConnection() {
        if (PASSWORD == null) {
            System.err.println("❌ Falta la variable de entorno PELUQPRO_DB_PASSWORD. " +
                    "Configurala antes de iniciar la aplicación (no se permite contraseña por defecto).");
            return null;
        }
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);

        } catch (ClassNotFoundException e) {
            System.err.println("❌ Driver PostgreSQL no encontrado. Agregá postgresql-42.x.x.jar a las librerías.");
            return null;
        } catch (SQLException e) {
            System.err.println("❌ Error de conexión: " + e.getMessage());
            return null;
        }
    }
}