package com.comercio.estoque.operation;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import com.comercio.estoque.exception.BusinessException;
import java.util.Map;

@Service
public class AccessService {
    private final JdbcTemplate db;
    public AccessService(JdbcTemplate db) { this.db = db; }
    public Map<String, Object> current() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return db.queryForMap("select id, name, email, role from users where email = ? and active = true", email);
    }
    public long id() { return ((Number) current().get("id")).longValue(); }
    public boolean admin() { return "ADMIN".equals(current().get("role")); }
    public void requireAdmin() { if (!admin()) throw new AccessDeniedException("Acesso exclusivo do gestor"); }
    public void checkOwner(Long owner) {
        if (!admin() && (owner == null || owner.longValue() != id()))
            throw new AccessDeniedException("Este trabalho não está atribuído a você");
    }
    public void validateOperator(Long id) {
        if (id == null || db.queryForObject("select count(*) from users where id = ? and role = 'OPERATOR' and active = true", Long.class, id) != 1)
            throw new BusinessException("Selecione um operador ativo");
    }
}
