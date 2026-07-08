package service;

import claseslogicas.Usuario;
import dao.UsuarioDAO;

public class UsuarioService {

    private final UsuarioDAO usuarioDAO = new UsuarioDAO();

    public Usuario login(String usuario, String contrasenaIngresada) {
        return usuarioDAO.validarUsuario(usuario, contrasenaIngresada);
    }
}
