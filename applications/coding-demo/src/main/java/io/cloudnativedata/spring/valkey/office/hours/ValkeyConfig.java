package io.cloudnativedata.spring.valkey.office.hours;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.RedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
public class ValkeyConfig {

    /** JSON values (readable with valkey-cli) and a TTL so stale entries age out. */
    @Bean
    RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .prefixCacheNameWith("catalog:")
                .serializeValuesWith(SerializationPair.fromSerializer(RedisSerializer.json()));
    }

    @Bean
    RedisSerializer<?> serializer(){
        return RedisSerializer.string();
    }
}
