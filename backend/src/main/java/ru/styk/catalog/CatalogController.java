package ru.styk.catalog;

import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import ru.styk.domain.Models.*;

@RestController @RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalog;
    public CatalogController(CatalogService catalog){this.catalog=catalog;}
    @GetMapping("/catalog/products") List<Product> products(){return catalog.products();}
    @GetMapping("/catalog/products/{id}") Product product(@PathVariable Long id){return catalog.product(id);}
    @GetMapping("/catalog/products/{id}/reviews") List<Review> reviews(@PathVariable Long id){return catalog.reviews(id);}
    public record ReviewInput(@Min(1) @Max(5) int rating,@NotBlank @Size(max=2000) String text){}
    @PostMapping("/products/{id}/reviews") Review review(@PathVariable Long id,@Valid @RequestBody ReviewInput r){return catalog.review(id,r.rating,r.text);}
    @GetMapping("/favorites") List<Long> favorites(){return catalog.favorites();}
    @PutMapping("/favorites/{id}") void favorite(@PathVariable Long id){catalog.favorite(id,true);}
    @DeleteMapping("/favorites/{id}") void unfavorite(@PathVariable Long id){catalog.favorite(id,false);}
    @GetMapping("/seller/products") List<Product> sellerProducts(){return catalog.sellerProducts();}
    @PostMapping("/seller/products") Product create(@Valid @RequestBody CatalogService.ProductInput input){return catalog.save(null,input);}
    @PutMapping("/seller/products/{id}") Product update(@PathVariable Long id,@Valid @RequestBody CatalogService.ProductInput input){return catalog.save(id,input);}
}
