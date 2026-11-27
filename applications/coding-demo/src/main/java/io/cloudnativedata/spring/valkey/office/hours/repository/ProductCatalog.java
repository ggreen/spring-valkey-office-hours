package io.cloudnativedata.spring.valkey.office.hours.repository;

import io.cloudnativedata.spring.valkey.office.hours.domains.Product;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Stand-in for the slow system of record (RDBMS, legacy API...).
 * Every access sleeps {@code app.db.latency-ms} so the cache benefit is easy to see.
 */
@Repository
public class ProductCatalog {

    private static final List<String> CATEGORIES = List.of("Laptops", "Phones", "Audio", "Cameras", "Wearables", "Gaming");
    private static final List<String> ADJECTIVES = List.of("Pro", "Max", "Lite", "Ultra", "Air", "Mini");

    /** Set when the current thread had to go to the "database" (lets the controller report HIT/MISS). */
    private static final ThreadLocal<Boolean> DB_HIT = ThreadLocal.withInitial(() -> false);

    private final Map<String, Product> table = new ConcurrentHashMap<>();
    private final long latencyMs;

    public ProductCatalog(@Value("${app.db.latency-ms:150}") long latencyMs,
                          @Value("${app.catalog.size:1000}") int size) {
        this.latencyMs = latencyMs;
        Random random = new Random(42);
        IntStream.rangeClosed(1, size).forEach(i -> {
            String category = CATEGORIES.get(i % CATEGORIES.size());
            String id = String.valueOf(i);
            table.put(id, new Product(id,
                    category.substring(0, category.length() - 1) + " " + ADJECTIVES.get(random.nextInt(ADJECTIVES.size())) + " " + i,
                    category,
                    BigDecimal.valueOf(9_99 + random.nextInt(199_000), 2),
                    random.nextInt(500),
                    Instant.now().getEpochSecond()));
        });
    }

    public Optional<Product> findById(String id) {
        simulateLatency();
        DB_HIT.set(true);
        return Optional.ofNullable(table.get(id));
    }

    public Product save(Product product) {
        simulateLatency();
        table.put(product.getId(), product);
        return product;
    }

    public int size() {
        return table.size();
    }

    /** Returns whether this thread hit the database since the last call, and resets the flag. */
    public static boolean consumeDbHit() {
        boolean hit = DB_HIT.get();
        DB_HIT.remove();
        return hit;
    }

    private void simulateLatency() {
        try {
            Thread.sleep(latencyMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}