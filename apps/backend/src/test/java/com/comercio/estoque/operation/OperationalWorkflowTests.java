package com.comercio.estoque.operation;

import com.comercio.estoque.entity.*;
import com.comercio.estoque.repository.*;
import com.comercio.estoque.exception.BusinessException;
import com.comercio.estoque.service.InventoryCountService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import java.math.BigDecimal;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Runs against the configured PostgreSQL. Each test rolls back its data.
@SpringBootTest
@Transactional
class OperationalWorkflowTests {
    @Autowired JdbcTemplate db;
    @Autowired PasswordEncoder encoder;
    @Autowired ImportController imports;
    @Autowired InventoryCountService counts;
    @Autowired InventoryCountRepository countRepository;
    @Autowired ProductRepository products;
    @Autowired TaskController tasks;
    @Autowired WebApplicationContext context;
    private long adminId, operatorId, otherId;
    private String adminEmail, operatorEmail, otherEmail;
    private final String password = "test-only-password-2026";
    private MockMvc mvc;
    @BeforeEach void setup() {
        String suffix=UUID.randomUUID().toString();
        adminEmail="admin-"+suffix+"@example.test"; operatorEmail="operator-"+suffix+"@example.test"; otherEmail="other-"+suffix+"@example.test";
        adminId=insertUser(adminEmail,"ADMIN"); operatorId=insertUser(operatorEmail,"OPERATOR"); otherId=insertUser(otherEmail,"OPERATOR");
        authenticate(adminEmail);
        mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }
    private long insertUser(String email,String role) {
        return db.queryForObject("insert into users(name,email,password,role) values (?,?,?,?) returning id",Long.class,role,email,encoder.encode(password),role);
    }
    private void authenticate(String email) { SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(email,"unused",List.of())); }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private ImportController.Request request(UUID key,String sku,boolean preview) {
        return new ImportController.Request(key,"test.csv",operatorId,List.of(new ImportController.Row("Test product",sku,null,"UN",new BigDecimal("17.500"))),preview);
    }
    @Test void previewDoesNotWriteAndRetryDoesNotDuplicate() {
        String sku="T-"+UUID.randomUUID(); UUID key=UUID.randomUUID();
        var preview=imports.importRows(request(key,sku,true));
        assertEquals(1,preview.get("createdProducts")); assertTrue(products.findBySku(sku).isEmpty());
        var result=imports.importRows(request(key,sku,false));
        long id=((Number)result.get("inventoryCountId")).longValue();
        var count=countRepository.findById(id).orElseThrow();
        assertEquals(operatorId,count.getAssignedUserId().longValue());
        assertEquals(0,count.getItems().get(0).getExpectedQuantity().compareTo(new BigDecimal("17.500")));
        long productId=products.findBySku(sku).orElseThrow().getId();
        assertEquals(0L,db.queryForObject("select count(*) from stock_items where product_id=?",Long.class,productId).longValue());
        assertEquals(true,imports.importRows(request(key,sku,false)).get("alreadyImported"));
        assertEquals(1L,db.queryForObject("select count(*) from inventory_imports where request_key=?",Long.class,key).longValue());
    }
    @Test void repeatedImportUsesExistingProductWithoutChangingItsStock() {
        String sku="T-"+UUID.randomUUID();
        imports.importRows(request(UUID.randomUUID(),sku,false));
        long productId=products.findBySku(sku).orElseThrow().getId();
        db.update("insert into stock_items(product_id,quantity) values (?,9)",productId);
        assertEquals(0,imports.importRows(request(UUID.randomUUID(),sku,false)).get("createdProducts"));
        assertEquals(0,db.queryForObject("select quantity from stock_items where product_id=?",BigDecimal.class,productId).compareTo(new BigDecimal("9")));
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void invalidRowRollsBackEarlierNewProducts() {
        String sku="T-"+UUID.randomUUID();
        var rows=List.of(new ImportController.Row("Valid",sku,null,"UN",BigDecimal.ONE),new ImportController.Row("Missing identity",null,null,"UN",BigDecimal.ONE));
        try {
            assertThrows(BusinessException.class,()->imports.importRows(new ImportController.Request(UUID.randomUUID(),"invalid.csv",operatorId,rows,false)));
            assertTrue(products.findBySku(sku).isEmpty(), "The new product must roll back when a later row fails");
        } finally { db.update("delete from users where id in (?,?,?)",adminId,operatorId,otherId); }
    }
    @Test void operatorCanOnlyReadTheirOwnCountsAndCannotReassign() {
        long id=((Number)imports.importRows(request(UUID.randomUUID(),"T-"+UUID.randomUUID(),false)).get("inventoryCountId")).longValue();
        authenticate(otherEmail);
        assertThrows(AccessDeniedException.class,()->counts.findById(id));
        assertFalse(counts.findAll().stream().anyMatch(c->c.id()==id));
        authenticate(operatorEmail); assertEquals(id,counts.findById(id).id().longValue());
        assertThrows(AccessDeniedException.class,()->counts.assign(id,otherId));
    }
    @Test void taskOwnershipAndTransitionsAreEnforced() {
        long id=((Number)tasks.create(new TaskController.CreateTask("Test task","Instructions","QUALITY",operatorId,null)).get("id")).longValue();
        authenticate(otherEmail);
        assertThrows(AccessDeniedException.class,()->tasks.update(id,new TaskController.UpdateTask("IN_PROGRESS",null)));
        authenticate(operatorEmail);
        assertThrows(BusinessException.class,()->tasks.update(id,new TaskController.UpdateTask("COMPLETED",null)));
    }
    @Test void endpointsEnforceRolesEvenWithoutTheFrontend() throws Exception {
        mvc.perform(get("/api/products").with(httpBasic(operatorEmail,password))).andExpect(status().isForbidden());
        mvc.perform(get("/api/users").with(httpBasic(operatorEmail,password))).andExpect(status().isForbidden());
        mvc.perform(get("/api/imports").with(httpBasic(operatorEmail,password))).andExpect(status().isForbidden());
        mvc.perform(get("/api/me").with(httpBasic(operatorEmail,password))).andExpect(status().isOk());
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }
}
