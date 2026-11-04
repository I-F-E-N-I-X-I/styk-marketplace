package ru.styk.supply;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ru.styk.auth.AuthService;
import ru.styk.common.Store;
import ru.styk.domain.Models.*;
import ru.styk.order.OrderService;
import java.time.*;
import java.util.*;

@Service @Transactional
public class SupplyService {
    private final Store db;private final AuthService auth;private final OrderService orders;
    public SupplyService(Store db,AuthService auth,OrderService orders){this.db=db;this.auth=auth;this.orders=orders;}
    public List<Supply> supplies(boolean seller){Account a=auth.current();return seller&&!a.role.equals("ADMIN")?db.query(Supply.class,"from Supply where sellerId=?1 order by id desc",a.id):db.query(Supply.class,"from Supply order by id desc");}
    public int replenish(){
        int created=0;
        var ids=db.query(Long.class,"select id from Product where status='ACTIVE' order by id");
        for(Long id:ids){
            Product p=db.lock(Product.class,id);
            long pending=db.query(Supply.class,"from Supply where productId=?1 and status in ('REQUESTED','CONFIRMED','IN_TRANSIT')",id).stream().mapToLong(s->s.quantity).sum();
            long sold=db.query(Long.class,"select coalesce(sum(i.quantity),0) from OrderItem i, Purchase o where i.orderId=o.id and i.productId=?1 and o.createdAt>=?2 and o.status in ('SHIPPED','DELIVERED')",id,Instant.now().minus(Duration.ofDays(14))).getFirst();
            int forecast=(int)Math.min(10000,Math.ceil(sold/14.0*7));
            int goal=Math.max(p.target,forecast);
            if(p.getAvailable()<=Math.max(p.threshold,forecast/2)&&p.getAvailable()+pending<goal){
                Supply s=new Supply();s.productId=id;s.sellerId=p.sellerId;s.quantity=(int)(goal-p.getAvailable()-pending);s.status="REQUESTED";s.note="Автопополнение · целевой запас "+goal;db.add(s);created++;
            }
        }
        return created;
    }
    public Supply create(Long productId,int quantity){Account a=auth.current();Product p=db.lock(Product.class,productId);owner(a,p.sellerId);Supply s=new Supply();s.productId=productId;s.sellerId=p.sellerId;s.quantity=quantity;s.status="REQUESTED";s.note="Заявка продавца";return db.add(s);}
    public Supply transition(Long id,String status,int received,String note,boolean warehouse){
        Account a=auth.current();
        // Lock product before supply, matching the planner's lock order.
        Supply found=db.get(Supply.class,id);Product p=db.lock(Product.class,found.productId);Supply s=db.lock(Supply.class,id);
        if(!warehouse)owner(a,s.sellerId);
        if(s.status.equals(status))return s;
        if(warehouse){
            if(!s.status.equals("IN_TRANSIT")||!status.equals("RECEIVED"))OrderService.fail("Принимать можно только поставку в пути");
            if(received<0||received>s.quantity)OrderService.fail("Принятое количество должно быть от 0 до заказанного");
            if(received!=s.quantity&&note.isBlank())OrderService.fail("Укажите причину расхождения");
            p.stock+=received;s.received=received;s.note=note;orders.movement(p.id,"RECEIVE",received,"SUPPLY-"+s.id);
        }else{
            String expected=Map.of("REQUESTED","CONFIRMED","CONFIRMED","IN_TRANSIT").get(s.status);
            boolean cancel=status.equals("CANCELLED")&&Set.of("REQUESTED","CONFIRMED").contains(s.status);
            if(!cancel&&!status.equals(expected))OrderService.fail("Недопустимый статус поставки");
        }
        s.status=status;return s;
    }
    private void owner(Account a,Long sellerId){if(!a.role.equals("ADMIN")&&!a.id.equals(sellerId))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Поставка другого продавца");}
}
