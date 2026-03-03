package io.caudal.server.auth;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@EnableConfigurationProperties(AuthProperties.class)
public class AuthFilter extends OncePerRequestFilter {

    private final AuthProperties props;

    public AuthFilter(AuthProperties props) {
        this.props = props;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
        FilterChain chain) throws ServletException, IOException {
        if (props.disabled()) {
            chain.doFilter(request, response);
            return;
        }

        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.isBlank()) {
            sendUnauthorized(response, "Missing Authorization header");
            return;
        }

        String token = extractToken(authorization);
        if (token == null) {
            sendUnauthorized(response, "Invalid Authorization format. Use 'Bearer <key>' or 'Token <key>'");
            return;
        }

        if (!token.equals(props.apiKey())) {
            sendUnauthorized(response, "Invalid API key");
            return;
        }

        chain.doFilter(request, response);
    }

    private String extractToken(String header) {
        if (header.startsWith("Bearer ")) {
            return header.substring(7).trim();
        }
        if (header.startsWith("Token ")) {
            return header.substring(6).trim();
        }
        return null;
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
