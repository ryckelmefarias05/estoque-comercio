package com.comercio.estoque.operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.comercio.estoque.exception.BusinessException;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UserController {
    private final JdbcTemplate db; private final AccessService access; private final PasswordEncoder encoder;
    public UserController(JdbcTemplate db, AccessService access, PasswordEncoder encoder) { this.db=db; this.access=access; this.encoder=encoder; }
    public record CreateUser(@NotBlank @Size(max=120) String name, @NotBlank @Email @Size(max=160) String email,
                             @NotBlank @Size(min=16,max=72) String password) {}
    @GetMapping("/me") public Map<String,Object> me() { return access.current(); }
    @GetMapping("/users") public List<Map<String,Object>> users() {
        return db.queryForList("select id,name,email,role,active from users order by name");
    }
    @PostMapping("/users") public Map<String,Object> create(@Valid @RequestBody CreateUser r) {
        String email=r.email().trim().toLowerCase();
        if (email.contains(":")) throw new BusinessException("E-mail não pode conter dois pontos");
        if (r.password().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new BusinessException("Senha deve ter até 72 bytes");
        if (db.queryForObject("select count(*) from users where email = ?", Long.class, email) > 0) throw new BusinessException("E-mail já cadastrado");
        Long id=db.queryForObject("insert into users(name,email,password,role) values (?,?,?,'OPERATOR') returning id", Long.class,
            r.name().trim(),email,encoder.encode(r.password()));
        return db.queryForMap("select id,name,email,role,active from users where id=?",id);
    }
}
