package dao;

import java.sql.*;

public class EstructuraBD {
    public static void main(String[] args) {
        try (Connection conn = ConexionBD.getConnection()) {
            if (conn == null) {
                System.err.println("No se pudo conectar a la base.");
                return;
            }

            DatabaseMetaData meta = conn.getMetaData();

            ResultSet tables = meta.getTables(null, null, "%", new String[] {"TABLE"});
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("Tabla: " + tableName);

                ResultSet columns = meta.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    String columnName = columns.getString("COLUMN_NAME");
                    String type = columns.getString("TYPE_NAME");
                    int size = columns.getInt("COLUMN_SIZE");
                    System.out.println("  " + columnName + " " + type + "(" + size + ")");
                }

                ResultSet pk = meta.getPrimaryKeys(null, null, tableName);
                while (pk.next()) {
                    System.out.println("  Clave primaria: " + pk.getString("COLUMN_NAME"));
                }

                System.out.println(); // Separador entre tablas
            }

        } catch (SQLException e) {
            System.err.println("Error al obtener estructura: " + e.getMessage());
        }
    }
}
