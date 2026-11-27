package io.cloudnativedata.spring.valkey.office.hours.domains;

import java.math.BigDecimal;

public record ProductPriceChangeEvent(
        String id, String name, BigDecimal oldPrice, BigDecimal newPrice, long at) {
}
