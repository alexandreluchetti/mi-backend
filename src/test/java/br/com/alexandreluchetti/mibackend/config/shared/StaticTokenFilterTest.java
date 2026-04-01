package br.com.alexandreluchetti.mibackend.config.shared;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StaticTokenFilterTest {

    private StaticTokenFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new StaticTokenFilter();
        ReflectionTestUtils.setField(filter, "tokenEnvio", "token-envio-123");
        ReflectionTestUtils.setField(filter, "tokenConsulta", "token-consulta-456");

        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithValidEnvioToken_ShouldSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer token-envio-123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_ENVIO")));
    }

    @Test
    void doFilterInternal_WithValidConsultaToken_ShouldSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer token-consulta-456");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertTrue(SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(a -> a.getAuthority().equals("ROLE_CONSULTA")));
    }

    @Test
    void doFilterInternal_WithInvalidToken_ShouldNotSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer token-invalido");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithoutHeader_ShouldNotSetAuthentication() throws Exception {
        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithInvalidPrefix_ShouldNotSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic token-envio-123");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
