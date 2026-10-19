package com.seventhray.contractmanager.config;

import com.seventhray.contractmanager.service.SupabaseAuthService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class AuthenticatedUserFilter extends OncePerRequestFilter {
    public static final String OWNER_ID_ATTRIBUTE = "authenticatedUserId";

    private final SupabaseAuthService auth;

    public AuthenticatedUserFilter(SupabaseAuthService auth) {
        this.auth = auth;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || !("/api/contracts".equals(path) || path.startsWith("/api/contracts/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || !authorization.regionMatches(true, 0, "Bearer ", 0, 7)
                || authorization.substring(7).isBlank()) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Sign in to access your contracts.");
            return;
        }

        try {
            UUID ownerId = auth.resolveUserId(authorization.substring(7).trim());
            request.setAttribute(OWNER_ID_ATTRIBUTE, ownerId);
            chain.doFilter(request, response);
        } catch (SupabaseAuthService.InvalidAccessTokenException ex) {
            reject(response, HttpServletResponse.SC_UNAUTHORIZED, "Your session is invalid or expired. Sign in again.");
        } catch (SupabaseAuthService.AuthProviderUnavailableException ex) {
            reject(response, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "Sign-in verification is temporarily unavailable.");
        }
    }

    private static void reject(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
