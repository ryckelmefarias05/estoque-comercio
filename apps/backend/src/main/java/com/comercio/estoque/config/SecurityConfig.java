package com.comercio.estoque.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(JdbcTemplate db) {
        return username -> db.query("select email, password, role from users where email = ? and active = true",
            (rs, n) -> User.withUsername(rs.getString("email")).password(rs.getString("password"))
                .roles(rs.getString("role")).build(), username.trim().toLowerCase())
            .stream().findFirst().orElseThrow(() -> new UsernameNotFoundException("Credenciais inválidas"));
    }
    @Bean SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/me").authenticated()
                .requestMatchers("/api/users/**", "/api/imports/**", "/api/products/**", "/api/stock/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/inventory-counts", "/api/tasks").hasRole("ADMIN")
                .requestMatchers("/api/inventory-counts/**", "/api/tasks/**").authenticated()
                .anyRequest().denyAll())
            .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
