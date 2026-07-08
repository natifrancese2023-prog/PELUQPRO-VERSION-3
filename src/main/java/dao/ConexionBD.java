package dao;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class ConexionBD {

    private static final String URL =
            System.getenv().getOrDefault("PELUQPRO_DB_URL", "jdbc:postgresql://localhost:5432/peluqueria");
    private static final String USER =
            System.getenv().getOrDefault("PELUQPRO_DB_USER", "postgres");
    private static final String PASSWORD =
            System.getenv("PELUQPRO_DB_PASSWORD"); // sin fallback: si falta, debe fallar explícitamente

    public static Connection getConnection() throws SQLException {
        if (PASSWORD == null) {
            throw new SQLException("Falta la variable de entorno PELUQPRO_DB_PASSWORD.");
        }
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver PostgreSQL no encontrado.", e);
        }
    }
}
