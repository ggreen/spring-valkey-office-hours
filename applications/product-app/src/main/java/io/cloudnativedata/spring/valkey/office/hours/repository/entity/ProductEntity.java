package io.cloudnativedata.spring.valkey.office.hours.repository.entity;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class ProductEntity {
    @Id
    private String productId;

    @Embedded
    private Product product;
}
