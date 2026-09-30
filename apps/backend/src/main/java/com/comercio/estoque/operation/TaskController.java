package com.comercio.estoque.operation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import com.comercio.estoque.exception.BusinessException;
import com.comercio.estoque.exception.ResourceNotFoundException;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final JdbcTemplate db; private final AccessService access;
    public TaskController(JdbcTemplate db, AccessService access) { this.db=db; this.access=access; }
    public record CreateTask(@NotBlank @Size(max=160) String title, @Size(max=4000) String description,
                             @NotBlank String type, @NotNull Long assignedUserId, LocalDateTime dueDate) {}
    public record UpdateTask(@NotBlank String status, @Size(max=4000) String notes) {}
    private static final String SELECT = "select t.*, u.name as operator_name from tasks t left join users u on u.id=t.assigned_user_id";
    @GetMapping public List<Map<String,Object>> list() {
        return access.admin() ? db.queryForList(SELECT+" order by t.created_at desc") : db.queryForList(SELECT+" where t.assigned_user_id=? order by t.created_at desc",access.id());
    }
    @PostMapping public Map<String,Object> create(@Valid @RequestBody CreateTask r) {
        access.validateOperator(r.assignedUserId());
        if (!Set.of("ORGANIZATION","EXPIRATION","QUALITY","RECEIVING","OTHER").contains(r.type())) throw new BusinessException("Tipo de tarefa inválido");
        Long id=db.queryForObject("insert into tasks(title,description,type,assigned_user_id,created_by_user_id,due_date) values (?,?,?,?,?,?) returning id",
            Long.class,r.title().trim(),r.description(),r.type(),r.assignedUserId(),access.id(),r.dueDate());
        return db.queryForMap(SELECT+" where t.id=?",id);
    }
    @PatchMapping("/{id}") @Transactional public Map<String,Object> update(@PathVariable Long id,@Valid @RequestBody UpdateTask r) {
        List<Map<String,Object>> found=db.queryForList("select * from tasks where id=? for update",id);
        if (found.isEmpty()) throw new ResourceNotFoundException("Tarefa não encontrada");
        Map<String,Object> task=found.get(0);
        access.checkOwner(task.get("assigned_user_id")==null ? null : ((Number)task.get("assigned_user_id")).longValue());
        String current=(String)task.get("status");
        if (!(current.equals("PENDING") && r.status().equals("IN_PROGRESS")) && !(current.equals("IN_PROGRESS") && r.status().equals("COMPLETED")))
            throw new BusinessException("Inicie a tarefa antes de concluir. Tarefas concluídas não podem ser alteradas.");
        db.update("update tasks set status=?, execution_notes=?, updated_at=current_timestamp, started_at=coalesce(started_at,current_timestamp), completed_at=case when ?='COMPLETED' then current_timestamp else null end where id=?",
            r.status(),r.notes(),r.status(),id);
        return db.queryForMap(SELECT+" where t.id=?",id);
    }
}
