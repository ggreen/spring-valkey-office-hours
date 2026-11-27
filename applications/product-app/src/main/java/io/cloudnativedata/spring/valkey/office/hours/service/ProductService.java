package io.cloudnativedata.spring.valkey.office.hours.service;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import io.cloudnativedata.spring.valkey.office.hours.repository.ProductionJpaRepository;
import io.cloudnativedata.spring.valkey.office.hours.repository.entity.ProductEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

/** Cache-aside via Spring's cache abstraction; the cache provider is Valkey. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductionJpaRepository repository;
    private final ProductEvents events;


    @Cacheable(cacheNames = "products", key = "#id")
    public Product findProductById(String id) {
        log.info("Getting product with id {} from database", id);
        return repository.findById(id).orElseThrow(
                () -> new NoSuchElementException("No product " + id)).getProduct();
    }

    /** Write-through: updates the database, refreshes the cache entry and publishes an event. */
    @CachePut(cacheNames = "products", key = "#id")
    public Product updatePrice(String id, BigDecimal newPrice) {
        var current = repository.findById(id).orElseThrow(() -> new NoSuchElementException("No product " + id))
                .getProduct();
        var produceWitNewPrice = current.withPrice(newPrice);
        saveProduct(produceWitNewPrice);

        events.publishPriceChange(current, produceWitNewPrice);
        return produceWitNewPrice;
    }

    public void saveProduct(Product product) {
        repository.save(ProductEntity.builder()
                .product(product)
                .productId(product.getId()).build());
    }
}
