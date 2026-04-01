package br.com.alexandreluchetti.mibackend.core.model;

import org.springframework.security.core.GrantedAuthority;

public enum RoleEnum implements GrantedAuthority {

    CONSULTA, ENVIO;

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }
}
