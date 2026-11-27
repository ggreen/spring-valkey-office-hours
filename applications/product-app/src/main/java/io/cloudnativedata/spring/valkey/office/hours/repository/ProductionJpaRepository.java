package io.cloudnativedata.spring.valkey.office.hours.repository;

import io.cloudnativedata.spring.valkey.office.hours.repository.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductionJpaRepository extends JpaRepository<ProductEntity,String> {
}
