package io.cloudnativedata.spring.valkey.office.hours;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/** Cache-aside via Spring's cache abstraction; the cache provider is Valkey. */
@Service
public class ProductService {

    private final ProductCatalog catalog;
    private final ProductEvents events;

    public ProductService(ProductCatalog catalog, ProductEvents events) {
        this.catalog = catalog;
        this.events = events;
    }

    @Cacheable(cacheNames = "products", key = "#id")
    public Product get(String id) {
        return catalog.findById(id).orElseThrow(() -> new NoSuchElementException("No product " + id));
    }

    /** Write-through: updates the database, refreshes the cache entry and publishes an event. */
    @CachePut(cacheNames = "products", key = "#id")
    public Product updatePrice(String id, BigDecimal newPrice) {
        Product current = catalog.findById(id).orElseThrow(() -> new NoSuchElementException("No product " + id));
        Product saved = catalog.save(current.withPrice(newPrice));
        events.publishPriceChange(current, saved);
        return saved;
    }
}
