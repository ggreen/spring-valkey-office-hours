package io.cloudnativedata.spring.valkey.office.hours.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Event-driven data: price changes are published to a Valkey pub/sub channel.
 * Any app instance subscribed to the channel pushes them to its browsers over SSE.
 */
@Component
public class ProductEvents implements MessageListener {

    public static final String CHANNEL = "product-events";

    private final StringRedisTemplate redis;
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public ProductEvents(StringRedisTemplate redis, RedisMessageListenerContainer container) {
        this.redis = redis;
        container.addMessageListener(this, new ChannelTopic(CHANNEL));
    }

    public void publishPriceChange(Product before, Product after) {
        String json = "{\"id\":\"%s\",\"name\":\"%s\",\"oldPrice\":%s,\"newPrice\":%s,\"at\":\"%s\"}"
                .formatted(after.getId(), after.getName(), before.getPrice(), after.getPrice(), after.getUpdatedAt());
        redis.convertAndSend(CHANNEL, json);
    }

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));
        return emitter;
    }

    @Override
    public void onMessage(org.springframework.data.redis.connection.Message message, byte[] pattern) {
        String body = new String(message.getBody(), StandardCharsets.UTF_8);
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("price-change").data(body));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }
}
