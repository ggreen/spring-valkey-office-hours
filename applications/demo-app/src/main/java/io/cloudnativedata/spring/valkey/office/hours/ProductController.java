package io.cloudnativedata.spring.valkey.office.hours;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api")
public class ProductController {

    private static final String VIEWS = "catalog:views";

    private final ProductService service;
    private final ProductEvents events;
    private final StringRedisTemplate redis;

    public ProductController(ProductService service, ProductEvents events, StringRedisTemplate redis) {
        this.service = service;
        this.events = events;
        this.redis = redis;
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<Product> get(@PathVariable String id) {
        long start = System.nanoTime();
        ProductCatalog.consumeDbHit();
        Product product = service.get(id);
        boolean miss = ProductCatalog.consumeDbHit();
        // Valkey sorted set as a real-time "most viewed" leaderboard
        redis.opsForZSet().incrementScore(VIEWS, id, 1);
        return ResponseEntity.ok()
                .header("X-Cache", miss ? "MISS" : "HIT")
                .header("X-Elapsed-Ms", "%.2f".formatted((System.nanoTime() - start) / 1_000_000.0))
                .body(product);
    }

    @PutMapping("/products/{id}/price")
    public Product updatePrice(@PathVariable String id, @RequestParam BigDecimal price) {
        return service.updatePrice(id, price);
    }

    @GetMapping("/popular")
    public List<Map<String, Object>> popular(@RequestParam(defaultValue = "5") int limit) {
        var top = redis.opsForZSet().reverseRangeWithScores(VIEWS, 0, limit - 1);
        if (top == null) return List.of();
        return top.stream().map(t -> Map.<String, Object>of(
                "product", service.get(t.getValue()), "views", t.getScore().longValue())).toList();
    }

    @GetMapping(path = "/events", produces = "text/event-stream")
    public SseEmitter events() {
        return events.subscribe();
    }

    @ExceptionHandler(NoSuchElementException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> notFound(NoSuchElementException e) {
        return Map.of("error", e.getMessage());
    }
}
