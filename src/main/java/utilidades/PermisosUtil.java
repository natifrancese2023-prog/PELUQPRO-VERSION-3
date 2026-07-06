package utilidades;

import claseslogicas.Rol;


public final class PermisosUtil {

    private PermisosUtil() {
        // Clase utilitaria: no se instancia.
    }

    private static Rol rolActual() {
        return SesionManager.getInstance().getUsuarioLogueado().getRol();
    }

    public static boolean esGerente() {
        return "Gerente".equalsIgnoreCase(rolActual().getNombre());
    }

    public static boolean esRecepcionista() {
        return "Recepcionista".equalsIgnoreCase(rolActual().getNombre());
    }

    public static boolean esEstilista() {
        Rol rol = rolActual();
        return "Estilista".equalsIgnoreCase(rol.getNombre()) || rol.isEsEstilista();
    }
}