package br.com.alexandreluchetti.mibackend.config.shared;

import br.com.alexandreluchetti.mibackend.core.model.RoleEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private final StaticTokenFilter staticTokenFilter;

    public SecurityConfig(StaticTokenFilter staticTokenFilter) {
        this.staticTokenFilter = staticTokenFilter;
    }

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/webjars/**"
                        ).permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/arquivos/upload").hasRole(RoleEnum.ENVIO.name())
                        .pathMatchers(HttpMethod.GET, "/api/arquivos/*/progresso").hasAnyRole(RoleEnum.ENVIO.name(), RoleEnum.CONSULTA.name())
                        .pathMatchers(HttpMethod.GET, "/api/arquivos/*/resultado").hasRole(RoleEnum.CONSULTA.name())
                        .anyExchange().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((swe, e) -> {
                            swe.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                            return Mono.empty();
                        })
                )
                .addFilterBefore(staticTokenFilter, SecurityWebFiltersOrder.AUTHENTICATION)
                .build();
    }
}
