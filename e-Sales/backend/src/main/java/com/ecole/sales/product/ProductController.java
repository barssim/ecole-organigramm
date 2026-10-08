package com.ecole.sales.product;

import com.ecole.sales.provider.ProductService;
import com.ecole.sales.security.CurrentUser;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/products")
public class ProductController {
    private final ProductService products;private final CurrentUser current;
    public ProductController(ProductService products,CurrentUser current){this.products=products;this.current=current;}
    @GetMapping public List<ProductService.Product> get(){current.get();return products.all();}
}
