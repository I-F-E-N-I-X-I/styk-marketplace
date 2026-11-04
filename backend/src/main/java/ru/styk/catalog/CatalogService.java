package ru.styk.catalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.styk.common.Store;
import ru.styk.domain.Models.*;
import ru.styk.auth.AuthService;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.*;

@Service @Transactional
public class CatalogService {
    private final Store db; private final AuthService auth;
    public CatalogService(Store db,AuthService auth){this.db=db;this.auth=auth;}
    public List<Product> products(){return db.query(Product.class,"from Product where status='ACTIVE' order by id");}
    public Product product(Long id){Product p=db.get(Product.class,id);if(!p.status.equals("ACTIVE"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Товар недоступен");return p;}
    public record ProductInput(@NotBlank @Size(max=160) String name,@NotBlank @Size(max=100) String brand,
        @NotBlank @Size(max=60) String category,@NotBlank @Size(max=4000) String description,
        @NotNull @DecimalMin("1.00") @DecimalMax("9999999.00") BigDecimal price,
        @NotBlank @Pattern(regexp="(/assets/[a-zA-Z0-9._/-]+|https://[^\\s]+)") @Size(max=500) String image,
        @NotBlank @Size(max=30) String color,@NotBlank @Size(max=80) String sku){}
    public Product save(Long id,ProductInput in){
        Account a=auth.current();Product p=id==null?new Product():db.lock(Product.class,id);
        if(id!=null&&!p.sellerId.equals(a.id)&&!a.role.equals("ADMIN"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Это товар другого продавца");
        if(id==null)p.sellerId=a.id;
        p.name=in.name;p.brand=in.brand;p.category=in.category;p.description=in.description;p.price=in.price;p.image=in.image;p.color=in.color;p.sku=in.sku;
        p.status="PENDING";if(id==null)db.add(p);audit(a.email,"Товар №"+p.id+" отправлен на модерацию");return p;
    }
    public List<Product> sellerProducts(){Account a=auth.current();return a.role.equals("ADMIN")?db.query(Product.class,"from Product order by id"):db.query(Product.class,"from Product where sellerId=?1 order by id",a.id);}
    public List<Review> reviews(Long id){return db.query(Review.class,"from Review where productId=?1 order by createdAt desc",id);}
    public Review review(Long id,int rating,String text){
        Account a=auth.current();product(id);
        Long purchased=db.query(Long.class,"select count(i) from OrderItem i, Purchase o where i.orderId=o.id and o.userId=?1 and i.productId=?2 and o.status='DELIVERED'",a.id,id).getFirst();
        if(purchased==0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Отзыв доступен после доставки покупки");
        Review r=new Review();r.userId=a.id;r.productId=id;r.author=a.name;r.rating=rating;r.text=text;return db.add(r);
    }
    public List<Long> favorites(){return db.query(Long.class,"select productId from Favorite where userId=?1",auth.current().id);}
    public void favorite(Long id,boolean enabled){Account a=db.lock(Account.class,auth.current().id);product(id);var list=db.query(Favorite.class,"from Favorite where userId=?1 and productId=?2",a.id,id);if(enabled&&list.isEmpty()){Favorite f=new Favorite();f.userId=a.id;f.productId=id;db.add(f);}else if(!enabled)list.forEach(db::remove);}
    public void audit(String actor,String action){Audit event=new Audit();event.actor=actor;event.action=action;db.add(event);}
}
