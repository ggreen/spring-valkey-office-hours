package io.cloudnativedata.spring.valkey.office.hours;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ValkeyOfficeHoursApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValkeyOfficeHoursApplication.class, args);
    }
}
