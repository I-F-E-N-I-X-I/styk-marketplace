package ru.styk.admin;

import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.styk.common.Store;
import ru.styk.domain.Models.*;
import ru.styk.auth.AuthService;
import ru.styk.catalog.CatalogService;
import ru.styk.order.OrderService;
import java.util.*;

@RestController @RequestMapping("/api") @Transactional
public class AdminController {
    private final Store db;private final AuthService auth;private final CatalogService catalog;
    public AdminController(Store db,AuthService auth,CatalogService catalog){this.db=db;this.auth=auth;this.catalog=catalog;}
    @GetMapping("/admin/stats") Map<String,Object> stats(){return Map.of(
        "orders",db.query(Long.class,"select count(o) from Purchase o").getFirst(),
        "revenue",db.query(java.math.BigDecimal.class,"select coalesce(sum(total),0) from Purchase where paymentStatus='DEMO_PAID'").getFirst(),
        "products",db.query(Long.class,"select count(p) from Product p").getFirst(),
        "lowStock",db.query(Long.class,"select count(p) from Product p where stock-reserved<=threshold").getFirst(),
        "sales",db.query(Object[].class,"select i.name, sum(i.quantity) from OrderItem i, Purchase o where i.orderId=o.id and o.paymentStatus='DEMO_PAID' group by i.name order by sum(i.quantity) desc"));}
    @GetMapping("/admin/products") List<Product> products(){return db.query(Product.class,"from Product order by id desc");}
    public record Moderation(@NotBlank String status){}
    @PostMapping("/admin/products/{id}/status") Product moderate(@PathVariable Long id,@Valid @RequestBody Moderation in){if(!Set.of("ACTIVE","REJECTED","PENDING").contains(in.status))OrderService.fail("Неизвестный статус");Product p=db.lock(Product.class,id);p.status=in.status;catalog.audit(auth.current().email,"Модерация товара №"+id+": "+in.status);return p;}
    @GetMapping("/admin/users") List<Account> users(){return db.query(Account.class,"from Account order by id");}
    public record UserChange(@NotBlank String role,boolean enabled){}
    @PutMapping("/admin/users/{id}") Account user(@PathVariable Long id,@Valid @RequestBody UserChange in){Account actor=auth.current();if(actor.id.equals(id))OrderService.fail("Нельзя изменить собственную роль");if(!Set.of("BUYER","SELLER","WAREHOUSE","ADMIN").contains(in.role))OrderService.fail("Неизвестная роль");Account a=db.lock(Account.class,id);a.role=in.role;a.enabled=in.enabled;catalog.audit(actor.email,"Изменён пользователь №"+id);return a;}
    @GetMapping("/admin/audit") List<Audit> audit(){return db.query(Audit.class,"from Audit order by id desc");}
    @GetMapping("/warehouse/inventory") List<Product> inventory(){return db.query(Product.class,"from Product order by id");}
    @GetMapping("/warehouse/movements") List<Movement> movements(){return db.query(Movement.class,"from Movement order by id desc");}
    public record Rule(@Min(0) @Max(10000) int threshold,@Min(1) @Max(10000) int target,@NotBlank @Size(max=30) String location){}
    @PutMapping("/warehouse/inventory/{id}") Product rule(@PathVariable Long id,@Valid @RequestBody Rule r){if(r.target<r.threshold)OrderService.fail("Целевой запас меньше порога");Product p=db.lock(Product.class,id);p.threshold=r.threshold;p.target=r.target;p.location=r.location;catalog.audit(auth.current().email,"Обновлено правило пополнения №"+id);return p;}
}
