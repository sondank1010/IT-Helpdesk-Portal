package com.example.backend.security;

import com.example.backend.service.AuthService;
import com.example.backend.service.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * auth request có header Authorization: Bearer .... Token hợp lệ thì vào SecurityContext, endpoint ko cần tự check token.
 */
public class JwtFilter extends OncePerRequestFilter {
    private static final String PREFIX = "Bearer ";

    private final AuthService auth;

    public JwtFilter(AuthService auth) {
        this.auth = auth;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            try {
                AuthService.Authenticated current = auth.authenticate(header.substring(PREFIX.length()));
                var authentication = new UsernamePasswordAuthenticationToken(current.user(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + current.user().getRole().name())));
                authentication.setDetails(current.sessionId());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException ex) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }
}
