package io.cloudnativedata.spring.valkey.office.hours;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import io.cloudnativedata.spring.valkey.office.hours.repository.ProductKeyValueRepository;
import io.cloudnativedata.spring.valkey.office.hours.repository.ProductionJpaRepository;
import io.cloudnativedata.spring.valkey.office.hours.repository.entity.ProductEntity;
import lombok.extern.slf4j.Slf4j;
import nyla.solutions.core.patterns.creational.generator.JavaBeanGeneratorCreator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class DemoConfig {

    @Value("${productCount:10000}")
    private int productCount;

    @Bean
    ApplicationRunner oracleComparisonThroughput(ProductionJpaRepository postgresRepository,
                                                 ProductKeyValueRepository valkeyRepository) {
        return args -> {
            log.info("Starting Oracle throughput benchmark count: {}", productCount);
            var oracleRecords = JavaBeanGeneratorCreator.of(ProductEntity.class).createCollection(productCount);

            long start = System.nanoTime();
            for (var record : oracleRecords)
                postgresRepository.save(record);

            long end = System.nanoTime();

            double elapsedSeconds = (end - start) / 1_000_000_000.0;
            double oracleWritesPerSecond = oracleRecords.size() / elapsedSeconds;

            log.info("Oracle Writes per second: {}", String.format("%.2f", oracleWritesPerSecond));

            log.info("Starting Valkey throughput benchmark count: {}", productCount);
            var records = JavaBeanGeneratorCreator.of(Product.class).createCollection(productCount);

            start = System.nanoTime();

            for (var record : records)
                valkeyRepository.save(record);

            end = System.nanoTime();

            elapsedSeconds = (end - start) / 1_000_000_000.0;
            var valkeyWritesPerSecond = records.size() / elapsedSeconds;

            log.info("Valkey Writes per second: {}", String.format("%.2f", valkeyWritesPerSecond));


            double percentImprovement = ((valkeyWritesPerSecond - oracleWritesPerSecond) / oracleWritesPerSecond) * 100.0;
            double speedupMultiplier = valkeyWritesPerSecond / oracleWritesPerSecond;

            log.info("Improvement - Valkey is: {}% ({}x faster)",
                    String.format("%.2f", percentImprovement),
                    String.format("%.2f", speedupMultiplier));

        };
    }

}
