package io.cloudnativedata.spring.valkey.office.hours.domains;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** A product catalog entry. Cached in Valkey as JSON. */
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class Product {
    private String id;
    private String name;
    private String category;
    private BigDecimal price;
    private int stock;
    private long updatedAt;

    public Product withPrice(BigDecimal newPrice) {
        return new Product(id, name, category, newPrice, stock, System.currentTimeMillis());
    }
}
