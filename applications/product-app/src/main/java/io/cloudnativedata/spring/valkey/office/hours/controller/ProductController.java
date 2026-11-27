package io.cloudnativedata.spring.valkey.office.hours.controller;

import io.cloudnativedata.spring.valkey.office.hours.service.ProductEvents;
import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import io.cloudnativedata.spring.valkey.office.hours.domains.ProductViews;
import io.cloudnativedata.spring.valkey.office.hours.service.ProductService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

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
    public ResponseEntity<Product> getProduct(@PathVariable String id) {
        long start = System.nanoTime();
        var product = service.findProductById(id);
        long end = System.nanoTime();

        // Valkey sorted set as a real-time "most viewed" leaderboard
        redis.opsForZSet().incrementScore(VIEWS, id, 1);

        return ResponseEntity.ok()
                .header("X-Elapsed-Ms", "%.2f".formatted((end - start) / 1_000_000.0))
                .body(product);
    }

    @PutMapping("/products/{id}/price")
    public Product updatePrice(@PathVariable String id, @RequestParam BigDecimal price) {
        return service.updatePrice(id, price);
    }

    @GetMapping("/popular")
    public List<ProductViews> popular(@RequestParam(defaultValue = "5") int limit) {

        //retrieves the top (ex: top 5) members of a Redis Sorted Set (ZSET)
        // ordered by highest score first, including each member's score.
        var top = redis.opsForZSet().reverseRangeWithScores(VIEWS, 0, limit - 1);
        if (top == null)
            return List.of();

        return top.stream().map( t -> ProductViews.builder()
                .product(service.findProductById(t.getValue()))
                .views(Objects.requireNonNull(t.getScore()).longValue())
                .build()).toList();
        /*
        return top.stream().map(t -> Map.<String, Object>of(
                "product", service.findProductById(t.getValue()),
                        "views", t.getScore().longValue()))
                .toList();
                */
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

    @PostMapping("/products/product")
    public void saveProduct(@RequestBody Product product) {

        this.service.saveProduct(product);

    }
}
