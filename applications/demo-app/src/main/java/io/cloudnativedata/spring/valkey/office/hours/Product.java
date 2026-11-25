package io.cloudnativedata.spring.valkey.office.hours;

import java.math.BigDecimal;
import java.time.Instant;

/** A product catalog entry. Cached in Valkey as JSON. */
public record Product(String id, String name, String category, BigDecimal price, int stock, Instant updatedAt) {

    public Product withPrice(BigDecimal newPrice) {
        return new Product(id, name, category, newPrice, stock, Instant.now());
    }
}
