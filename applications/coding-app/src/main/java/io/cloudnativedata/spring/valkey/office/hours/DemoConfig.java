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
import org.springframework.util.Assert;

@Configuration
@Slf4j
public class DemoConfig {

    @Value("${productCount:10000}")
    private int productCount;

//    @Bean
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

    @Bean
    ApplicationRunner reads(ProductKeyValueRepository valkeyRepository,ProductionJpaRepository oracleRepository){
        return args -> {


            Product product = JavaBeanGeneratorCreator.of(Product.class).create();
            valkeyRepository.save(product);

            Assert.isTrue(valkeyRepository.findById(product.getId()).isPresent(),"Has Record");

            long start = System.nanoTime();
            int iterations = 100;
            for (int i = 0; i < iterations; i++) {
                valkeyRepository.findById(product.getId());
            }
            long end = System.nanoTime();

            long totalNs = end - start;
            double avgLatencyMs = (totalNs / (double) iterations) / 1_000_000.0;
            double avgLatencyUs = (totalNs / (double) iterations) / 1_000.0;

            log.info("Valkey Average latency: {} ms ({} µs)",
                    String.format("%.4f", avgLatencyMs),
                    String.format("%.2f", avgLatencyUs));


            var productEntity = JavaBeanGeneratorCreator.of(ProductEntity.class).create();
            oracleRepository.save(productEntity);

            Assert.isTrue(oracleRepository.findById(productEntity.getProductId()).isPresent(),"Has Record");

             start = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                oracleRepository.findById(productEntity.getProductId());
            }
             end = System.nanoTime();

            totalNs = end - start;
             avgLatencyMs = (totalNs / (double) iterations) / 1_000_000.0;
             avgLatencyUs = (totalNs / (double) iterations) / 1_000.0;

            log.info("Oracle Average latency: {} ms ({} µs)",
                    String.format("%.4f", avgLatencyMs),
                    String.format("%.2f", avgLatencyUs));


        };
    }

}
