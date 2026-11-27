package io.cloudnativedata.spring.valkey.office.hours.service;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import io.cloudnativedata.spring.valkey.office.hours.repository.ProductionJpaRepository;
import io.cloudnativedata.spring.valkey.office.hours.repository.entity.ProductEntity;
import nyla.solutions.core.patterns.creational.generator.JavaBeanGeneratorCreator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductionJpaRepository repository;

    @Mock
    private ProductEvents events;

    @InjectMocks
    private ProductService subject;

    private static final String PRODUCT_ID = "P100";

    private Product product;
    private ProductEntity productEntity;


    @Mock
    private Product updated;

    private final BigDecimal oldPrice = new BigDecimal("10.00");
    private final BigDecimal newPrice = new BigDecimal("20.00");

    @BeforeEach
    void setUp() {
        product = JavaBeanGeneratorCreator.of(Product.class).create();
        productEntity = ProductEntity.builder()
                .productId(product.getId())
                .product(product)
                .build();
    }

    @Test
    void saveProduct() {

        subject.saveProduct(product);

        verify(repository).save(any(ProductEntity.class));
    }

    @Test
    void findBy() {
        when(repository.findById(anyString())).thenReturn(Optional.of(productEntity));
        var actual = subject.findProductById(product.getId());

        assertThat(actual).isEqualTo(product);
    }

    @Test
    void updatePublishesNewPrice() {
        // Given
        String id = "P100";
        BigDecimal newPrice = new BigDecimal("20.00");


        when(repository.findById(id)).thenReturn(Optional.of(productEntity));

        // When
        Product result = subject.updatePrice(id, newPrice);

        // Then
        assertThat(newPrice).isEqualTo(result.getPrice());

        verify(events).publishPriceChange(any(), any());
        verify(repository).save(any(ProductEntity.class));
    }

}
