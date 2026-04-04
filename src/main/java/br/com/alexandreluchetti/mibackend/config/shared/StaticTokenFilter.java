package br.com.alexandreluchetti.mibackend.config.shared;

import br.com.alexandreluchetti.mibackend.core.model.RoleEnum;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class StaticTokenFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    @Value("${security.tokens.envio}")
    private String tokenEnvio;

    @Value("${security.tokens.consulta}")
    private String tokenConsulta;

    @Override
    @NonNull
    public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");

        if (authHeader != null && authHeader.startsWith(BEARER_PREFIX)) {
            String token = authHeader.substring(BEARER_PREFIX.length()).trim();
            RoleEnum role = resolveRole(token);

            if (role != null) {
                var authorities = List.of(role);
                var authentication = new UsernamePasswordAuthenticationToken(token, null, authorities);
                SecurityContext context = new SecurityContextImpl(authentication);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
            }
        }

        return chain.filter(exchange);
    }

    private RoleEnum resolveRole(String token) {
        if (tokenEnvio.equals(token)) return RoleEnum.ENVIO;
        if (tokenConsulta.equals(token)) return RoleEnum.CONSULTA;
        return null;
    }
}
