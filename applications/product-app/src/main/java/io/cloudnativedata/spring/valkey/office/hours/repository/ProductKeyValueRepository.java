package io.cloudnativedata.spring.valkey.office.hours.repository;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import org.springframework.data.keyvalue.repository.KeyValueRepository;

public interface ProductKeyValueRepository extends KeyValueRepository<Product, String> {
}
