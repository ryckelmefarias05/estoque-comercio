package com.comercio.estoque.operation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final String email, password;
    public AdminBootstrap(JdbcTemplate db, PasswordEncoder encoder,
            @Value("${vintra.admin.email:}") String email, @Value("${vintra.admin.password:}") String password) {
        this.db = db; this.encoder = encoder; this.email = email.trim().toLowerCase(); this.password = password;
    }
    @Override public void run(ApplicationArguments args) {
        if (db.queryForObject("select count(*) from users where role = 'ADMIN' and active = true", Long.class) > 0) return;
        if (!email.contains("@") || email.contains(":") || password.length() < 16 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
            throw new IllegalStateException("Primeiro acesso: configure VINTRA_ADMIN_EMAIL e VINTRA_ADMIN_PASSWORD (16 a 72 bytes)");
        db.update("insert into users(name,email,password,role) values (?,?,?,'ADMIN')", "Gestor", email, encoder.encode(password));
    }
}
