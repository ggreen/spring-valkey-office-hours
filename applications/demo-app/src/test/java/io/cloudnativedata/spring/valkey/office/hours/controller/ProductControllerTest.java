package io.cloudnativedata.spring.valkey.office.hours.controller;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import io.cloudnativedata.spring.valkey.office.hours.service.ProductService;
import nyla.solutions.core.patterns.creational.generator.JavaBeanGeneratorCreator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @InjectMocks
    private ProductController subject;

    @Mock
    private ProductService service;

    private final Product product = JavaBeanGeneratorCreator.of(Product.class).create();

    @Test
    void saveProduct() {

        subject.saveProduct(product);

        verify(service).saveProduct(any());
    }
}