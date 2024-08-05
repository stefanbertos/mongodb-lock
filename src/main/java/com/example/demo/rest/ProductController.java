package com.example.demo.rest;

import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/product")
public class ProductController {
    @Autowired
    private ProductService productService;

    @GetMapping
    public void productBlackHour() {
        for(int i=0; i<100; i++) {
            productService.create("product" + i);
        }
    }
}
