package com.comercio.estoque.operation;

import com.comercio.estoque.entity.*;
import com.comercio.estoque.repository.*;
import com.comercio.estoque.exception.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/imports")
public class ImportController {
    private final ProductRepository products; private final InventoryCountRepository counts;
    private final AccessService access; private final JdbcTemplate db;
    public ImportController(ProductRepository products,InventoryCountRepository counts,AccessService access,JdbcTemplate db) {
        this.products=products; this.counts=counts; this.access=access; this.db=db;
    }
    public record Row(@NotBlank @Size(max=160) String name, @Size(max=80) String sku, @Size(max=80) String barcode,
                      @NotBlank @Size(max=30) String unit, @NotNull @DecimalMin("0") @Digits(integer=11,fraction=3) BigDecimal quantity) {}
    public record Request(@NotNull UUID requestKey,@NotBlank @Size(max=200) String fileName,
                          @NotNull Long assignedUserId,@NotEmpty @Size(max=5000) List<@Valid Row> rows,boolean preview) {}
    @GetMapping public List<Map<String,Object>> history() {
        return db.queryForList("select i.*, u.name as creator_name from inventory_imports i join users u on u.id=i.created_by_user_id order by i.created_at desc");
    }
    private String norm(String value) { return value==null || value.isBlank() ? null : value.trim(); }
    @PostMapping @Transactional public Map<String,Object> importRows(@Valid @RequestBody Request r) {
        access.requireAdmin(); access.validateOperator(r.assignedUserId());
        // Serialize confirmations, so simultaneous retries cannot create two inventories.
        if (!r.preview()) db.execute("select pg_advisory_xact_lock(82731019)");
        String fingerprint;
        try { fingerprint=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((r.fileName()+r.assignedUserId()+r.rows()).getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
        var previous=db.queryForList("select * from inventory_imports where request_key=?",r.requestKey());
        if (!previous.isEmpty()) {
            if (!fingerprint.equals(previous.get(0).get("payload_hash"))) throw new BusinessException("Esta chave já foi usada com outro arquivo. Selecione o arquivo novamente.");
            return Map.of("alreadyImported",true,"inventoryCountId",previous.get(0).get("inventory_count_id"),"createdProducts",previous.get(0).get("created_products"),"productCount",previous.get(0).get("product_count"));
        }
        Set<String> skus=new HashSet<>(), barcodes=new HashSet<>(); Set<Long> ids=new HashSet<>();
        List<Map<String,Object>> preview=new ArrayList<>();
        InventoryCount count=new InventoryCount(); count.setAssignedUserId(r.assignedUserId()); count.setCreatedByUserId(access.id());
        int created=0, index=0;
        for (Row row:r.rows()) {
            index++;
            String sku=norm(row.sku()), barcode=norm(row.barcode());
            if (sku==null && barcode==null) throw new BusinessException("Linha "+index+": informe SKU ou código de barras");
            if ((sku!=null && !skus.add(sku)) || (barcode!=null && !barcodes.add(barcode))) throw new BusinessException("Linha "+index+": identificador duplicado no arquivo");
            Product bySku=sku==null ? null : products.findBySku(sku).orElse(null);
            Product byBarcode=barcode==null ? null : products.findByBarcode(barcode).orElse(null);
            if (bySku!=null && byBarcode!=null && !bySku.getId().equals(byBarcode.getId())) throw new BusinessException("Linha "+index+": SKU e código de barras apontam para produtos diferentes");
            Product p=bySku!=null ? bySku : byBarcode;
            boolean isNew=p==null;
            if (isNew) {
                p=new Product(); p.setName(row.name().trim()); p.setSku(sku); p.setBarcode(barcode); p.setUnit(row.unit().trim().toUpperCase()); created++;
                if (!r.preview()) p=products.save(p);
            } else {
                if (!p.getActive()) throw new BusinessException("Linha "+index+": produto inativo");
                if (!ids.add(p.getId())) throw new BusinessException("Linha "+index+": produto repetido por outro identificador");
                if ((sku!=null && p.getSku()!=null && !sku.equals(p.getSku())) || (barcode!=null && p.getBarcode()!=null && !barcode.equals(p.getBarcode()))) throw new BusinessException("Linha "+index+": identificadores divergem do cadastro");
                if (!p.getUnit().equalsIgnoreCase(row.unit().trim())) throw new BusinessException("Linha "+index+": unidade diferente do cadastro. Não há conversão automática de unidades.");
            }
            preview.add(Map.of("line",index,"name",p.getName(),"sku",sku==null ? "" : sku,"quantity",row.quantity(),"action",isNew ? "CREATE" : "MATCH"));
            if (!r.preview()) {
                InventoryCountItem item=new InventoryCountItem(); item.setProduct(p); item.setExpectedQuantity(row.quantity()); count.addItem(item);
            }
        }
        if (r.preview()) return Map.of("rows",preview,"createdProducts",created,"productCount",r.rows().size());
        count=counts.saveAndFlush(count);
        db.update("insert into inventory_imports(request_key,file_name,created_by_user_id,inventory_count_id,product_count,created_products,payload_hash) values (?,?,?,?,?,?,?)",
            r.requestKey(),r.fileName(),access.id(),count.getId(),r.rows().size(),created,fingerprint);
        return Map.of("inventoryCountId",count.getId(),"createdProducts",created,"productCount",r.rows().size(),"alreadyImported",false);
    }
}
