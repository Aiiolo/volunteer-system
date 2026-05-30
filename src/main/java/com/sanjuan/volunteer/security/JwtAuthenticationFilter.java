package com.sanjuan.volunteer.security;

import com.sanjuan.volunteer.entity.Enums;
import com.sanjuan.volunteer.entity.SysUser;
import com.sanjuan.volunteer.repository.SysUserRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final SysUserRepository userRepository;

    public JwtAuthenticationFilter(JwtService jwtService, SysUserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwtService.parseToken(header.substring(7));
                Long userId = ((Number) claims.get("userId")).longValue();
                SysUser user = userRepository.findById(userId).orElse(null);
                if (user == null || user.getStatus() != Enums.UserStatus.ACTIVE) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }
                String role = claims.get("role", String.class);
                CurrentUser principal = new CurrentUser(userId, claims.getSubject(), role);
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        principal,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        filterChain.doFilter(request, response);
    }
}
