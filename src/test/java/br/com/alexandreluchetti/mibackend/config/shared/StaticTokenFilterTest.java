package br.com.alexandreluchetti.mibackend.config.shared;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class StaticTokenFilterTest {

    private StaticTokenFilter filter;
    private WebFilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new StaticTokenFilter();
        ReflectionTestUtils.setField(filter, "tokenEnvio", "token-envio-123");
        ReflectionTestUtils.setField(filter, "tokenConsulta", "token-consulta-456");

        filterChain = mock(WebFilterChain.class);
    }

    @Test
    void filter_WithValidEnvioToken_ShouldSetAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer token-envio-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenAnswer(invocation -> 
            ReactiveSecurityContextHolder.getContext().doOnNext(ctx -> {
                assertNotNull(ctx.getAuthentication());
                assertTrue(ctx.getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ENVIO")));
            }).then()
        );

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();
    }

    @Test
    void filter_WithValidConsultaToken_ShouldSetAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer token-consulta-456")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenAnswer(invocation -> 
            ReactiveSecurityContextHolder.getContext().doOnNext(ctx -> {
                assertNotNull(ctx.getAuthentication());
                assertTrue(ctx.getAuthentication().getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_CONSULTA")));
            }).then()
        );

        StepVerifier.create(filter.filter(exchange, filterChain))
                .verifyComplete();
    }

    @Test
    void filter_WithInvalidToken_ShouldNotSetAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Bearer token-invalido")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .verifyComplete();
        
        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_WithoutHeader_ShouldNotSetAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .verifyComplete();
        
        verify(filterChain).filter(exchange);
    }

    @Test
    void filter_WithInvalidPrefix_ShouldNotSetAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/")
                .header("Authorization", "Basic token-envio-123")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        when(filterChain.filter(any())).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, filterChain)
                .contextWrite(ReactiveSecurityContextHolder.clearContext()))
                .verifyComplete();
        
        verify(filterChain).filter(exchange);
    }
}
