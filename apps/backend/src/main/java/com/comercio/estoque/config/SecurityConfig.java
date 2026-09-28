package com.comercio.estoque.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;

@Configuration
public class SecurityConfig {
    @Bean
    UserDetailsService users(@Value("${vintra.api.user}") String username,
                             @Value("${vintra.api.password}") String password) {
        if (username.isBlank() || password.length() < 16) {
            throw new IllegalStateException("Configure VINTRA_API_USER e VINTRA_API_PASSWORD (mínimo 16 caracteres)");
        }
        return new InMemoryUserDetailsManager(User.withUsername(username)
                .password("{noop}" + password).roles("OPERATOR").build());
    }

    @Bean
    public org.springframework.security.web.SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }
}
