package io.cloudnativedata.spring.valkey.office.hours.domains;

import lombok.Builder;

@Builder
public record ProductViews(Product product,long views) {
}
