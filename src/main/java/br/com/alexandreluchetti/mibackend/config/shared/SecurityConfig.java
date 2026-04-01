package br.com.alexandreluchetti.mibackend.config.shared;

import br.com.alexandreluchetti.mibackend.core.model.RoleEnum;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final StaticTokenFilter staticTokenFilter;

    public SecurityConfig(StaticTokenFilter staticTokenFilter) {
        this.staticTokenFilter = staticTokenFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/arquivos/upload").hasRole(RoleEnum.ENVIO.name())
                        .requestMatchers(HttpMethod.GET, "/api/arquivos/*/progresso").hasAnyRole(RoleEnum.ENVIO.name(), RoleEnum.CONSULTA.name())
                        .requestMatchers(HttpMethod.GET, "/api/arquivos/*/resultado").hasRole(RoleEnum.CONSULTA.name())
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
                .addFilterBefore(staticTokenFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
