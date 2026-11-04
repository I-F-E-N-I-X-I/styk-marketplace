package ru.styk.order;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;

@RestController @RequestMapping("/api")
public class OrderController {
    private final OrderService orders;
    public OrderController(OrderService orders){this.orders=orders;}
    public record Quantity(@Min(0) @Max(99) int quantity){}
    public record Checkout(@NotBlank @Size(min=8,max=400) String address,@NotBlank String delivery,@NotNull @Size(max=30) String promo,@NotBlank @Size(max=80) String requestKey){}
    public record Transition(@NotBlank String status){}
    @GetMapping("/cart") List<OrderService.CartRow> cart(){return orders.cart();}
    @PutMapping("/cart/{id}") void quantity(@PathVariable Long id,@Valid @RequestBody Quantity q){orders.setCart(id,q.quantity);}
    @GetMapping("/orders") List<OrderService.OrderView> orders(){return orders.orders();}
    @PostMapping("/orders") OrderService.OrderView checkout(@Valid @RequestBody Checkout in){return orders.checkout(in.address,in.delivery,in.promo,in.requestKey);}
    @PostMapping("/orders/{id}/status") OrderService.OrderView change(@PathVariable Long id,@Valid @RequestBody Transition in){return orders.transition(id,in.status,false);}
    @GetMapping("/warehouse/orders") List<OrderService.OrderView> warehouse(){return orders.warehouse();}
    @PostMapping("/warehouse/orders/{id}/status") OrderService.OrderView warehouseChange(@PathVariable Long id,@Valid @RequestBody Transition in){return orders.transition(id,in.status,true);}
}
