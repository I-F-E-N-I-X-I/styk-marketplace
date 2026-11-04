package ru.styk.order;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import ru.styk.common.Store;
import ru.styk.auth.AuthService;
import ru.styk.domain.Models.*;
import java.math.*;
import java.util.*;

@Service @Transactional
public class OrderService {
    private final Store db; private final AuthService auth;
    public OrderService(Store db,AuthService auth){this.db=db;this.auth=auth;}
    public record CartRow(Long id,Product product,int quantity){}
    public List<CartRow> cart(){return db.query(CartItem.class,"from CartItem where userId=?1 order by productId",auth.current().id).stream().map(c->new CartRow(c.id,db.get(Product.class,c.productId),c.quantity)).toList();}
    public void setCart(Long productId,int quantity){
        Account a=db.lock(Account.class,auth.current().id);
        Product p=db.get(Product.class,productId);
        if(quantity<0||quantity>99)fail("Количество должно быть от 0 до 99");
        if(quantity>0&&(!p.status.equals("ACTIVE")||quantity>p.getAvailable()))fail("Недостаточно товара на складе");
        var rows=db.query(CartItem.class,"from CartItem where userId=?1 and productId=?2",a.id,productId);
        if(quantity==0){rows.forEach(db::remove);return;}
        CartItem c=rows.isEmpty()?new CartItem():rows.getFirst();c.userId=a.id;c.productId=productId;c.quantity=quantity;if(c.id==null)db.add(c);
    }
    public record OrderView(Purchase order,List<OrderItem> items,List<OrderEvent> events){}
    public OrderView view(Purchase o){return new OrderView(o,db.query(OrderItem.class,"from OrderItem where orderId=?1 order by productId",o.id),db.query(OrderEvent.class,"from OrderEvent where orderId=?1 order by id",o.id));}
    public List<OrderView> orders(){return db.query(Purchase.class,"from Purchase where userId=?1 order by id desc",auth.current().id).stream().map(this::view).toList();}
    public OrderView checkout(String address,String delivery,String promo,String key){
        Account a=db.lock(Account.class,auth.current().id);
        var existing=db.query(Purchase.class,"from Purchase where userId=?1 and requestKey=?2",a.id,key);
        if(!existing.isEmpty())return view(existing.getFirst());
        if(!Set.of("COURIER","PICKUP").contains(delivery))fail("Неизвестный способ доставки");
        if(!promo.isBlank()&&!promo.equalsIgnoreCase("STYK10"))fail("Промокод не найден");
        var cart=db.query(CartItem.class,"from CartItem where userId=?1 order by productId",a.id);
        if(cart.isEmpty())fail("Корзина пуста");
        Purchase o=new Purchase();o.userId=a.id;o.address=address;o.delivery=delivery;o.requestKey=key;o.status="RESERVED";o.paymentStatus="DEMO_PAID";o.total=BigDecimal.ZERO;o.discount=BigDecimal.ZERO;db.add(o);
        for(CartItem c:cart){
            Product p=db.lock(Product.class,c.productId);
            if(!p.status.equals("ACTIVE")||p.getAvailable()<c.quantity)fail("Недостаточно товара: "+p.name);
            p.reserved+=c.quantity;movement(p.id,"RESERVE",c.quantity,"ORDER-"+o.id);
            OrderItem item=new OrderItem();item.orderId=o.id;item.productId=p.id;item.sellerId=p.sellerId;item.name=p.name;item.price=p.price;item.quantity=c.quantity;db.add(item);
            o.total=o.total.add(p.price.multiply(BigDecimal.valueOf(c.quantity)));db.remove(c);
        }
        if(promo.equalsIgnoreCase("STYK10")){o.discount=o.total.multiply(new BigDecimal("0.10")).setScale(2,RoundingMode.HALF_UP);o.total=o.total.subtract(o.discount);}
        event(o,"Заказ оплачен в демонстрационном режиме. Товары зарезервированы.");return view(o);
    }
    public OrderView transition(Long id,String next,boolean operator){
        Account a=auth.current();Purchase o=db.lock(Purchase.class,id);
        if(!operator&&!o.userId.equals(a.id))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Заказ другого покупателя");
        if(o.status.equals(next))return view(o);
        boolean cancel=next.equals("CANCELLED")&&Set.of("RESERVED","PICKING","PACKED").contains(o.status);
        boolean refund=next.equals("RETURN_REQUESTED")&&o.status.equals("DELIVERED");
        Map<String,String> flow=Map.of("RESERVED","PICKING","PICKING","PACKED","PACKED","SHIPPED","SHIPPED","DELIVERED","RETURN_REQUESTED","RETURNED");
        if(!cancel&&!refund&&(!operator||!next.equals(flow.get(o.status))))fail("Этот переход статуса недоступен");
        var items=db.query(OrderItem.class,"from OrderItem where orderId=?1 order by productId",o.id);
        for(OrderItem item:items){
            Product p=db.lock(Product.class,item.productId);
            if(cancel){p.reserved-=item.quantity;movement(p.id,"RELEASE",item.quantity,"ORDER-"+o.id);}
            if(next.equals("SHIPPED")){p.reserved-=item.quantity;p.stock-=item.quantity;movement(p.id,"SHIP",-item.quantity,"ORDER-"+o.id);}
            if(next.equals("RETURNED")){p.stock+=item.quantity;movement(p.id,"RETURN",item.quantity,"ORDER-"+o.id);}
        }
        o.status=next;if(cancel||next.equals("RETURNED"))o.paymentStatus="DEMO_REFUNDED";
        event(o,"Статус изменён: "+next+" · "+a.name);return view(o);
    }
    public List<OrderView> warehouse(){return db.query(Purchase.class,"from Purchase order by id desc").stream().map(this::view).toList();}
    public void movement(Long productId,String type,int quantity,String ref){Movement m=new Movement();m.productId=productId;m.type=type;m.quantity=quantity;m.reference=ref;db.add(m);}
    private void event(Purchase order,String note){OrderEvent e=new OrderEvent();e.orderId=order.id;e.status=order.status;e.note=note;db.add(e);}
    public static void fail(String message){throw new ResponseStatusException(HttpStatus.CONFLICT,message);}
}
