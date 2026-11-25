package io.cloudnativedata.spring.valkey.office.hours.controller;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadLocalRandom;

import io.cloudnativedata.spring.valkey.office.hours.ProductCatalog;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

/** Server-side load generator + cache stats, used by the demo page. */
@RestController
@RequestMapping("/api")
public class BenchmarkController {

    private final StringRedisTemplate redis;
    private final ProductCatalog catalog;

    public BenchmarkController(StringRedisTemplate redis, ProductCatalog catalog) {
        this.redis = redis;
        this.catalog = catalog;
    }

    /** Hammers Valkey with a read/write mix from many concurrent threads. */
    @PostMapping("/benchmark")
    public Map<String, Object> benchmark(@RequestParam(defaultValue = "16") int threads,
                                         @RequestParam(defaultValue = "50000") int ops,
                                         @RequestParam(defaultValue = "10") int writePercent) throws Exception {
        String payload = "x".repeat(512);
        int keys = 1000;
        for (int i = 0; i < keys; i++) redis.opsForValue().set("bench:" + i, payload);

        int perThread = ops / threads;
        long[][] latencies = new long[threads][perThread];
        long start = System.nanoTime();
        try (ExecutorService pool = Executors.newFixedThreadPool(threads)) {
            List<Future<?>> futures = new ArrayList<>();
            for (int t = 0; t < threads; t++) {
                final int idx = t;
                futures.add(pool.submit(() -> {
                    ThreadLocalRandom rnd = ThreadLocalRandom.current();
                    for (int i = 0; i < perThread; i++) {
                        String key = "bench:" + rnd.nextInt(keys);
                        long s = System.nanoTime();
                        if (rnd.nextInt(100) < writePercent) redis.opsForValue().set(key, payload);
                        else redis.opsForValue().get(key);
                        latencies[idx][i] = System.nanoTime() - s;
                    }
                }));
            }
            for (Future<?> f : futures) f.get();
        }
        double seconds = (System.nanoTime() - start) / 1e9;
        long[] all = Arrays.stream(latencies).flatMapToLong(Arrays::stream).sorted().toArray();
        int total = all.length;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("threads", threads);
        result.put("operations", total);
        result.put("writePercent", writePercent);
        result.put("seconds", round(seconds));
        result.put("opsPerSecond", Math.round(total / seconds));
        result.put("p50Micros", all[total / 2] / 1000);
        result.put("p99Micros", all[(int) (total * 0.99)] / 1000);
        result.put("databaseOpsPerSecondAtSameConcurrency", Math.round(threads * 1000.0 / dbLatency()));
        return result;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Properties server = (Properties) redis.execute((RedisCallback<Object>) c -> c.serverCommands().info("server"));
        Properties stats = (Properties) redis.execute((RedisCallback<Object>) c -> c.serverCommands().info("stats"));
        String version = server.getProperty("valkey_version", server.getProperty("redis_version"));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("server", server.getProperty("server_name", "redis") + " " + version);
        result.put("keys", redis.execute((RedisCallback<Object>) c -> c.serverCommands().dbSize()));
        result.put("keyspaceHits", stats.getProperty("keyspace_hits"));
        result.put("keyspaceMisses", stats.getProperty("keyspace_misses"));
        result.put("catalogSize", catalog.size());
        return result;
    }

    @DeleteMapping("/cache")
    public Map<String, Object> flush() {
        var keys = redis.keys("catalog:*");
        if (keys != null && !keys.isEmpty()) redis.delete(keys);
        return Map.of("evicted", keys == null ? 0 : keys.size());
    }

    private long dbLatency() {
        long s = System.nanoTime();
        catalog.findById("1");
        ProductCatalog.consumeDbHit();
        return Math.max(1, (System.nanoTime() - s) / 1_000_000);
    }

    private static double round(double v) {
        return Math.round(v * 100) / 100.0;
    }
}
