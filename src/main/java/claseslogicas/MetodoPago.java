package claseslogicas;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class MetodoPago {
    private int idMetodo;
    private String nombreMetodo;
    private double porcentajeModificador;

    public MetodoPago(int idMetodo, String nombreMetodo, double porcentajeModificador) {
        this.idMetodo = idMetodo;
        this.nombreMetodo = nombreMetodo;
        this.porcentajeModificador = porcentajeModificador;
    }

    public int getIdMetodo() { return idMetodo; }

    public double getPorcentajeModificador() { return porcentajeModificador; }

}
