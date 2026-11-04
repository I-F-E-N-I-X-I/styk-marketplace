package ru.styk.supply;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import ru.styk.domain.Models.Supply;
import java.util.*;

@RestController @RequestMapping("/api")
public class SupplyController {
    private final SupplyService supplies;
    public SupplyController(SupplyService supplies){this.supplies=supplies;}
    public record Create(@NotNull Long productId,@Min(1) @Max(10000) int quantity){}
    public record Change(@NotBlank String status,@Min(0) int received,@NotNull @Size(max=500) String note){}
    @GetMapping("/seller/supplies") List<Supply> seller(){return supplies.supplies(true);}
    @GetMapping("/warehouse/supplies") List<Supply> warehouse(){return supplies.supplies(false);}
    @PostMapping("/seller/supplies") Supply create(@Valid @RequestBody Create c){return supplies.create(c.productId,c.quantity);}
    @PostMapping("/seller/supplies/{id}/status") Supply sellerChange(@PathVariable Long id,@Valid @RequestBody Change c){return supplies.transition(id,c.status,c.received,c.note,false);}
    @PostMapping("/warehouse/supplies/{id}/status") Supply receive(@PathVariable Long id,@Valid @RequestBody Change c){return supplies.transition(id,c.status,c.received,c.note,true);}
    @PostMapping("/warehouse/replenish") Map<String,Integer> replenish(){return Map.of("created",supplies.replenish());}
}
