package br.com.alexandreluchetti.mibackend.config.shared;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class StaticTokenFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String ROLE_ENVIO = "ROLE_ENVIO";
    private static final String ROLE_CONSULTA = "ROLE_CONSULTA";

    @Value("${security.tokens.envio}")
    private String tokenEnvio;

    @Value("${security.tokens.consulta}")
    private String tokenConsulta;

    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request,
                                    @org.springframework.lang.NonNull HttpServletResponse response,
                                    @org.springframework.lang.NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            String role = resolveRole(token);

            if (role != null) {
                var authorities = List.of(new SimpleGrantedAuthority(role));
                var authentication = new UsernamePasswordAuthenticationToken(token, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }

    private String resolveRole(String token) {
        if (tokenEnvio.equals(token)) return ROLE_ENVIO;
        if (tokenConsulta.equals(token)) return ROLE_CONSULTA;
        return null;
    }
}
