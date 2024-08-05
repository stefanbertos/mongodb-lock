package com.example.demo.service;

import com.example.demo.dto.Product;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class ProductService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private LockService lockService;

    public Product create(String name) {
        var documentId = UUID.randomUUID().toString();

        lockService.acquireLock(documentId);

        Product product = new Product(documentId, name);
        mongoTemplate.save(product);

        lockService.releaseLock(documentId);

        return product;
    }

    public Product update(String documentId, String name) {
        lockService.acquireLock(documentId);

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(documentId));

        Product product = mongoTemplate.findAndModify(query, Update.update("name", name), Product.class);

        lockService.releaseLock(documentId);
        return product;
    }
}
