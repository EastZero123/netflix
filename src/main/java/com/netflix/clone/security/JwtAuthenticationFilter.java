package com.netflix.clone.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    public JwtAuthenticationFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String jwt = extractJwtToken(request);
        String username = null;
        if(jwt != null) {
            username = jwtUtil.getUsernameFromToken(jwt);
        }

        if(shouldProcessAuthentication(username)) {
            processAuthentication(request, jwt, username);
        }

        filterChain.doFilter(request, response);
    }

    private void processAuthentication(HttpServletRequest request, String jwt, String username) {
        if(jwtUtil.validateToken(jwt)) {
            UserDetails userDetails = createUserDetailFromToken(jwt, username);
            setAuthenticationInContext(request, userDetails);
        }
    }

    private void setAuthenticationInContext(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }

    private UserDetails createUserDetailFromToken(String jwt, String username) {
        String role = jwtUtil.getRoleFromToken(jwt);

        return User.builder()
                .username(username)
                .password("")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)))
                .build();
    }

    private String extractJwtToken(HttpServletRequest request) {
        final String authorizationHeader = request.getHeader("Authorization");
        final String requestURI = request.getRequestURI();

        if(authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }
        else if ((requestURI.contains("/api/files/video/") || requestURI.contains("/api/files/image/"))
        && request.getParameter("token") != null) {
            return request.getParameter("token");
        }
        return null;
    }

    private boolean shouldProcessAuthentication(String username) {
        return username != null && SecurityContextHolder.getContext().getAuthentication() == null;
    }
}
